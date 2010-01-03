/*
    This file is part of Titan.

    Titan is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as 
    published by the Free Software Foundation, either version 3 of 
    the License, or (at your option) any later version.

    Titan is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Titan. If not, see <http://www.gnu.org/licenses/>.
*/

/**
 * TitanSerialM.nc
 * Takes care about the serial connection between a Titan mote and the PC.
 *
 * The module takes incoming messages from the serial port and forwards it to the RF module or the 
 * LocalCfgForward interface depending on whether the message was sent for the network or the local 
 * mote.
 *
 * The local node can send messages over the serial port using the AMSend interface, which 
 * basically wraps the SerialMessageC AMSend interface.
 *
 *
 *
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * @author Mirco Rossi <rossi@ife.ee.ethz.ch>
 */
 
 #include "TitanComm.h"
 
 module TitanSerialM {
   uses{
    interface SplitControl as Control;
    interface Boot;
    
    // serial interface
    interface Receive as SerialReceive;
    interface AMSend  as SerialSend;
    interface Packet as SerialPacket;
    interface AMPacket as SerialAMPacket;
	
    
    // RF interface
    interface SplitControl as RFControl;
    interface AMSend   as RFSendCfg;
    interface AMSend   as RFSendData;
    interface AMPacket as RFAMPacket;
    interface Packet   as RFPacket;
    interface PacketLink;
    
    
    // Led debug
    interface Leds;
	
  	//Random Number Generator
  	interface Random;
    
   }
   
   //provides interface AMSend;
   provides interface Receive as LocalCfgForward;
   provides interface Receive as LocalDataForward;
   provides interface TitanSerialBufferSend as CFG_BufferSend;
   provides interface TitanSerialBufferSend as DAT_BufferSend;
 }
 
 implementation {
 
  message_t m_RFMsg; ///< buffers 1 message from the serial port that go to the RF interface
  bool      m_bRFSending;
  
  
  bool serial_locked;
  
  
  //buffer for CFG messages (rf2serial)
  bool      m_CFG_Locked[SERIAL_BUFFER_SIZE];
  bool      m_CFG_Send[SERIAL_BUFFER_SIZE];
  uint8_t   m_CFG_Length[SERIAL_BUFFER_SIZE];
  message_t m_CFG_Buffer[SERIAL_BUFFER_SIZE];
  uint8_t   m_CFG_Next;
  message_t* m_CFG_msg;
  
  //buffer for DAT messages (rf2serial)
  bool      m_DAT_Locked[SERIAL_BUFFER_SIZE];
  bool      m_DAT_Send[SERIAL_BUFFER_SIZE];
  uint8_t   m_DAT_Length[SERIAL_BUFFER_SIZE];
  message_t m_DAT_Buffer[SERIAL_BUFFER_SIZE];
  uint8_t   m_DAT_Next;
  message_t* m_DAT_msg;

  bool isCFGSending;
  
  //Function prototypes
  void CFG_SendNext(message_t *msg, error_t error);
  void DAT_SendNext(message_t *msg, error_t error);
  task void CFG_SendNextTask();
  task void DAT_SendNextTask();
  
  //Paket Link Delay
  uint16_t m_Retrydelay;
  
  
  ////////////////////////////////////////////////////////////////////////////
  // Initialization

  event void Boot.booted() {
	uint8_t i;

    call Control.start();
    call RFControl.start();
    m_bRFSending = FALSE;
	
    for (i=0; i<SERIAL_BUFFER_SIZE; i++ ) {
      m_DAT_Locked[i]= FALSE;
      m_DAT_Send[i]  = FALSE;
      m_CFG_Locked[i] = FALSE;
      m_CFG_Send[i]   = FALSE;
    }
	
	serial_locked=FALSE;
	m_DAT_msg=&(m_DAT_Buffer[0]);
	m_CFG_msg= &(m_CFG_Buffer[0]);
	m_DAT_Next=m_CFG_Next=0;
	
  }
  
  event void Control.startDone(error_t err) {
  }

  event void Control.stopDone(error_t err) {
  }

  event void RFControl.startDone(error_t err) {
  }

  event void RFControl.stopDone(error_t err) {
  }

  ////////////////////////////////////////////////////////////////////////////
  // Configuration data
  
  /**
   * Called when a message has been sent over the serial channel. If the message originates from 
   * the TitanSerialM message buffer, it is freed. In the other case the AMSend interface has been 
   * used, so notify the sender.
   */
  event void SerialSend.sendDone( message_t* pMsg, error_t err ) {
    if(isCFGSending)
      CFG_SendNext(pMsg,err);
    else
      DAT_SendNext(pMsg,err);
  }
  
  /**
   * Receives a message from the serial port. Detects whether they are intended for the local mote 
   * or have to be sent over the RF channel. If the RF channel is busy, the message is dropped.
  */
  event message_t* SerialReceive.receive(message_t* pMsg, void* payload, uint8_t len) {
  
    bool bSending = FALSE;
    SerialMsg* pSMsg    = (SerialMsg*)call SerialPacket.getPayload(pMsg,1);

    //dbg("TitanSerial", "INFO: Received message\n");
    
    //////////////////////////////////////////////////////////////////////////
    // check whether this is a data message
    //TODO: This is a quick fix and needs to be improved - it basically does the same as the cfg sender
    if ( (pSMsg->data[0] & 0x0F) == TITANCOMM_DATAMSG ) {
    
      call Leds.led0Toggle();
    
      if (pSMsg->address == call RFAMPacket.address() || pSMsg->address == AM_BROADCAST_ADDR) {
        signal LocalDataForward.receive(pMsg, pSMsg->data+1, pSMsg->length-1);
        
        if ( pSMsg->address != AM_BROADCAST_ADDR) return pMsg;
      }

      call Leds.led1Toggle();

      atomic {if (m_bRFSending == FALSE ) bSending = m_bRFSending = TRUE;}      
      
      if (bSending == TRUE) {
      
        uint8_t *dataDest,i;
        
        // get payload pointer
        dataDest = (uint8_t*)call RFPacket.getPayload(&m_RFMsg,1);

        // check sizes
        if ( pSMsg->length > (call RFPacket.maxPayloadLength()) ) {
          dbg("TitanSerial","ERROR: Serial packet too big for RF packet Dropping packet\n");
          return pMsg;
        }
        
        // copy data
        for( i=1; i < pSMsg->length; i++ ) {
          dataDest[i-1] = pSMsg->data[i];
        }
        
  	  #ifndef TOSSIM
  		  //m_Retrydelay=1;
  			m_Retrydelay= (call Random.rand16())&0x1F;
        m_Retrydelay= m_Retrydelay+10;
            call PacketLink.setRetries(&m_RFMsg, TITAN_DATAMSG_RETRIES);
            call PacketLink.setRetryDelay(&m_RFMsg, m_Retrydelay);//TITAN_DATAMSG_RETRYDELAY
        #endif
        
  	  // send data
        if ( call RFSendData.send( pSMsg->address, &m_RFMsg, pSMsg->length ) != SUCCESS ) {
          dbg("TitanSerial","ERROR: Could not send RF packet\n");
          m_bRFSending = FALSE; // some error: fail
        } else {
          dbg("TitanSerial","Successfully forwarded config");
        }
   
      }
      
    }
    //////////////////////////////////////////////////////////////////////////
    
    // local messages can stay local
    if ( pSMsg->address == call RFAMPacket.address() || pSMsg->address == AM_BROADCAST_ADDR  ) {
    
      signal LocalCfgForward.receive(pMsg,pSMsg->data,pSMsg->length);
      
      if ( pSMsg->address != AM_BROADCAST_ADDR) return pMsg;
    }
  
    // Try to reserve the RF channel
    atomic {if (m_bRFSending == FALSE ) bSending = m_bRFSending = TRUE;}
  

    if ( bSending == TRUE ) {
      uint8_t *dataDest,i;
      
      // get payload pointer
      dataDest = (uint8_t*)call RFPacket.getPayload(&m_RFMsg,1);

      // check sizes
      if ( pSMsg->length > (call RFPacket.maxPayloadLength()) ) {
        dbg("TitanSerial","ERROR: Serial packet too big for RF packet Dropping packet\n");
        return pMsg;
      }
      
      // copy data
      for( i=0; i < pSMsg->length; i++ ) {
        dataDest[i] = pSMsg->data[i];
      }
      
	  #ifndef TOSSIM
		  //m_Retrydelay=1;
			m_Retrydelay= (call Random.rand16())&0x1F;
      m_Retrydelay= m_Retrydelay+10;
          call PacketLink.setRetries(&m_RFMsg, TITAN_CFGMSG_RETRIES);
          call PacketLink.setRetryDelay(&m_RFMsg, m_Retrydelay);//TITAN_CFGMSG_RETRYDELAY
      #endif
      
	  // send data
      if ( call RFSendCfg.send( pSMsg->address, &m_RFMsg, pSMsg->length ) != SUCCESS ) {
        dbg("TitanSerial","ERROR: Could not send RF packet\n");
        m_bRFSending = FALSE; // some error: fail
      } else {
        dbg("TitanSerial","Successfully forwarded config");
      }
      
    } else {
      //call Leds.led0Toggle();
      //drop packet and return
      dbg("TitanSerial","ERROR: Dropping packet Serial->RF due to busy link\n");
      return pMsg;
    }

    return pMsg;
  }
  

  ////////////////////////////////////////////////////////////////////////////
  // RFSend
  
  event void RFSendCfg.sendDone( message_t* pMsg, error_t err ) {
    // free the channel again
    if ( pMsg == &m_RFMsg ) {
      m_bRFSending = FALSE;
    }

    #if defined(PACKET_LINK) && !defined(TOSSIM)
    if(call PacketLink.wasDelivered(pMsg) != TRUE) {
		 // call Leds.led1Toggle();
    }
    #endif
	
	
  }
  
  event void RFSendData.sendDone( message_t* pMsg, error_t err ) {
    if ( pMsg == &m_RFMsg ) {
      m_bRFSending = FALSE;
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  // CFG_BufferSend interface 
    command void CFG_BufferSend.send(message_t *msg, uint8_t len) {    
    
	#ifndef TOSSIM
    uint8_t i,iCurMsg=0;
    bool bFoundMsg = FALSE;
    
	
    // activate the buffer for sending
    for(i=0; i<SERIAL_BUFFER_SIZE; i++) {
      if (msg == &(m_CFG_Buffer[i])) {
        m_CFG_Locked[i] = TRUE;
        m_CFG_Length[i] = len;
        m_CFG_Send[i] = TRUE;
        bFoundMsg = TRUE;
        iCurMsg=i;
        break;
      }
    }

    // first message is not from the buffer - copy it into it
    if (bFoundMsg == FALSE) {
      uint8_t j,*pSrc,*pDest;

      m_CFG_Locked[0] = TRUE;
      m_CFG_Send[0] = TRUE;
      m_CFG_Length[0] = len;
	  
      pSrc = (uint8_t*)msg;
      pDest= (uint8_t*)&(m_CFG_Buffer[0]);
      for (j=0; j<sizeof(message_t); j++) {
        *(pDest++) = *(pSrc++);
      }
      
      m_CFG_Locked[1] = TRUE;
      m_CFG_Send[1] = FALSE;
      post CFG_SendNextTask();
      m_CFG_msg= &(m_CFG_Buffer[1]);
	  return;
    }
    
    // start processing if there is no lock yet
    post CFG_SendNextTask();
  
    // search for the next free buffer
    atomic {
      for (i=0; i<SERIAL_BUFFER_SIZE; i++) {
        uint8_t icheck = (m_CFG_Next + i)%SERIAL_BUFFER_SIZE;
        if (m_CFG_Locked[icheck] == FALSE) {
          m_CFG_Locked[icheck] = TRUE;
          m_CFG_Send[icheck] = FALSE;
          m_CFG_msg= &(m_CFG_Buffer[icheck]);
		  return;
        }
      }
    }

    // none free - drop this message
    //call Leds.led2Toggle();
    m_CFG_Send[iCurMsg] = FALSE;
    m_CFG_msg= msg;
	#else
      // SIMULATION!
      char strNumber[16];
      char strPacket[4096];
      SerialMsg* pMsg;
      nx_uint8_t* pPayload;
      int i;
      
      // extract RF packet and send it over the debug channel
      pMsg = (SerialMsg*)call SerialPacket.getPayload(msg,0);
      pPayload = pMsg->data;
      sprintf(strPacket,"To: %i, len: %i: ", pMsg->address, pMsg->length);
      for(i=0;i<len;i++) {
        sprintf(strNumber, "%03i ", pPayload[i]);
        strcat(strPacket,strNumber);
      }
      strcat(strPacket,"\n");
      dbg("TitanSerial",strPacket);
      signal SerialSend.sendDone( msg, SUCCESS );
    #endif
  }
  
  task void CFG_SendNextTask() {
    bool bGotLock = FALSE;
    bool bSuccess = TRUE;
	
    // try to see whether we're still waiting for a send to complete
    atomic {
      if (!serial_locked) {
        serial_locked = TRUE;
        bGotLock = TRUE;
		isCFGSending=TRUE;
      }
    }
    
    // wait and try later again
    if ( bGotLock == FALSE ) {
      post CFG_SendNextTask();
      return;
    }

    // send the next message coming up (if it is ready to be sent)
    if ( m_CFG_Locked[m_CFG_Next] == TRUE && m_CFG_Send[m_CFG_Next] == TRUE ) {
	  
	  if ( call SerialSend.send( 0, &m_CFG_Buffer[m_CFG_Next], m_CFG_Length[m_CFG_Next]) == FAIL ) {
          serial_locked = FALSE; // lock not obtained
          post CFG_SendNextTask();
          bSuccess = FALSE;
      }
      
      // send successful - release the lock
      if (bSuccess == TRUE) {
        m_CFG_Send[m_CFG_Next] = FALSE;
        
        // m_CFG_Locked will be freed in sendDone
        // advance in buffer
        m_CFG_Next = (m_CFG_Next+1)%SERIAL_BUFFER_SIZE;
      }
      
    } else {
      // no more messages to send - call RF free
      serial_locked = FALSE;
    }  
  }
  
   void CFG_SendNext(message_t *msg, error_t error) {
    uint8_t i;

    // search for the message in the buffer and free it up
    // also: get the oldest message out (last one)
    for (i=0; i<SERIAL_BUFFER_SIZE; i++) {
      if ( msg == &(m_CFG_Buffer[i]) ) {
        m_CFG_Locked[i] = FALSE;
        m_CFG_Send[i] = FALSE;
        break;
      }
    }
    
    serial_locked = FALSE;
    
    post CFG_SendNextTask();
  }
  
 command message_t* CFG_BufferSend.getMsg(){
	return m_CFG_msg;
 
  }
  
   ////////////////////////////////////////////////////////////////////////////
  // DAT_BufferSend interface 

  command void DAT_BufferSend.send(message_t *msg, uint8_t len) {    
    #ifndef TOSSIM
      uint8_t i,iCurMsg=0;
      bool bFoundMsg = FALSE;
      
      // activate the buffer for sending
      for(i=0; i<SERIAL_BUFFER_SIZE; i++) {
        if (msg == &(m_DAT_Buffer[i])) {
          m_DAT_Locked[i] = TRUE;
          m_DAT_Length[i] = len;
          m_DAT_Send[i] = TRUE;
          bFoundMsg = TRUE;
          iCurMsg=i;
          break;
        }
      }

      // first message is not from the buffer - copy it into it
      if (bFoundMsg == FALSE) {
        uint8_t j,*pSrc,*pDest;

        m_DAT_Locked[0] = TRUE;
        m_DAT_Send[0] = TRUE;
        m_DAT_Length[0] = len;
  	  
        pSrc = (uint8_t*)msg;
        pDest= (uint8_t*)&(m_DAT_Buffer[0]);
        for (j=0; j<sizeof(message_t); j++) {
          *(pDest++) = *(pSrc++);
        }
        
        m_DAT_Locked[1] = TRUE;
        m_DAT_Send[1] = FALSE;
        post DAT_SendNextTask();
        m_DAT_msg= &(m_DAT_Buffer[1]);
  	    return;
      }
      
      // start processing if there is no lock yet
      post DAT_SendNextTask();
    
      // search for the next free buffer
      atomic {
        for (i=0; i<SERIAL_BUFFER_SIZE; i++) {
          uint8_t icheck = (m_DAT_Next + i)%SERIAL_BUFFER_SIZE;
          if (m_DAT_Locked[icheck] == FALSE) {
            m_DAT_Locked[icheck] = TRUE;
            m_DAT_Send[icheck] = FALSE;
            m_DAT_msg= &(m_DAT_Buffer[icheck]);
  		      return;
          }
        }
      }

      // none free - drop this message
      //call Leds.led2Toggle();
      m_DAT_Send[iCurMsg] = FALSE;
      m_DAT_msg= msg;
      
      
    #else
    
      // SIMULATION!
      char strNumber[16];
      char strPacket[4096];
      SerialMsg* pMsg;
      nx_uint8_t* pPayload;
      int i;
      
      // extract RF packet and send it over the debug channel
      pMsg = (SerialMsg*)call SerialPacket.getPayload(msg,0);
      pPayload = pMsg->data;
      sprintf(strPacket,"To: %i, len: %i: ", pMsg->address, pMsg->length);
      for(i=0;i<len;i++) {
        sprintf(strNumber, "%03i ", pPayload[i]);
        strcat(strPacket,strNumber);
      }
      strcat(strPacket,"\n");
      dbg("TitanSerial",strPacket);
      signal SerialSend.sendDone( msg, SUCCESS );
    #endif
  }
  
  task void DAT_SendNextTask() {
    bool bGotLock = FALSE;
    bool bSuccess = TRUE;
	
    // try to see whether we're still waiting for a send to complete
    atomic {
      if (!serial_locked) {
        serial_locked = TRUE;
        bGotLock = TRUE;
        isCFGSending=FALSE;
      }
    }
    
    // wait and try later again
    if ( bGotLock == FALSE ) {
      post DAT_SendNextTask();
      return;
    }

    // send the next message coming up (if it is ready to be sent)
    if ( m_DAT_Locked[m_DAT_Next] == TRUE && m_DAT_Send[m_DAT_Next] == TRUE ) {
	  
	  if ( call SerialSend.send( 0, &m_DAT_Buffer[m_DAT_Next], m_DAT_Length[m_DAT_Next] ) == FAIL ) {
          serial_locked = FALSE; // lock not obtained
          post DAT_SendNextTask();
          bSuccess = FALSE;
      }
      
      // send successful - release the lock
      if (bSuccess == TRUE) {
        m_DAT_Send[m_DAT_Next] = FALSE;
        
        // m_DAT_Locked will be freed in sendDone
        // advance in buffer
        m_DAT_Next = (m_DAT_Next+1)%SERIAL_BUFFER_SIZE;
      }
      
    } else {
      // no more messages to send - call RF free
      serial_locked = FALSE;
    }  
  }
  
   void DAT_SendNext(message_t *msg, error_t error) {
    uint8_t i;

    // search for the message in the buffer and free it up
    // also: get the oldest message out (last one)
    for (i=0; i<SERIAL_BUFFER_SIZE; i++) {
      if ( msg == &(m_DAT_Buffer[i]) ) {
        m_DAT_Locked[i] = FALSE;
        m_DAT_Send[i] = FALSE;
        break;
      }
    }
    
    serial_locked = FALSE;
    
    post DAT_SendNextTask();
  }
  
  command message_t* DAT_BufferSend.getMsg(){
	return m_DAT_msg;
 
  }
  
  
  
  
  ////////////////////////////////////////////////////////////////////////////
  // LocalCfgForward interface - this is just a wrapper for SerialReceive
/*  
  command void *LocalCfgForward.getPayload(message_t *pMsg, uint8_t len) {
    return call SerialReceive.getPayload(pMsg,len);
  }
  
  command uint8_t LocalCfgForward.payloadLength(message_t *pMsg) {
    return call SerialReceive.payloadLength(pMsg);
  }
*/
}
