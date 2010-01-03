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
  * TitanTaskSynchronizer.nc
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  *
  * Computes the Synchronizer over a defined length of data. All input is 
  * assumed to be uint16_t.
  * 
  * Configuration is one byte:
  * [uiWindowSize]
  * Window size = 2^uiWindowSize
  * 
  */

module TitanTaskSynchronizer {
	uses interface Titan;
}

implementation {

	// the data used by the task should always be 
	// organized in a structure.
	typedef struct SynchronizerTaskData {
	  uint32_t uiLastMult;
	  uint16_t uiWindowSize;
	  uint16_t uiCurPos;
	  uint8_t bPass;
	} SynchronizerTaskData;

    event error_t Titan.init() {
      return SUCCESS;
    }

	/**
	* Returns the universal task identifier of the task. These identifiers are 
	* stored in TitanTaskUIDs.h and should be statically programmed.
	* @return Universal Task Identifier
	*/
	event TitanTaskUID Titan.getTaskUID() {
		return TITAN_TASK_SYNCHRONIZER;
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

		SynchronizerTaskData* pData;

		// define number of i/o-ports		
		pConfig->inPorts  = 1;
		pConfig->outPorts = 1;
		
		////////////////////////////////////////////////////////////////////////
		// Application dependent code
		
		if ( pConfig->configLength != 2 ) {
		  dbg( "TitanTask", "Task Synchronizer: Unknown configuration size\n");
  		  call Titan.issueError(ERROR_CONFIG);
		  return FALSE;
		}
		
		
		// allocate memory for the data structure
		pConfig->pTaskData = call Titan.allocMemory(pConfig->taskID,sizeof(SynchronizerTaskData));
		if ( pConfig->pTaskData == NULL ) {
			call Titan.issueError(ERROR_NO_MEMORY);
		}
		pData = (SynchronizerTaskData*)pConfig->pTaskData;

		pData->uiWindowSize = ((uint16_t)pConfig->configData[0]<<8) + pConfig->configData[1];
		pData->uiCurPos = 0;
		pData->uiLastMult = 0;
		pData->bPass = 0;
		
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
	    dbg("TitanTask", "Starting Synchronizer task\n");
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
		SynchronizerTaskData *pData;
		TitanPacket *pPacketIn;
		
		uint16_t i;
		uint16_t uiCurPos = pData->uiCurPos;
		uint16_t *pIn;
		
		if ( pConfig == NULL ) {
			dbg("TitanTask","WARNING: Got no context in task Synchronizer\n");
			return;
		}
		
		pData  = (SynchronizerTaskData*)pConfig->pTaskData;
		
		// get in/out packets
		pPacketIn  = call Titan.getNextPacket( pConfig->taskID, 0 );
		

        if ( pPacketIn == NULL ) {
          call Titan.issueError(ERROR_PACKET);
          return;
        }

        dbg( "TitanTask", "Task Synchronizer: Running\n" );

#ifdef MEASURE_CYCLES
        P6OUT |=  BIT1;  // set on
#endif

        pIn  = (uint16_t*)pPacketIn->data;
        
        for (i=0; i<pPacketIn->length>>1;i++) {
          uint32_t uiMult, uiMultSum;
          
          uiMult = (*pIn) * (*pIn);
          
          uiMultSum = uiMult + pData->uiLastMult;
          
          pData->uiLastMult = uiMult;
          
          uiCurPos++;
          
          dbg( "TitanTask", "Sum: %i \n",uiMult + pData->uiLastMult);

          // band finished?
          if ( uiMultSum > 10000 ) {
            uint8_t j;
            uint8_t *pDataIn, *pDataOut;
            TitanPacket *pPacketOut = call Titan.allocPacket( pConfig->taskID, 0 );
            
            // check if really got a packet
            if ( pPacketOut == NULL ) {
              call Titan.issueError(ERROR_PACKET);
              return;
            }
            
            pPacketOut->type = pPacketIn->type;
            pPacketOut->length = pPacketIn->length;
            
            pDataIn = pPacketIn->data;
            pDataOut = pPacketOut->data;
            for(j=0;j<pPacketOut->length;j++) {
              *(pDataOut++) = *(pDataIn++);
            }
            
            dbg( "TitanTask", "Task Synchronizer: Sending packet\n");
            
#ifdef MEASURE_CYCLES
        P6OUT &= ~BIT1;  // set off
#endif
            call Titan.sendPacket(pConfig->taskID, 0, pPacketOut );
            
            pData->bPass = 1;
            
            break;
          }
          
          pIn++;
        } // foreach pPacket->length
        
		
		// TinyOS 2.0 scheduler drops out
        if ( call Titan.hasPacket( pConfig->taskID, 0 ) > 0 ) {
          call Titan.postExecution(pConfig,0);
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
		  call Titan.issueError(6);
		  return FAIL;
		}

		return SUCCESS;
	}

}
