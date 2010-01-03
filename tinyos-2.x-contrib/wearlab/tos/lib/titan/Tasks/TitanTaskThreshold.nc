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
  * TitanTaskThreshold.nc
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  *
  * Does a thresholding of the input data. Incoming are 16 bit, outgoing 8 bit 
  * data thresholded and numbered by the thresholds. Note: Thresholds must be 
  * ordered in ascending order.
  * 
  * Configuration:
  * [ num, values[num] ]
  * 
  * num is 1 byte, all values 2 bytes
  * 
  * Example:
  * The configuration 
  * [ 2, 10, 20 ] 
  * transform an input sequence 
  * [ 1, 39, 15, 4, 22 ] 
  * into the output:
  * [ 0,  2,  1, 0,  2 ]. 
  * 
  */

module TitanTaskThreshold {
	uses interface Titan;
}

implementation {

	// the data used by the task should always be 
	// organized in a structure.
	typedef struct ThresholdTaskData {
		uint8_t uiNum;
		uint16_t thresholds[];
	}ThresholdTaskData;

  
    event error_t Titan.init() {
      return SUCCESS;
    }

	/**
	* Returns the universal task identifier of the task. These identifiers are 
	* stored in TitanTaskUIDs.h and should be statically programmed.
	* @return Universal Task Identifier
	*/
	event TitanTaskUID Titan.getTaskUID() {
		return TITAN_TASK_THRESHOLD;
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

		ThresholdTaskData* pData;
		uint8_t i;

        if ( pConfig->configLength <= 1 ) {
          return FAIL;
        }

		// define number of i/o-ports		
		pConfig->inPorts  = 1;
		pConfig->outPorts = 1;
		
		if ( pConfig->configData[0] < 1 ) {
		  return FAIL;
		}
		
		// allocate memory for the data structure
		pConfig->pTaskData = call Titan.allocMemory(pConfig->taskID,pConfig->configLength+1);
		if ( pConfig->pTaskData == NULL ) {
			call Titan.issueError(ERROR_NO_MEMORY);
		}

		// for easy access		
		pData = (ThresholdTaskData*)pConfig->pTaskData;
		
		////////////////////////////////////////////////////////////////////////
		// Application dependent code
		
		pData->uiNum = pConfig->configData[0];
		
		for ( i=0; i < pData->uiNum; i++ ) {
		  pData->thresholds[i] = ((uint16_t)pConfig->configData[i*2+1]<<8) + ((uint16_t)pConfig->configData[i*2+2]);
		}
	
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

		ThresholdTaskData *pData;
		TitanPacket *pPacketIn;
		TitanPacket *pPacketOut;
		
		if ( pConfig == NULL ) {
			dbg("TitanTask","WARNING: Got no context in task Threshold\n");
			return;
		}
		
		pData  = (ThresholdTaskData*)pConfig->pTaskData;
		
		////////////////////////////////////////////////////////////////////////
		// Application dependent code

		// use these functions to access packets		

		// input
		pPacketIn = call Titan.getNextPacket( pConfig->taskID, 0 );
		

#ifdef MEASURE_CYCLES
        P6OUT |=  BIT1;  // set on
#endif

    if ( pPacketIn == NULL ) {
      call Titan.issueError(ERROR_IN_FIFO_EMPTY);
      dbg("TitanTask", "Task Threshold: NULL packet received\n");
      return;
    }
		pPacketOut = call Titan.allocPacket( pConfig->taskID, 0 );
    if ( pPacketOut == NULL ) {
      call Titan.issueError(ERROR_OUT_FIFO_FULL);
      dbg("TitanTask", "Task Threshold: NULL packet received\n");
      return;
    }

		/* TODO: handle different formats (AB: problem is that type is not correctly transmitted from task to task) */
		
		//if ( pPacketIn->type == TT_INT16 || pPacketIn->type == TT_UINT16 ) 
		{
			uint16_t *pDataIn  = (uint16_t*)pPacketIn->data;
			uint8_t *pDataOut = pPacketOut->data;
			uint8_t i,j;
			
			for ( i=0; i < (pPacketIn->length)>>1; i++ ) {
			  
			  uint16_t iCurrentValue = *pDataIn;
			  uint16_t *pThresholds = (uint16_t*)(pData+1);
			  
			  // find the first threshold that is bigger than the value
			  for (j=0; j< pData->uiNum; j++ ) {
			    if ( iCurrentValue < *(pThresholds++) ) break;
			  }
			  *pDataOut = j;
			  
			  dbg("TitanTask", "Threshold in: %4i out %4i\n", *pDataIn, *pDataOut );

			  pDataIn++;pDataOut++;
			}
			
			pPacketOut->length = (pPacketIn->length)>>1;
			
		}
		/*
		else if ( pPacketIn->type == TT_INT8 || pPacketIn->type == TT_UINT8 ) {
			uint8_t  *pDataIn  = (uint8_t*)pPacketIn->data;
			uint8_t *pDataOut = pPacketOut->data;
			uint8_t i,j;
			
			for ( i=0; i < pPacketIn->length; i++ ) {
			  
			  uint8_t iCurrentValue = *pDataIn;
			  uint16_t *pThresholds = (uint16_t*)(pData+1);
			  
			  // find the first threshold that is bigger than the value
			  for (j=0; j< pData->uiNum; j++ ) {
			    if ( iCurrentValue < *(pThresholds++) ) break;
			  }
			  *pDataOut = j;
			  
			  dbg("TitanTask", "Threshold in: %4i out %4i\n", *pDataIn, *pDataOut );
			  
			  pDataIn++;pDataOut++;
			}
			
			pPacketOut->length = pPacketIn->length;
		} else {
		  dbg( "TitanTask", "Threshold: unknown data type\n" );
      call Titan.issueError(ERROR_TYPE);
		  return;
		}
		*/
		
		pPacketOut->type = TT_UINT8;

#ifdef MEASURE_CYCLES
        P6OUT &= ~BIT1;  // set off
#endif
		call Titan.sendPacket( pConfig->taskID, 0, pPacketOut );

		
		// TinyOS 2.0 scheduler drops out
        if ( call Titan.hasPacket( pConfig->taskID, uiPortID ) > 0 ) {
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
