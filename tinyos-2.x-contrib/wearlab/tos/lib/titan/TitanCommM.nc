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
  * TitanCommM.nc
  *
  * Communication module for the Tiny Task Network.
  *
  * Configuration data format: 
  * [ uint8_t nodeIDH, uint8_t nodeIDL, uint8_t portID ]
  * where only Input ports are mapped to other nodes. Incoming packets tell to 
  * which port they must be routed.
  * 
  * CAN ONLY BE INSTANTIATED ONCE!
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  */

#include "TitanComm.h"
#include "TitanInternal.h"
  
module TitanCommM {

	// communication
	uses interface AMSend;
	uses interface AMPacket;
	uses interface Packet;
#ifndef TOSSIM
	uses interface CC2420Packet;
	uses interface PacketLink;
#endif
	uses interface SplitControl as AMControl;
	uses interface Receive;
  uses interface Receive as Snoop;
 	
 	// Emulate an interface to TitanM
 	uses interface Titan;

	// link to serial interface
	//uses interface AMSend as SerialSend;
	uses interface TitanSerialBufferSend;
	uses interface Packet as SerialPacket;

	uses interface Leds;
	
     //Random Number Generator
	uses interface Random;
  
  uses interface Receive as LocalReceive;
}

implementation {

    uint8_t m_taskID;   // the task ID assigned to the COMTask
    uint8_t m_running;  // number of times the COMTask has been instantiated
    bool    m_bRFBusy;  // already sending a message?
    

    message_t m_SendBuffer;      // AM message to be sent
    uint8_t   m_waitingPorts[TC_MAX_CONNECTIONS]; // ports waiting with messages
    
//cl    message_t* m_Msg;
    bool      m_bSerialSending;
    
    TitanTaskConfig* m_pConfig;
    
	//Paket Link Delay
	uint16_t m_Retrydelay;
	
	
	////////////////////////////////////////////////////////////////////////////
	// Titan interface

  event error_t Titan.init() {
    int i;
    for (i=0;i<sizeof(m_waitingPorts);i++) m_waitingPorts[i] = -1;
    return SUCCESS;
  }

  event TitanTaskUID Titan.getTaskUID() {
    return TITAN_COMM_MODULE;
  }
	
	event error_t Titan.configure(TitanTaskConfig* pConfig) {
	  int i;
	  TCConnMap *pConns;
    m_pConfig = pConfig;
      
    // register a task ID for received packets
    m_taskID = pConfig->taskID;

    if ( pConfig->configLength == 0 ) {
      dbg( "TitanComm", "Comm Module config: only receiving\n", pConfig->taskID );
      pConfig->outPorts  = 127; // infinite, is read from incoming packets
      m_running++;
      return SUCCESS;
    }

    dbg( "TitanComm", "New task with ID: %i: Communication module\n", pConfig->taskID );
    
    pConns = (TCConnMap*)call Titan.allocMemory( pConfig->taskID, pConfig->configLength/3*sizeof(TCConnMap)+sizeof(uint8_t)  );

    if ( pConns == NULL ) {
      dbg( "TitanComm", "Comm Module config: not enough memory\n", pConfig->taskID );
      return FAIL;
    }
    
    // read the connection data
    for ( i=0; i < (pConfig->configLength/3); i++ ) {
      pConns[i].nodeIDH = pConfig->configData[i*3  ];
      pConns[i].nodeIDL = pConfig->configData[i*3+1];
      pConns[i].portID  = pConfig->configData[i*3+2];
    }
    
    pConfig->inPorts   = pConfig->configLength/3;
    pConfig->outPorts  = 127; // infinite, is read from incoming packets
    pConfig->pTaskData = pConns;
    
    m_running++;
      
	  return SUCCESS;
	}
	
	event error_t Titan.terminate(TitanTaskConfig* pConfig) {
      m_running--;
	  
	  return call Titan.freeMemory( pConfig->taskID, pConfig->pTaskData );
	  
	}
	
	event error_t Titan.start(TitanTaskConfig* pConfig) {
      dbg( "TitanComm", "Starting Communication module with %i inputs and %i outputs\n", pConfig->inPorts, pConfig->outPorts );
      m_bRFBusy = FALSE;
	  return SUCCESS;
	}
	
	event error_t Titan.stop(TitanTaskConfig* pConfig) {
	  return SUCCESS;
	}

	////////////////////////////////////////////////////////////////////////////
	// Sending packets

	event void Titan.execute(TitanTaskConfig* pConfig, uint8_t uiPort) {
  
    atomic {
      // before being able to do anything, the RF port must be free
  	  if ( m_bRFBusy == FALSE ) {
    		TCConnMap        *pConnMap;
    		TitanPacket      *pPacket;
    		TitanCommDataMsg *pCDMsg;
    		int i,curIndex=-1;

        // get waiting port
        for (i=0; i< sizeof(m_waitingPorts);i++) {
          if (m_waitingPorts[i] != (uint8_t)-1) {
            uiPort = m_waitingPorts[i];
            curIndex=i;
          }
        }

        // check whether there actually is a port
        if ( uiPort == (uint8_t)-1 ) {
          return;
        }
        
        // check whether the task has been correctly posted		
    		if ( pConfig == NULL ) {
          dbgerror( "TitanComm","WARNING: Could not retrieve context\n");
          //call Titan.issueError(ERROR_NO_CONTEXT);
          pConfig = m_pConfig;
          return;
        }

        // init structure pointers
    		pConnMap = pConfig->pTaskData;
    		pPacket = call Titan.getNextPacket( pConfig->taskID, uiPort );
    		
        // if no more packets are available, delete port from the list
    		if ( pPacket == NULL ) {
          m_waitingPorts[curIndex] = -1;
    		  //dbgerror( "TitanComm", "Could not get input packet!\n");
          //call Titan.issueError(ERROR_IN_FIFO_EMPTY);
    		  return;
    		}
  		
        // fill in connection information
        pCDMsg = (TitanCommDataMsg*)(call Packet.getPayload(&(m_SendBuffer),0));
//          pCDMsg->destNodeH = pConnMap[uiPort].nodeIDH;
//          pCDMsg->destNodeL = pConnMap[uiPort].nodeIDL;

    		if ( pCDMsg == NULL ) {
    		  dbgerror("TitanComm", "pCDMsg == NULL in %s: %s\n", __FILE__, __LINE__ );
    		  return;
    		}

        pCDMsg->destPort  = pConnMap[uiPort].portID;
        
        // copy packet data
        for(i=0; (i < pPacket->length) && (i < TITAN_PACKET_SIZE); i++ ) {
          pCDMsg->data[i] = pPacket->data[i];
        }
        pCDMsg->dataLength = i;
        
        // acknowledgments are only for config messages
        #ifndef TOSSIM
			//m_Retrydelay=1;
			m_Retrydelay= (call Random.rand16())&0x1F;
      m_Retrydelay= m_Retrydelay+10;
	        call PacketLink.setRetries(&m_SendBuffer, TITAN_DATAMSG_RETRIES);
			call PacketLink.setRetryDelay(&m_SendBuffer, m_Retrydelay);//TITAN_CFGMSG_RETRYDELAY
        #endif
		
		
        // send the data packet
        if ( call AMSend.send( (uint16_t)pConnMap[uiPort].nodeIDH * 256 + (uint16_t)pConnMap[uiPort].nodeIDL, 
                            &(m_SendBuffer), sizeof(TitanCommDataMsg) + pCDMsg->dataLength
                             ) == SUCCESS ) {
           dbg( "TitanComm", "Packet sent to %i\n", (uint16_t)pConnMap[uiPort].nodeIDH * 256 + (uint16_t)pConnMap[uiPort].nodeIDL);
           m_bRFBusy = TRUE; // store that the sending process is running

        } // if send failed
        else {
         dbg( "TitanComm", "CommModule: sending failed: destNode: %u size: %u\n", 
              (uint16_t)pConnMap[uiPort].nodeIDH * 256 + (uint16_t)pConnMap[uiPort].nodeIDL,
              sizeof(TitanCommDataMsg) +pCDMsg->dataLength );
              call Titan.postExecution(pConfig,uiPort);
        }
      } // if m_bRFBusy == TRUE else wait for sendDone, which will repost this task
      else {
        dbgerror("TitanComm", "Could not send packet: RF busy\n");
      }
    } //atomic
	}
	
	async event error_t Titan.packetAvailable(TitanTaskConfig* pConfig, uint8_t InPort) {

    int i;
  
    if ( InPort > TC_MAX_CONNECTIONS ) {
	    dbg( "TitanComm", "access to undefined connection %i\n", InPort );
	    return FAIL;
	  }

    dbg( "TitanComm", "Packet coming in on port %i\n", InPort );

    // schedule the sending task
    atomic {
      for (i=0;i<sizeof(m_waitingPorts);i++) {
        if (m_waitingPorts[i]==InPort) break;
        if (m_waitingPorts[i]==(uint8_t)-1) break;
      }
      // no port free? -> drop message
      if ( i==sizeof(m_waitingPorts) ) {
        call Titan.getNextPacket( pConfig->taskID, InPort );
        return FAIL;
      }
      m_waitingPorts[i] = InPort;
			
			call Titan.postExecution(pConfig,InPort);
			
    } // atomic
	  
	  return SUCCESS;
	}
  
	event void AMSend.sendDone(message_t* pMsg, error_t error) {
	  
	  int i;
    
    dbg("TitanComm","sendDone received on AMSend with 0x%X\n",pMsg);
	  
    // flag the RF to be free and post sendPacket in case there is another 
    // message to be sent	  
	  m_bRFBusy = FALSE;

#if defined(PACKET_LINK) && !defined(TOSSIM)
	 if(call PacketLink.wasDelivered(pMsg)) {
		 //call Leds.led1Toggle();
    } else{
		 //call Leds.led0Toggle();
		 //call Leds.led2On();
	 }
#endif 	
	
    // check whether there are more messages to be sent
    atomic {
  	  for (i=0; i<sizeof(m_waitingPorts); i++ ) {
        if (m_waitingPorts[i] != (uint8_t)-1 ) {
          call Titan.postExecution(m_pConfig,m_waitingPorts[i]);
          break;
        }
      } // for
    } // atomic
	}

	////////////////////////////////////////////////////////////////////////////
	// Communication interfaces
  
  message_t localReceive;
  
  event message_t* LocalReceive.receive(message_t* pMsg, void* payload, uint8_t len) {
  
    return signal Receive.receive(pMsg, payload, len);
  }
  
  event message_t* Snoop.receive(message_t* pMsg, void* payload, uint8_t len) {
  
    int i;
    uint8_t  datalength = call Packet.payloadLength(pMsg);
    uint8_t* data = (uint8_t*)(call Packet.getPayload( pMsg, 0 ));
    SerialMsg* pSMsg;
    message_t * m_Msg;

    call Leds.led1Toggle();
    
    if ( call AMPacket.address() != 0 ) return pMsg;

    m_Msg=call TitanSerialBufferSend.getMsg();
    pSMsg = (SerialMsg*)(call SerialPacket.getPayload(m_Msg,0));

    pSMsg->address = call AMPacket.destination(pMsg);

#ifndef TOSSIM
    pSMsg->rssi = call CC2420Packet.getRssi(pMsg);
#endif

    // add data message header
    pSMsg->length = datalength+1;
    pSMsg->data[0] = (TITANCOMM_VERSION<<4)|TITANCOMM_DATAMSG;
    
    // copy data
    for ( i=1; i<=datalength; i++ ) {
      pSMsg->data[i] = data[i-1];
    }
	  
	  call TitanSerialBufferSend.send(m_Msg, pSMsg->length + sizeof(SerialMsg));
    
    return pMsg;
  }
  
  
	event message_t* Receive.receive(message_t* pMsg, void* payload, uint8_t len) {
	  int i;
	  
	  // check whether this message is for the local node
	  TitanCommDataMsg* pDataMsg = (TitanCommDataMsg*)payload;
	  TitanPacket*      pPacket;
    message_t* m_Msg;
    
	  m_Msg=call TitanSerialBufferSend.getMsg();
    
    call Leds.led0Toggle(); // shows that a message has been seen
	  
    dbg( "TitanComm", "Packet received\n" );
    // node 0 is just a gateway to the PC
    if ( (call AMPacket.address()) == 0 ) {
      dbg( "TitanComm", "Forwarding to snoop interface\n" );
      signal Snoop.receive(pMsg, payload, len);
    } // if AMPacket.address()==0
    
    // is any instance running	  
	  if ( m_running == 0 ) {
        dbg( "TitanComm", "CommModule: Not running, dropping packet!\n" );
	    return pMsg;
	  }

//cl	  pDataMsg->destPort = 0;

      pPacket = call Titan.allocPacket( m_taskID, pDataMsg->destPort );

      if ( pPacket == NULL ) {
    	  dbg( "TitanComm", "CommModule: Could not allocate packet!\n" );
        call Titan.issueError(ERROR_OUT_FIFO_FULL);
    	  return pMsg;
      }

      // copy the data
      pPacket->length = pDataMsg->dataLength;
      for ( i=0; (i < pDataMsg->dataLength) && (i<TITAN_PACKET_SIZE); i++ ) {
        pPacket->data[i] = pDataMsg->data[i];
      }

      dbg("TitanComm", "Forwarding packet to port %i (taskID=%i,length=%i)\n", pDataMsg->destPort, m_taskID, pPacket->length);
      
	  if ( call Titan.sendPacket( m_taskID, pDataMsg->destPort, pPacket ) == FAIL ) {
    	  dbg( "TitanComm", "CommModule: Failed to send packet internally!\n" );
	  }
	  
	  return pMsg;
	}
	
	event void AMControl.startDone(error_t error) {
	}

	event void AMControl.stopDone(error_t error) {
	}

}
