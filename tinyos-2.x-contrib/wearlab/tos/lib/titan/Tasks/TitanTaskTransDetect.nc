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
  * TitanTaskTransDetect.nc
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  *
  * Issues a packet if a signal changes. The packet contains the new value. If 
  * there are multiple changes in one packet, only one packet is issued with 
  * all changes.
  * 
  */

module TitanTaskTransDetect {
	uses interface Titan;
}

implementation {

	// the data used by the task should always be 
	// organized in a structure.
	typedef struct TransDetectTaskData {
	  int16_t iCurValue;
	} TransDetectTaskData;

    event error_t Titan.init() {
      return SUCCESS;
    }

	/**
	* Returns the universal task identifier of the task. These identifiers are 
	* stored in TitanTaskUIDs.h and should be statically programmed.
	* @return Universal Task Identifier
	*/
	event TitanTaskUID Titan.getTaskUID() {
		return TITAN_TASK_TRANSDETECT;
	}

	/**
	* Initializes a component and passes a configuration structure. In the pInOut 
	* structure, the task returns its configuration, ie. how many input and output 
	* ports it reserves according to the configuration received.
	* @param pConfig Pointer to the configuration structure for the task
	* @param pInOut  Pointer to the resulting input and output port configuration
	* @return Whether the task has successfully been initialized
	*/
	event error_t Titan.configure( TitanTaskConfig* pConfig ) {

		TransDetectTaskData* pData;

		// define number of i/o-ports		
		pConfig->inPorts  = 1;
		pConfig->outPorts = 1;
		
		////////////////////////////////////////////////////////////////////////
		// Application dependent code
		
		if ( pConfig->configLength != 0 ) {
		  dbg( "TitanTask", "Task TransDetect: Unknown configuration size\n");
  		  call Titan.issueError(ERROR_CONFIG);
		  return FALSE;
		}
		
		// allocate memory for the data structure
		pConfig->pTaskData = call Titan.allocMemory(pConfig->taskID,sizeof(TransDetectTaskData));
		if ( pConfig->pTaskData == NULL ) {
			call Titan.issueError(ERROR_NO_MEMORY);
		}
		pData = (TransDetectTaskData*)pConfig->pTaskData;

        pData->iCurValue = 0;

		return SUCCESS;
	}
	
	
	/**
	* Indicates that the task will be terminated. After this event has been 
	* received, the task should process no more data and free all resources 
	* allocated.
	* @return Whether the task has successfully been terminated.
	*/
	event error_t Titan.terminate( TitanTaskConfig* pConfig ) {
	
		error_t result = call Titan.freeMemory(pConfig->taskID, pConfig->pTaskData);
	
		////////////////////////////////////////////////////////////////////////
		// Application dependent code
	
		return result;
	}
	
	/**
	* Starts the operation of a task. This could also wake it up again.
	*/
	event error_t Titan.start( TitanTaskConfig* pConfig ) {
		return SUCCESS;
	}
	
	/**
	* Stops the operation of a task. It should then prepare a sleep mode.
	*/
	event error_t Titan.stop( TitanTaskConfig* pConfig ) {
		return SUCCESS;
	}
	
	////////////////////////////////////////////////////////////////////////////
	// This is the actual working thread	
	event void Titan.execute( TitanTaskConfig* pConfig, uint8_t uiPortID ) {

        // get context stuff
		TransDetectTaskData *pData;
		
		TitanPacket *pPacketIn;
		TitanPacket *pPacketOut = NULL;

		
		if ( pConfig == NULL ) {
			dbg("TitanTask","WARNING: Got no context in task TransDetect\n");
			return;
		}
		
		pData  = (TransDetectTaskData*)pConfig->pTaskData;
		
		// get in/out packets
		pPacketIn  = call Titan.getNextPacket( pConfig->taskID, 0 );

#ifdef MEASURE_CYCLES
        P6OUT |=  BIT1;  // set on
#endif

        // check on data type
        if ( (pPacketIn->type == TT_INT8) || (pPacketIn->type == TT_UINT8) ) {
          
          uint8_t i, iCurValue = (uint8_t)pData->iCurValue;
          uint8_t* pDataIn = pPacketIn->data;
          uint8_t* pDataOut = NULL;
          
          // check on data
          for ( i=0; i < pPacketIn->length; i++ ) {
            
            // did data change?
            if ( *pDataIn != iCurValue ) {
              
              // already a packet allocated?
              if ( pPacketOut == NULL ) {
                pPacketOut = call Titan.allocPacket( pConfig->taskID, 0 );
                pPacketOut->length = 0;
                pPacketOut->type = pPacketIn->type;
                pDataOut = pPacketOut->data;
              }
              
              // update output data
              *pDataOut = *pDataIn;
              pDataOut++;
              pPacketOut->length++;
              
              iCurValue = *pDataIn;
            }
            pDataIn++;

          } // foreach PacketIn element

          pData->iCurValue = iCurValue;
          
        } else if ( (pPacketIn->type == TT_INT16) || (pPacketIn->type == TT_UINT16) ) {

          uint16_t i, iCurValue = (uint16_t)pData->iCurValue;
          uint16_t* pDataIn = (uint16_t*)pPacketIn->data;
          uint16_t* pDataOut = NULL;
          
          // check on data
          for ( i=0; i < (pPacketIn->length>>1); i++ ) {
            
            // did data change?
            if ( *pDataIn != iCurValue ) {
              
              // already a packet allocated?
              if ( pPacketOut == NULL ) {
                pPacketOut = call Titan.allocPacket( pConfig->taskID, 0 );
                pPacketOut->length = 0;
                pPacketOut->type = pPacketIn->type;
                pDataOut = (uint16_t*)pPacketOut->data;
              }
              
              // update output data
              *pDataOut = *pDataIn;
              pDataOut++;
              pPacketOut->length++;
              
              iCurValue = *pDataIn;
            }
            pDataIn++;
          } // foreach PacketIn element
          
          pData->iCurValue = iCurValue;

        } else { // unknow packet data type
          call Titan.issueError(ERROR_PACKET);
          dbg( "TitanTask", "TransDetect: ERROR: invalid packet data type\n");
          return;
        }
        
        // if there has been a change, a packet has been created, now send it
        if ( pPacketOut != NULL ) {

#ifdef MEASURE_CYCLES
        P6OUT &= ~BIT1;  // set off
#endif
          
          call Titan.sendPacket( pConfig->taskID, 0, pPacketOut );
        }
        
		// TinyOS 2.0 scheduler drops out
        if ( call Titan.hasPacket( pConfig->taskID, 0 ) > 0 ) {
          call Titan.postExecution(pConfig,uiPortID);
        }
	}

	/**
	* Is issued when a new packet arrives at the input port.
	* @param  iPort Port where the packet arrives
	* @return SUCCESS if the packet will be processed
	*/
	async event error_t Titan.packetAvailable( TitanTaskConfig* pConfig, uint8_t uiPort ) {

		// start the working task
		if (call Titan.postExecution(pConfig,uiPort) == FAIL ) {
		  call Titan.issueError(ERROR_PACKET);
		  return FAIL;
		}

		return SUCCESS;
	}

}
