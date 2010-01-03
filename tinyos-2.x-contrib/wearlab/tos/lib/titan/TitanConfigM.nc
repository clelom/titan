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
  * TitanConfigM.nc
  *
  * Communication module for the Tiny Task Network.
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  */

#include "TitanComm.h"

module TitanConfigM {
  
  provides interface Init;

  uses interface Boot;

  uses interface AMSend;
  uses interface AMPacket;
  uses interface Packet;
  uses interface PacketLink;
  uses interface SplitControl as AMControl;
  uses interface Receive;
  uses interface Receive as Snoop;
  uses interface Receive as LocalReceive;
  //uses interface AMSend as SerialSend;
  uses interface TitanSerialBufferSend;
  uses interface Packet as SerialPacket;
  
  uses interface TitanConfigure;
  uses interface TitanConfigCache;
  uses interface Leds;
  uses interface Random;
  
  #ifndef TOSSIM
    uses interface CC2420Packet;
  #endif
}

implementation {
  
    bool      m_bReconfiguring;   ///< TRUE if the configuration is going on
    bool      m_bDelayedReconfig; ///< FALSE if reconfiguration should take place immediately after last configuration message has been received, else a config start message is needed
    uint8_t   m_ConfigID;         ///< ID of the current reconfiguration
    uint16_t  m_MasterID;         ///< node which sent the configuration
    uint8_t   m_Tasks;            ///< number of tasks to be configured
    uint8_t   m_Connections;      ///< number of connections to be configured
    uint8_t   m_ConfiguredTasks;  ///< Number of already configured tasks
    uint8_t   m_ConfiguredConns;  ///< number of already configured connections
    message_t* m_pMsg;              ///< Message to be used for sending
    message_t m_Msg_Froward;
    bool      m_bMsgSending;      ///< TRUE when sending, don't overwrite!
    uint16_t  m_DiscNode;         ///< Node to which to answer the discovery
    uint16_t m_uiLastSentTaskID; ///< Last task sent with the service discovery reply message
    TCCTaskMsg* m_pTaskMsg;      ///< TaskMsg from which to start the reconfiguration
    
//#define CL_DEBUG
#ifdef CL_DEBUG
    uint8_t m_debug[10];
#endif
    
	//Paket Link Delay
	uint16_t m_Retrydelay;
  ////////////////////////////////////////////////////////////////////////////
  // Initialization interface
  
  command error_t Init.init() {
      m_bMsgSending = FALSE;
      m_bReconfiguring = FALSE;
      m_ConfigID = 0;
      m_Tasks = 0;
      m_Connections = 0;
      
      m_uiLastSentTaskID=0;

    return SUCCESS;
  }
  
  event void Boot.booted() {
    call AMControl.start();
  }
  
  event void AMControl.startDone(error_t error) {
  }

  event void AMControl.stopDone(error_t error) {
  }

  
  ////////////////////////////////////////////////////////////////////////////
  // Message processing

  error_t processConnMsg( TCCConnMsg* pConnMsg ) {
    int i;
    TCCConnection* pConn = (TCCConnection*)(pConnMsg+1);
    
    // check whether the configuration ID matches to see whether there really 
    // is a connection packet
    if ( (pConnMsg->configID) != m_ConfigID ) {
      return SUCCESS;
    }
    
    for ( i=0; i < (pConnMsg->numConns&0xF); i++ ) {
      
      error_t result = call TitanConfigure.processConnection(pConn[i].sourceRunID, pConn[i].sourcePortID,
                                            pConn[i].destRunID, pConn[i].destPortID );                                            
      if ( result == FAIL ) {
        signal TitanConfigure.issueError(-i, ERROR_NOT_IMPLEMENTED); //cl:debug
        return FAIL;
      }
      m_ConfiguredConns++;
      
    } // foreach connection
    
    return SUCCESS;
  }

  error_t processTaskMsg( TCCTaskMsg* pTaskMsg ) {

    int i;
    TCCTaskDesc *pTaskDesc = (TCCTaskDesc*)(pTaskMsg+1);

    // check whether the configuration ID matches to see whether this really 
    // is a task packet for the current configuration
    if ( (pTaskMsg->configID) != m_ConfigID ) {
      signal TitanConfigure.issueError(157,ERROR_CONFIG);
      return FAIL;
    }

    // go through all task definitions
    for ( i=0; i < (pTaskMsg->numTasks&0xF); i++ ) {

      TitanTaskUID taskID = (((uint16_t)pTaskDesc->taskIDH)*256 + 
                             ((uint16_t)pTaskDesc->taskIDL));
      
      error_t result;
      
/*      { int q;
        dbg("TitanConfig", "CONFIG: Configuring task %i id %i with string:\n", 
                      taskID, pTaskDesc->runID );
        for ( q=0; q<pTaskDesc->configLength; q++ ) {
           dbg( "TitanConfig", "\"%i\"", ((uint8_t*)(pTaskDesc+1))[q] );
        }
        dbg( "TitanConfig", "\n" );
      }
*/      

      // check whether the task has previously already received a configuration message
      if ( call TitanConfigure.isTaskConfigured( taskID, pTaskDesc->runID) == FAIL ) {
        m_ConfiguredTasks++;
      }
        // normal task description
      result = call TitanConfigure.processTaskConfig( 
                     taskID, pTaskDesc->runID, 
                     pTaskDesc->configLength, 
                     (pTaskDesc->configLength>0)? (uint8_t*)(pTaskDesc+1) : NULL );

      if ( result == FAIL ) {
        dbgerror("TitanConfig","Configuration failed on task (taskID=%i,runID=%i\n",taskID,pTaskDesc->runID);
        //signal TitanConfigure.issueError(-pTaskDesc->runID, ERROR_CONFIG); //cl:debug
        signal TitanConfigure.issueError(m_ConfigID, ERROR_CONFIG);
        return FAIL;
      }
                                             
          // set pointer to the next beginning task description
      pTaskDesc = (TCCTaskDesc*)(((void*)pTaskDesc) + sizeof(TCCTaskDesc)+ 
                  pTaskDesc->configLength );


    } // foreach task
      
    dbg("TitanConfig", "Configured %i tasks\n", m_ConfiguredTasks);

      // there may be an additional connection message at the end
    return processConnMsg( (TCCConnMsg*)pTaskDesc );
  }
  
  
  ////////////////////////////////////////////////////////////////////////////
  // ReceiveConfig interface
  
  task void sendConfigurationSuccess() {
    
    // reserve channel
    atomic {
        am_addr_t    myaddr = call AMPacket.address();
        TCCMsg       *pTCCMsg;
        TCCfgSuccess *pCfgSuc;
		m_pMsg= call TitanSerialBufferSend.getMsg();

        // check whether we send a serial or rf packet
        if ( call AMPacket.address() == 0 ) {
          SerialMsg* pSMsg = (SerialMsg*)(call SerialPacket.getPayload(m_pMsg,sizeof(TCCfgSuccess)+sizeof(TCCMsg)+sizeof(TCCMsg)));
          pTCCMsg = (TCCMsg*)pSMsg->data;
        } else {
          pTCCMsg = (TCCMsg*)(call Packet.getPayload(m_pMsg,sizeof(TCCfgSuccess) + sizeof(TCCMsg)));
        }

        // fill in message
        pCfgSuc = (TCCfgSuccess*)(pTCCMsg+1);
        pTCCMsg->verType   = (TITANCOMM_VERSION<<4)|TITANCOMM_CFGSUCC;
        pCfgSuc->configID = m_ConfigID;
        
        pCfgSuc->nodeIDH  = myaddr>>8;
        pCfgSuc->nodeIDL  = myaddr&0xFF;

		
        // send message
        if ( call AMPacket.address() == 0 ) {
          SerialMsg* pSMsg = (SerialMsg*)(call SerialPacket.getPayload(m_pMsg,sizeof(TCCfgSuccess)+sizeof(TCCMsg)+sizeof(TCCMsg)));
          pSMsg->address = m_MasterID;
          pSMsg->length = sizeof(TCCfgSuccess) + sizeof(TCCMsg);
          
          call TitanSerialBufferSend.send(m_pMsg, pSMsg->length+sizeof(SerialMsg));

        } else if (call AMPacket.address() != 0 &&  m_bMsgSending == FALSE ) {
		
          #ifndef TOSSIM
		  //m_Retrydelay=1;
			m_Retrydelay= (call Random.rand16())&0x1F;
      m_Retrydelay= m_Retrydelay+10;
          call PacketLink.setRetries(m_pMsg, TITAN_CFGMSG_RETRIES);
          call PacketLink.setRetryDelay(m_pMsg, m_Retrydelay );//TITAN_CFGMSG_RETRYDELAY
          #endif
		  
          if ( call AMSend.send( m_MasterID, m_pMsg, sizeof(TCCfgSuccess) + sizeof(TCCMsg) ) == SUCCESS ) {
            dbg("TitanConfig","Confirmation message sent to %i ...\n", m_MasterID);
            m_bMsgSending = TRUE;
          }
        }
        
      
#ifdef TOSSIM
      else dbgerror("TitanConfig", "Omitting configuration success message\n");
#else      
      else {
        post sendConfigurationSuccess(); // retry later on
      }
#endif
    }
  }
  
  task void replyDiscovery() {

      // check whether the sending channel is free
    atomic{

          int i;
          am_addr_t     myaddr    = call AMPacket.address();
          TCCMsg        *pTCCMsg;
          TCDiscoverRep *pDiscMsg;
          TitanTaskUID  *pTasks;
          uint8_t       *pData;
          uint16_t      totalTasks = call TitanConfigure.getTaskTypes( &pTasks );
		      m_pMsg= call TitanSerialBufferSend.getMsg();

          // check whether it is a serial packet or an rf
          if ( call AMPacket.address() == 0 ) {
            SerialMsg* pSMsg = (SerialMsg*)(call SerialPacket.getPayload(m_pMsg,1));
            pTCCMsg = (TCCMsg*)pSMsg->data;
          } else {
            pTCCMsg  = (TCCMsg*)call Packet.getPayload(m_pMsg,1);
          }

          // set pointers
          pDiscMsg = (TCDiscoverRep*)(pTCCMsg+1);
          pData = (uint8_t*)(pDiscMsg+1);
          
          // fill in data
          pTCCMsg->verType   = (TITANCOMM_VERSION<<4)|TITANCOMM_DISC_REP;
          pDiscMsg->numTasks = 0;
          pDiscMsg->nodeIDH  = myaddr>>8;
          pDiscMsg->nodeIDL  = myaddr&0xFF;
          
          // post the next set of tasks
          for ( i=m_uiLastSentTaskID; (i < totalTasks) && ((pData-(uint8_t*)m_pMsg->data) < TOSH_DATA_LENGTH-1); i++ ) {
            *(pData++) = pTasks[i]>>8;
            *(pData++) = pTasks[i]&0xFF;
            pDiscMsg->numTasks++;
          }

          // update counter
          if ( i >= totalTasks ) {
            m_uiLastSentTaskID=0;
          } else {
            m_uiLastSentTaskID=i;
            post replyDiscovery();
          }
          
          dbg("TitanConfig", "Replying to node %i with discovery information (size:%i)\n", m_DiscNode, sizeof(TCDiscover) + sizeof(TCCMsg) + pDiscMsg->numTasks*2*sizeof(uint8_t));
        
          if ( call AMPacket.address() == 0 ) {
            SerialMsg* pSMsg = (SerialMsg*)(call SerialPacket.getPayload(m_pMsg,1));
            pSMsg->address = m_MasterID;
            pSMsg->length = sizeof(TCDiscover) + sizeof(TCCMsg) + pDiscMsg->numTasks*2*sizeof(uint8_t);
            call TitanSerialBufferSend.send(m_pMsg, pSMsg->length+sizeof(SerialMsg));

          } else if ( call AMPacket.address() == 0 && m_bMsgSending == FALSE ){
            if ( call AMSend.send( m_DiscNode, m_pMsg, sizeof(TCDiscover) + sizeof(TCCMsg) + pDiscMsg->numTasks*2*sizeof(uint8_t) ) == SUCCESS ) {
              m_bMsgSending = TRUE;
            } else {
              dbg("TitanConfig", "ERROR: Sending service discovery reply message failed\n");
            }
          }
        
    } // atomic
  }
    
  event void AMSend.sendDone(message_t* pMsg, error_t error) {
     
    if ( m_pMsg == pMsg ) {
      m_bMsgSending = FALSE;
    }
	
    #if defined(PACKET_LINK) && !defined(TOSSIM)
  	if(call PacketLink.wasDelivered(pMsg)) {
  		call Leds.led1Toggle();
      } else{
  		call Leds.led0Toggle();
  		call Leds.led2On();
  	}
    #endif
     
//     if (m_uiLastSentTaskID != 0) post replyDiscovery();
  }
  
  // event void SerialSend.sendDone( message_t* pMsg, error_t error ) {
    // if ( m_pMsg == pMsg ) {
      // m_bMsgSending = FALSE;
    // }
  // }

  void checkReconfiguration() {
    // message processed, now check whether everything has been configured
    if ( m_bReconfiguring == TRUE ) {
      
      // too many configurations received?
      if ( (m_ConfiguredTasks > m_Tasks) || (m_ConfiguredConns > m_Connections) ) {
        dbg("TitanConfig","TitanConfigM: Too many tasks or connections!\n");
        m_bReconfiguring = FALSE;

        // notify the configurer
        signal TitanConfigure.issueError(-5,ERROR_CONFIG);


      // all configuration data received?
      } else if ( (m_ConfiguredTasks == m_Tasks ) && 
                  (m_ConfiguredConns == m_Connections) ) {

        if ( m_bDelayedReconfig == FALSE ) {
          dbg("TitanConfig","Configuration complete, starting up config %u ...\n", m_ConfigID);
          m_bReconfiguring = FALSE;

          if ( call TitanConfigure.startupConfiguration(m_Tasks) != SUCCESS ) {
            dbg( "TitanConfig", "FAILED\n");

            // notify the configurer
            signal TitanConfigure.issueError(-6,ERROR_CONFIG);

            return;
          }
        } else {
          dbg("TitanConfig","Configuration complete, waiting for start config on config %u ...\n", m_ConfigID);
        }
        
        post sendConfigurationSuccess();

      } // if done
      
    }
  }

  ////////////////////////////////////////////////////////////////////////////
  // PROCESS MESSAGE
  
  /*
   * returns true if the message needs to be buffered
   */
  bool processMessage( TCCMsg * pConfigMsg ) {
    bool retValue = FALSE;
    switch ( pConfigMsg->verType & 0x0F ) {
    
      case TITANCOMM_CONFIG: {
        
          // cast data stream into structures
          TCCConfigMsg *pConfigData = (TCCConfigMsg*)(pConfigMsg+1);
          TCCTaskMsg   *pTaskMsg    = (TCCTaskMsg*)(pConfigData+1);
          
          uint8_t uiTaskNum = pConfigData->numTasks;
          uint8_t uiConnNum = pConfigData->numConnections;
          
          // if this is a CLEAR message, we have no tasks - try a soft RESET
          #ifdef TITAN_USE_SOFT_RESET
          if ( uiTaskNum == 0 && (call AMPacket.address() != 0)) {
            atomic asm("br #0x4000;nop;nop;nop;nop"); // jump to MSP430 reset vector
          }
          #endif
          
          if ( (uiTaskNum <= TITAN_MAX_TASKS) && 
               (uiConnNum <= TITAN_MAX_FIFOS) ) {

                dbg( "TitanConfig", "Received valid config message. " \
                     "Starting configuration (%u) of %u tasks and %u connections\n", 
                     pTaskMsg->configID, uiTaskNum, uiConnNum );
                
                // keep information about whether the reconfiguration data 
                // is complete
                m_bReconfiguring = TRUE;
                m_bDelayedReconfig = (pConfigData->delayedReconfig == 1);
                m_ConfigID    = pTaskMsg->configID;
                m_Tasks       = uiTaskNum;
                m_Connections = uiConnNum;
                m_MasterID    = (uint16_t)pConfigData->masterIDH*256+(uint16_t)pConfigData->masterIDL;
                m_ConfiguredTasks = 0;
                m_ConfiguredConns = 0;

                // stores the task message until it is being executed
                m_pTaskMsg = pTaskMsg;
                
                // terminate tasks and initiate configuration -> resetConfigurationDone() will continue handling the message
                call TitanConfigure.resetConfiguration();
                retValue = TRUE;
                
          } else {
                dbg( "TitanConfig", "Too many taks or connections requested.\n" );
                // notify the configurer
                signal TitanConfigure.issueError(-2,ERROR_CONFIG);
          }
        
        } break;
      case TITANCOMM_CACHE_START:{
          TCCMsg *pCacheMsg = call TitanConfigCache.getCacheEntry(pConfigMsg);
          
          // check whether cache entry exists
          if ( pCacheMsg != NULL ) {
            processMessage(pCacheMsg);
            retValue = FALSE; // do not keep the message!
          } else {
            signal TitanConfigure.issueError(-2,ERROR_NO_CACHE_ENTRY);
          }
        } break;
      case TITANCOMM_CACHE_STORE:{
          if ( call TitanConfigCache.storeCacheEntry( pConfigMsg ) != SUCCESS ) {
            signal TitanConfigure.issueError(-2,ERROR_CACHE);
          }
        } break;
      case TITANCOMM_CFGTASK:{
            // extract message and process it            
          TCCTaskMsg   *pTaskMsg   = (TCCTaskMsg*)(pConfigMsg+1);
          
          if ( m_bReconfiguring == FALSE ) {
            dbg( "TitanConfig", "Received task configuration message without being reconfiguring\n");
            break;
          }

          if ( m_pTaskMsg != NULL ) {
            dbg( "TitanConfig", "Received new task configuration message before having process first config message\n");
            signal TitanConfigure.issueError(-4,ERROR_CONFIG);
            break;
          }

          if ( (pTaskMsg->configID) == m_ConfigID ) {
              if ( processTaskMsg ( pTaskMsg ) == FAIL ) {
                  signal TitanConfigure.issueError(-3,ERROR_CONFIG);
              }
          } else {
            dbg( "TitanConfig", "Received task message belonging to another configuration (%u)\n", 
                 (pTaskMsg->configID));
          }
        
        } break;
        
      case TITANCOMM_CFGCONN:{
        
         TCCConnMsg* pConnMsg = (TCCConnMsg*)(pConfigMsg+1);
         
          if ( m_bReconfiguring == FALSE ) {
            dbg( "TitanConfig", "Received connection configuration message without being reconfiguring\n");
            break;
          }

          if ( m_pTaskMsg != NULL ) {
            dbg( "TitanConfig", "Received new connection configuration message before having process first config message\n");
            signal TitanConfigure.issueError(-4,ERROR_CONFIG);
          }

          if ( (pConnMsg->configID) == m_ConfigID ) {
            if( processConnMsg( pConnMsg ) == FAIL ) {
              signal TitanConfigure.issueError(-4,ERROR_CONFIG);
            }
          } else {
            dbg( "TitanConfig", "Received task message belonging to another configuration (%u)\n", 
                 (pConnMsg->configID));
          }
        
        } break;

      ///////////////////////////////////////////////////////
      // CFGSTART Message
      case TITANCOMM_CFGSTART: {
        TCCCfgStartMsg* pCfgStart = (TCCCfgStartMsg*)(pConfigMsg+1);
        
        if ( pCfgStart->configID == m_ConfigID ) {
          m_bDelayedReconfig = FALSE;
          dbg( "TitanConfig", "Received start message for configuration %u\n", m_ConfigID );
        }
        
        break;
        }
      ///////////////////////////////////////////////////////
      // DISCOVER Message
      case TITANCOMM_DISCOVER: {
          TCDiscover* pDiscMsg = (TCDiscover*)(pConfigMsg+1);
          m_DiscNode = (uint16_t)pDiscMsg->sourceIDH*256 + (uint16_t)pDiscMsg->sourceIDL;
          post replyDiscovery();
        
        } break;
        
      ///////////////////////////////////////////////////////
      // FORWARD Message
      case TITANCOMM_FORWARD: {
      
          TCForwardMsg* pFwdMsg = (TCForwardMsg*)(pConfigMsg+1);
          uint8_t*      pMsgData= (uint8_t*)(call Packet.getPayload(&m_Msg_Froward,1));


          // if busy sending another packet, drop it -> congestion
          if ( m_bMsgSending == TRUE ) {
             dbg( "TitanConfig", "Dropping forward message\n" );
             break;
          }
          
          // check if this is the last hop
          if ( pFwdMsg->hops == 0 ) {
            dbg( "TitanConfig", "ERROR: forward message with zero hops received\n");
            break;
          }
          
          //dbg("TitanConfig", "hops: %i, length: %i\n", pFwdMsg->hops, pFwdMsg->length );
          
          if ( pFwdMsg->hops == 1 ) {
            int i;
            uint8_t *pData  = (uint8_t*)(pFwdMsg + 1); // after nodeID
            uint16_t uiNode = (((uint16_t)pData[0])<<8) + pData[1];
            pData+=2;
            
            // does the forward message have to stay here?
            if ( uiNode == (call AMPacket.address()) ) {
              return processMessage( (TCCMsg*)pData );
            }
            
            // strip message and send directly
            for ( i=0; i<pFwdMsg->length; i++ ) {
              pMsgData[i] = pData[i];
//                  dbg( "TitanConfig", "FORWARD: %3i\n", pData[i] ); 
            }
            pMsgData[i] = 0; // terminate message right
            
            // send it
            if ( call AMSend.send( uiNode, &m_Msg_Froward, pFwdMsg->length ) == SUCCESS ) {
              m_bMsgSending = TRUE;
            } 
            
          } else { // more than 1 hop
            
            // forward to next node, stripping the path

            int i;
            uint8_t *pData  = (uint8_t*)(pFwdMsg + 1); // after nodeID
            uint8_t *pDest  = (uint8_t*)(call Packet.getPayload(&m_Msg_Froward,1));
            uint16_t uiNode = (((uint16_t)pData[0])<<8) + pData[1];
            pData+=2;

            *(pDest++) = pFwdMsg->hops-1;
            *(pDest++) = pFwdMsg->length;
            
            for (i=0; i < pFwdMsg->hops*2+pFwdMsg->length; i++ ) {
              *(pDest++) = *(pData++);
            }
            
            // ok, new message generated, now send it
            if ( call AMSend.send( uiNode, &m_Msg_Froward, pFwdMsg->length + (pFwdMsg->hops-1+1)*sizeof(uint16_t) ) == SUCCESS ) {
              m_bMsgSending = TRUE;
            } 
          } // multihop fwd message
        }
      default:
    } // switch TCCMsg->verType
    
    // check whether the reconfiguration is done
    checkReconfiguration();

    return retValue;
  }
  
  /**
   * Called when the configuration has been cleared and the device is ready to be reprogrammed.
   */
  event error_t TitanConfigure.resetConfigurationDone() {

    // now process the tasks delivered with the message
    if ( processTaskMsg( m_pTaskMsg ) == FAIL ) {
      dbg("TitanConfig","Configuration failed");
      signal TitanConfigure.issueError(156,ERROR_CONFIG);
    }
    checkReconfiguration();
    
    m_pTaskMsg = NULL; // signal that first config message has been processed
    
    return SUCCESS;
  }
  
  
  event message_t* LocalReceive.receive(message_t* pMsg, void* payload, uint8_t len) {

    //TODO: handle local messages

    TCCMsg *pConfigMsg = (TCCMsg*) payload;
    if ( (pConfigMsg->verType >> 4) == TITANCOMM_VERSION ) {
        processMessage( pConfigMsg );
    }
    checkReconfiguration();

    return pMsg;
  }

  event message_t* Snoop.receive(message_t* pMsg, void* payload, uint8_t len) {
    int i;
    uint8_t  datalength = len;
    uint8_t* data = (uint8_t*)payload;
    SerialMsg* pSMsg;
    message_t* pBFMsg;

    if ( call AMPacket.address() != 0 ) return pMsg;

    pBFMsg= call TitanSerialBufferSend.getMsg();
    pSMsg = (SerialMsg*)(call SerialPacket.getPayload(pBFMsg,1));
    
    pSMsg->address = call AMPacket.destination(pMsg);
    pSMsg->length = datalength;
    
    #ifndef TOSSIM
    pSMsg->rssi = call CC2420Packet.getRssi(pMsg);
    #endif

    for ( i=0; i<datalength; i++ ) {
      pSMsg->data[i] = data[i];
    }
    call TitanSerialBufferSend.send(pBFMsg, pSMsg->length+sizeof(SerialMsg));

    return pMsg;
  }
  
  
  
  event message_t* Receive.receive(message_t* pMsg, void* payload, uint8_t len) {
    
    TCCMsg *pConfigMsg = (TCCMsg*) payload;
	
    if ( call AMPacket.address() == 0 ) {
      signal Snoop.receive( pMsg, payload, len );
    }


#ifdef MEASURE_CYCLES
        P6OUT |=  BIT1;  // set on
#endif
    
    dbg("TitanConfig", "Received config message version %i, type %i\n", 
        pConfigMsg->verType >> 4, pConfigMsg->verType & 0x0F );
    
    if ( (pConfigMsg->verType >> 4) == TITANCOMM_VERSION ) {
    
        // process message. If failed, send it over the serial port
        if ( processMessage( pConfigMsg ) == FALSE ) {
          signal Snoop.receive( pMsg, payload, len );
        }
    } else { // if version is equal
        dbg( "TitanConfig", "Received config message of unknown version! (%u)\n", (pConfigMsg->verType >> 4) );
    }

    return pMsg;
  }

  ////////////////////////////////////////////////////////////////////////////
  // ERROR HANDLING

  //todo: protect those errors from being overwritten before having been sent.
  uint8_t m_uiErrorSource;
  uint8_t m_uiErrorType;
  bool    m_bErrorWaiting;
  
  task void sendError() {
    atomic {
        am_addr_t    myaddr = call AMPacket.address();
        TCCMsg     *pTCCMsg;
        TCErrorMsg *pErrMsg;
		    m_pMsg= call TitanSerialBufferSend.getMsg();

        // check whether we send a serial or rf packet
        if ( myaddr == 0 ) {
          SerialMsg* pSMsg = (SerialMsg*)(call SerialPacket.getPayload(m_pMsg,1));
          pTCCMsg = (TCCMsg*)pSMsg->data;
        } else {
          pTCCMsg = (TCCMsg*)(call Packet.getPayload(m_pMsg,1));
        }

        // fill in message
        pErrMsg = (TCErrorMsg*)(pTCCMsg+1);
        pTCCMsg->verType   = (TITANCOMM_VERSION<<4)|TITANCOMM_ERROR;
        pErrMsg->nodeID = myaddr;
        atomic {
          pErrMsg->configID  = m_ConfigID;
          pErrMsg->errSource = m_uiErrorSource;
          pErrMsg->errType   = m_uiErrorType;
        }
        
#ifdef CL_DEBUG
         {
          uint8_t q;
          uint8_t* pAddData = ((uint8_t*)pTCCMsg)+sizeof(TCErrorMsg) + sizeof(TCCMsg);
          for (q=0;q<sizeof(m_debug);q++) {
            *(pAddData++) = m_debug[q];
          }
         }
#endif

        // send message
        if ( call AMPacket.address() == 0 ) {
          SerialMsg* pSMsg = (SerialMsg*)(call SerialPacket.getPayload(m_pMsg,1));
          pSMsg->address = m_MasterID;
          pSMsg->length = sizeof(TCErrorMsg) + sizeof(TCCMsg);
		      call TitanSerialBufferSend.send(m_pMsg, pSMsg->length+sizeof(SerialMsg));
        } else if (call AMPacket.address() != 0 &&  m_bMsgSending == FALSE ) {
#ifdef CL_DEBUG
          if ( call AMSend.send( m_MasterID, m_pMsg, sizeof(TCErrorMsg) + sizeof(TCCMsg) + sizeof(m_debug) ) == SUCCESS ) {
#else
          if ( call AMSend.send( m_MasterID, m_pMsg, sizeof(TCErrorMsg) + sizeof(TCCMsg) ) == SUCCESS ) {
#endif
            m_bMsgSending = TRUE;
          }
        }
        
      
#ifndef TOSSIM
      else post sendError(); // retry later on
#else
       else dbgerror("TitanConfig", "Omitting error message (source=%u,type=%u)\n",m_uiErrorSource,m_uiErrorType);

#endif
      m_bErrorWaiting = FALSE;
    } // end atomic
  }
  
  /**
  * Forwards an error encountered to the 
  */
  async event error_t TitanConfigure.issueError( uint8_t uiSource, uint8_t uiError ) {
    atomic {
      if (m_bErrorWaiting == FALSE) {
        m_uiErrorSource = uiSource;
        m_uiErrorType   = uiError;
      }
    }
    dbg("TitanConfig","ERROR: Source %i, ID %i\n",uiSource,uiError);
    post sendError();
    return SUCCESS;
  }


}
