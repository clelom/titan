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
 *  Implementation of the gateway functionality. Stripped down to a Bluetooth-802.15.4 interface.
 *  This implementation adds buffers for to reduce the drop rate. This is non-optimized code.
 *
 * There are three message buffers:
 * m_ccBuffer   - controls data coming in from the LMX and to be sent to data or config of the CC
 * m_lmx_CFG_Buffer  - data coming in from the CC data and control interface, to be send to LMX
 *
 * Leds:
 * 0: Incoming Bluetooth message
 * 1: Incoming CC config message
 *
 *
 *  @author Christoph Walser
 *  @author Fabian Schenkel
 *  @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *  @author Tonio Gsell <tgsell@ee.ethz.ch>
 *  @date   April 8 2008
 *
 **/
 
#include "Gateway.h"
 
module Gateway {
  uses {
    interface Boot;
    interface Leds;
    
    interface AddressResolution as AddrRes;
  	
    // Radio interfaces

    // Bluetooth LMX
    interface Init     as lmxInit;
  	interface AMSend   as lmxAMSend;
  	interface Receive  as lmxReceive;
    interface AMPacket as lmxPacket;
    
    // CC2420
    interface SplitControl as ccSplitControl; // needed for shared UART
  	interface AMSend       as ccAMSendConfig;
  	interface Receive      as ccReceiveConfig;
  	interface AMSend       as ccAMSendData;
  	interface Receive      as ccReceiveData;
    interface AMPacket     as ccPacket;
    interface PacketLink   as ccPacketLink;
	}
  
}

implementation {
  
  bool cc_locked = FALSE;
  bool lmx_locked = FALSE;
	
  // to be found in TitanComm.h
  const uint8_t TITANCOMM_VERSION = 1;
  const uint8_t TITANCOMM_CONFIG = 0;
  const uint8_t AM_TITANCOMMCONFIGMSG = 13;
  const uint8_t AM_TITANCOMMDATAMSG = 42;
  
  #define GATEWAY_BUFFER_SIZE 32
  
  // bluetooth buffers
  bool      m_lmx_CFG_Locked[GATEWAY_BUFFER_SIZE];  // locks the buffer entry (passed to receive next message)
  bool      m_lmx_CFG_Send[GATEWAY_BUFFER_SIZE];    // marks the buffer ready to be sent
  uint8_t   m_lmx_CFG_Length[GATEWAY_BUFFER_SIZE];  // length of payload in the buffer
  message_t m_lmx_CFG_Buffer[GATEWAY_BUFFER_SIZE];  // buffer for CC data in, LMX out
  uint8_t   m_lmx_CFG_Next;                         // next item to be sent (FIFO) or oldest entry

  bool      m_lmx_DAT_Locked[GATEWAY_BUFFER_SIZE];  // locks the buffer entry (passed to receive next message)
  bool      m_lmx_DAT_Send[GATEWAY_BUFFER_SIZE];    // marks the buffer ready to be sent
  uint8_t   m_lmx_DAT_Length[GATEWAY_BUFFER_SIZE];  // length of payload in the buffer
  message_t m_lmx_DAT_Buffer[GATEWAY_BUFFER_SIZE];  // buffer for CC data in, LMX out
  uint8_t   m_lmx_DAT_Next;  
  
  // zigbee buffers - control
  bool      m_ccLocked[GATEWAY_BUFFER_SIZE];
  bool      m_ccSend[GATEWAY_BUFFER_SIZE];
  uint8_t   m_ccLength[GATEWAY_BUFFER_SIZE];
  message_t m_ccBuffer[GATEWAY_BUFFER_SIZE]; // buffer for LMX in, CC config and data out
  uint8_t   m_ccNext;
  
  
  bool m_isCFGSending;
  
  
  //Function Prototype
  void lmx_CFG_SendNext (message_t *msg, error_t error);
  void lmx_DAT_SendNext (message_t *msg, error_t error);
  
  ////////////////////////////////////////////////////////////////////////////
  // Initialization
  
  /** Initializes the drivers and data structures*/
  event void Boot.booted() {
  
    uint8_t i;
    for (i=0; i<GATEWAY_BUFFER_SIZE; i++ ) {
      m_lmx_CFG_Locked[i]= FALSE;
      m_lmx_CFG_Send[i]  = FALSE;
	  m_lmx_DAT_Locked[i]= FALSE;
      m_lmx_DAT_Send[i]  = FALSE;
      m_ccLocked[i] = FALSE;
      m_ccSend[i]   = FALSE;
    }
  
    m_ccNext = m_lmx_DAT_Next = m_lmx_CFG_Next = 0;
  	//Initialize the radios:
    call lmxInit.init();
    call ccSplitControl.start();
  }
  
  /** Called when SplitControl gives access to the UART */ 
  event void ccSplitControl.startDone(error_t err) {
    if (err != SUCCESS) {
      call ccSplitControl.start(); // try to start again
    }
  }
  
  /** not expected to happen - in this version we have exclusive access to the UART*/
  event void ccSplitControl.stopDone(error_t err) {}
  

  ////////////////////////////////////////////////////////////////////////////
  // LMX IN - CC OUT

  /** 
   * Determines whether the CC is ready to send and picks the next message from the 
   * buffer. Here a difference is made on whether it is a data or config message.
   */
  task void ccSendNextTask() {
    bool bGotLock = FALSE;
    
    // try to see whether we're still waiting for a send to complete
    atomic {
      if (!cc_locked) {
        cc_locked = TRUE;
        bGotLock = TRUE;
      }
    }
    
    // wait and try later again
    if ( bGotLock == FALSE ) {
      post ccSendNextTask();
      return;
    }

    // send the next message coming up (if it is ready to be sent)
    if ( m_ccLocked[m_ccNext] == TRUE && m_ccSend[m_ccNext] == TRUE ) {
      uint16_t dest = call lmxPacket.destination(&(m_ccBuffer[m_ccNext]));
      bool bSuccess = TRUE;
      if(call lmxPacket.type(&(m_ccBuffer[m_ccNext])) == AM_TITANCOMMCONFIGMSG) {
        call ccPacketLink.setRetries(&(m_ccBuffer[m_ccNext]), 3);
        call ccPacketLink.setRetryDelay(&(m_ccBuffer[m_ccNext]), 50);
        if ( call ccAMSendConfig.send( ((dest&0x00FF)==0xFF)?0xFFFF:(dest & 0xFF),&(m_ccBuffer[m_ccNext]), m_ccLength[m_ccNext] ) == FAIL) {  // check for broadcast
          cc_locked = FALSE; // lock not obtained
          post ccSendNextTask();
          bSuccess = FALSE;
        }
      } else if(call lmxPacket.type(&(m_ccBuffer[m_ccNext])) == AM_TITANCOMMDATAMSG) {
        if ( call ccAMSendData.send(dest & 0xFF, &(m_ccBuffer[m_ccNext]), m_ccLength[m_ccNext ] ) == FAIL ) {
          cc_locked = FALSE; // lock not obtained
          post ccSendNextTask();
          bSuccess = FALSE;
        }
      } 
      
      // send successful - release the lock
      if (bSuccess == TRUE) {
        m_ccSend[m_ccNext] = FALSE;
        
        // m_ccLocked will be freed in sendDone
        // advance in buffer
        m_ccNext = (m_ccNext+1)%GATEWAY_BUFFER_SIZE;
      }
      
    } else {
    
      // no more messages to send - call CC free
      cc_locked = FALSE;
    }  
  }
  
  /** called by the two sendDone events. Releases the message buffer and calls the sending task to send the next message */
  void ccSendNext(message_t *msg, error_t error) {
    uint8_t i;

    // search for the message in the buffer and free it up
    // also: get the oldest message out (last one)
    for (i=0; i<GATEWAY_BUFFER_SIZE; i++) {
      if ( msg == &(m_ccBuffer[i]) ) {
        m_ccLocked[i] = FALSE;
        m_ccSend[i] = FALSE;
        break;
      }
    }
    
    cc_locked = FALSE;
    
    post ccSendNextTask();
  }
  
  /** Called when message is done sending */
  event void ccAMSendConfig.sendDone(message_t *msg, error_t error) {
    ccSendNext(msg,error);
  }
  
  /** Called when message is done sending */
  event void ccAMSendData.sendDone(message_t *msg, error_t error) {
    ccSendNext(msg,error);
  }
  
  /** Received packet from the Bluetooth connection. Start sending task and return next buffer to use
   * for the reception of the next message
   */
  event message_t *lmxReceive.receive(message_t *msg, void *payload, uint8_t len) {    

    uint8_t i,iCurMsg=0;
    bool bFoundMsg = FALSE;
  
    call Leds.led0Toggle();

    // data messages have no first byte (message type indicator)
    if ( call lmxPacket.type(msg) == AM_TITANCOMMDATAMSG) {
      uint8_t *pay = (uint8_t*)payload;
      for (i=1; i<len; i++ ) {
        pay[i-1] = pay[i];
      }
      len--;
      
    }
    
    // activate the buffer for sending
    for(i=0; i<GATEWAY_BUFFER_SIZE; i++) {
      if (msg == &(m_ccBuffer[i])) {
        m_ccLocked[i] = TRUE;
        m_ccLength[i] = len;
        m_ccSend[i] = TRUE;
        bFoundMsg = TRUE;
        iCurMsg=i;
        break;
      }
    }

    // first message is not from the buffer - copy it into it
    if (bFoundMsg == FALSE) {
      uint8_t j,*pSrc,*pDest;

      m_ccLocked[0] = TRUE;
      m_ccSend[0] = TRUE;
      m_ccLength[0] = len;

      pSrc = (uint8_t*)msg;
      pDest= (uint8_t*)&(m_ccBuffer[0]);
      for (j=0; j<sizeof(message_t); j++) {
        *(pDest++) = *(pSrc++);
      }
      
      m_ccLocked[1] = TRUE;
      m_ccSend[1] = FALSE;
      post ccSendNextTask();
      return &(m_ccBuffer[1]);

    }
    
    // start processing if there is no lock yet
    post ccSendNextTask();
  
    // search for the next free buffer
    atomic {
      for (i=0; i<GATEWAY_BUFFER_SIZE; i++) {
        uint8_t icheck = (m_ccNext + i)%GATEWAY_BUFFER_SIZE;
        if (m_ccLocked[icheck] == FALSE) {
          m_ccLocked[icheck] = TRUE;
          m_ccSend[icheck] = FALSE;
          return &(m_ccBuffer[icheck]);
        }
      }
    }

    // none free - drop this message
    m_ccSend[iCurMsg] = FALSE;
  	return msg;
  }

  ////////////////////////////////////////////////////////////////////////////
  // CC IN- LMXOUT

  /** 
   * Sends the next message over the LMX interface. Puts priority on the configuration 
   * channel over the data channel.
   */
  task void lmx_CFG_SendNextTask() {

    bool bGotLock = FALSE;
    
    // check whether the LMX is free to send
    atomic {
      if (!lmx_locked) {
        lmx_locked = TRUE;
        bGotLock = TRUE;
		m_isCFGSending=TRUE;
      }
    }
    
    // wait and try later again
    if ( bGotLock == FALSE ) {
      post lmx_CFG_SendNextTask();
      return;
    }

    // check whether we have some message to send. 
    if ( m_lmx_CFG_Locked[m_lmx_CFG_Next] == TRUE && m_lmx_CFG_Send[m_lmx_CFG_Next] == TRUE ) {

      if ( call lmxAMSend.send( 0xFFFF,&(m_lmx_CFG_Buffer[m_lmx_CFG_Next]), m_lmx_CFG_Length[m_lmx_CFG_Next] ) == FAIL) {
        // failed - try again later
        lmx_locked = FALSE;
        post lmx_CFG_SendNextTask();
      } else {
        // successful - advance to next buffer entry
        m_lmx_CFG_Send[m_lmx_CFG_Next] = FALSE;
        m_lmx_CFG_Next = (m_lmx_CFG_Next+1)%GATEWAY_BUFFER_SIZE;
      }
      
    } else {

      // make sure no other messages are ready to send
      int8_t i=0;
      for (i=0; i<GATEWAY_BUFFER_SIZE; i++) {
        int8_t buf_index = (m_lmx_CFG_Next + i)%GATEWAY_BUFFER_SIZE;
        if ( m_lmx_CFG_Locked[buf_index] == TRUE && m_lmx_CFG_Send[buf_index] == TRUE ) {
          if ( call lmxAMSend.send( 0xFFFF,&(m_lmx_CFG_Buffer[buf_index]), m_lmx_CFG_Length[buf_index] ) == FAIL) {
            // failed - try again later
            lmx_locked = FALSE;
            post lmx_CFG_SendNextTask();
          } else {
            // successful - advance to next buffer entry
            m_lmx_CFG_Send[buf_index] = FALSE;
            m_lmx_CFG_Next = (buf_index+1)%GATEWAY_BUFFER_SIZE;
          }
        }
      }
    
    
    
      lmx_locked = FALSE;
    }
  }
  
  
    task void lmx_DAT_SendNextTask() {

    bool bGotLock = FALSE;
    
    // check whether the LMX is free to send
    atomic {
      if (!lmx_locked) {
        lmx_locked = TRUE;
        bGotLock = TRUE;
		m_isCFGSending=FALSE;
      }
    }
    
    // wait and try later again
    if ( bGotLock == FALSE ) {
      post lmx_DAT_SendNextTask();
      return;
    }

    // check whether we have some message to send. 
    if ( m_lmx_DAT_Locked[m_lmx_DAT_Next] == TRUE && m_lmx_DAT_Send[m_lmx_DAT_Next] == TRUE ) {

      if ( call lmxAMSend.send( 0xFFFF,&(m_lmx_DAT_Buffer[m_lmx_DAT_Next]), m_lmx_DAT_Length[m_lmx_DAT_Next] ) == FAIL) {
        // failed - try again later
        lmx_locked = FALSE;
        post lmx_DAT_SendNextTask();
      } else {
        // successful - advance to next buffer entry
        m_lmx_DAT_Send[m_lmx_DAT_Next] = FALSE;
        m_lmx_DAT_Next = (m_lmx_DAT_Next+1)%GATEWAY_BUFFER_SIZE;
      }
      
    } else {
    
      // make sure no other messages are ready to send
      int8_t i=0;
      for (i=0; i<GATEWAY_BUFFER_SIZE; i++) {
        int8_t buf_index = (m_lmx_DAT_Next + i)%GATEWAY_BUFFER_SIZE;
        if ( m_lmx_DAT_Locked[buf_index] == TRUE && m_lmx_DAT_Send[buf_index] == TRUE ) {
          if ( call lmxAMSend.send( 0xFFFF,&(m_lmx_DAT_Buffer[buf_index]), m_lmx_DAT_Length[buf_index] ) == FAIL) {
            // failed - try again later
            lmx_locked = FALSE;
            post lmx_DAT_SendNextTask();
          } else {
            // successful - advance to next buffer entry
            m_lmx_DAT_Send[buf_index] = FALSE;
            m_lmx_DAT_Next = (buf_index+1)%GATEWAY_BUFFER_SIZE;
          }
        }
      }
    
      lmx_locked = FALSE;
    }
  }
  
  

    void lmx_CFG_SendNext(message_t *msg, error_t error) {
		    uint8_t i;

		// search for the message in the buffer and free it up
		for (i=0; i<GATEWAY_BUFFER_SIZE; i++) {
			if ( msg == &(m_lmx_CFG_Buffer[i]) ) {
				m_lmx_CFG_Locked[i] = FALSE;
				m_lmx_CFG_Send[i] = FALSE;
				break;
			}	 
		}
    
		lmx_locked = FALSE;
    
		post lmx_CFG_SendNextTask();
	}
  



    void lmx_DAT_SendNext(message_t *msg, error_t error) {
		    uint8_t i;

    
    
		// search for the message in the buffer and free it up
		for (i=0; i<GATEWAY_BUFFER_SIZE; i++) {
			if ( msg == &(m_lmx_DAT_Buffer[i]) ) {
				m_lmx_DAT_Locked[i] = FALSE;
				m_lmx_DAT_Send[i] = FALSE;
				break;
			}	 
		}
    
		lmx_locked = FALSE;
    
		post lmx_DAT_SendNextTask();
	}
  
  
  
  
  /** LMX has completed sending the message. Free buffer and start sending task */
  event void lmxAMSend.sendDone(message_t *msg, error_t error) {
//		if(m_isCFGSending)
			lmx_CFG_SendNext (msg, error);
//		else
			lmx_DAT_SendNext (msg, error);
			  
  }
  

  
  /** Received config packet from the CC2420 config connection */
  event message_t *ccReceiveConfig.receive(message_t *msg, void *payload, uint8_t len) { 
    
    uint8_t i,iCurMsg=0;
	bool bFoundMsg = FALSE;

	  call Leds.led1Toggle();
	
    // activate the buffer for sending
    for(i=0; i<GATEWAY_BUFFER_SIZE; i++) {
      if (msg == &(m_lmx_CFG_Buffer[i])) {
        m_lmx_CFG_Locked[i] = TRUE;
        m_lmx_CFG_Length[i] = len;
        m_lmx_CFG_Send[i] = TRUE;
        bFoundMsg = TRUE;
        iCurMsg = i;
        break;
      }
    }

    // first message is not from the buffer - copy it into it
    if (bFoundMsg == FALSE) {
      uint8_t j,*pSrc,*pDest;

      m_lmx_CFG_Locked[0] = TRUE;
      m_lmx_CFG_Send[0] = TRUE;
      m_lmx_CFG_Length[0] = len;

      pSrc = (uint8_t*)msg;
      pDest= (uint8_t*)&(m_lmx_CFG_Buffer[0]);
      for (j=0; j<sizeof(message_t); j++) {
        *(pDest++) = *(pSrc++);
      }
      
      m_lmx_CFG_Locked[1] = TRUE;
      m_lmx_CFG_Send[1] = FALSE;
      post lmx_CFG_SendNextTask();
      return &(m_lmx_CFG_Buffer[1]);
    }
    
    // start processing if there is no lock yet
    post lmx_CFG_SendNextTask();
  
    // search for the next free buffer
    atomic {
      for (i=0; i<GATEWAY_BUFFER_SIZE; i++) {
        uint8_t icheck = (m_lmx_CFG_Next + i)%GATEWAY_BUFFER_SIZE;
        if (m_lmx_CFG_Locked[icheck] == FALSE) {
          m_lmx_CFG_Locked[icheck] = TRUE;
          m_lmx_CFG_Send[icheck] = FALSE;
          return &(m_lmx_CFG_Buffer[icheck]);
        }
      }
    }

    // none free - drop message
    m_lmx_CFG_Send[iCurMsg] = FALSE;
  	return msg;
  }
  
  /** Received data packet from the CC2420 data connection */
  event message_t *ccReceiveData.receive(message_t *msg, void *payload, uint8_t len) { 
	uint8_t iCurMsg=0;
	bool bFoundMsg = FALSE;
  
    // control messages have a header, data not. 
    // Insert header for transmission over bluetooth
    uint8_t i,*pay = (uint8_t*)payload;
    for(i=len; i>0; i--) pay[i] = pay[i-1];
    pay[0] = 7;		// this is a data message, see TitanComm.h
    len++;
  
    call Leds.led2Toggle();
	
    // activate the buffer for sending
    for(i=0; i<GATEWAY_BUFFER_SIZE; i++) {
      if (msg == &(m_lmx_DAT_Buffer[i])) {
        m_lmx_DAT_Locked[i] = TRUE;
        m_lmx_DAT_Length[i] = len;
        m_lmx_DAT_Send[i] = TRUE;
        bFoundMsg = TRUE;
        iCurMsg = i;
        break;
      }
    }

    // first message is not from the buffer - copy it into it
    if (bFoundMsg == FALSE) {
      uint8_t j,*pSrc,*pDest;

      m_lmx_DAT_Locked[0] = TRUE;
      m_lmx_DAT_Send[0] = TRUE;
      m_lmx_DAT_Length[0] = len;

      pSrc = (uint8_t*)msg;
      pDest= (uint8_t*)&(m_lmx_DAT_Buffer[0]);
      for (j=0; j<sizeof(message_t); j++) {
        *(pDest++) = *(pSrc++);
      }
      
      m_lmx_DAT_Locked[1] = TRUE;
      m_lmx_DAT_Send[1] = FALSE;
      post lmx_DAT_SendNextTask();
      return &(m_lmx_DAT_Buffer[1]);
    }
    
    // start processing if there is no lock yet
    post lmx_DAT_SendNextTask();
  
    // search for the next free buffer
    atomic {
      for (i=0; i<GATEWAY_BUFFER_SIZE; i++) {
        uint8_t icheck = (m_lmx_DAT_Next + i)%GATEWAY_BUFFER_SIZE;
        if (m_lmx_DAT_Locked[icheck] == FALSE) {
          m_lmx_DAT_Locked[icheck] = TRUE;
          m_lmx_DAT_Send[icheck] = FALSE;
          return &(m_lmx_DAT_Buffer[icheck]);
        }
      }
    }
    
    // none free - drop message
    m_lmx_DAT_Send[iCurMsg] = FALSE;
  	return msg;
  }

}
