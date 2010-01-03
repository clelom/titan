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
  * TitanTaskSum.nc
  *
  * @author Andreas Breitenmoser
  *
  * Computes the sum over a defined length of data.
  * All input is assumed to be int16_t.
  * 
  * Configuration is one byte:
  * [uiWindowSize, uiWindowShift]
  * 
  * uiWindowSize specifies over how many data items the sliding window goes: length of sliding window = uiWindowSize (max. 256).
  * uiWindowShift sets the number of data items the sliding window is shifted before the next sum value is calculated.  
  *
  * Created: 20.02.2008
  * Last modified: 20.02.2008
  *
  */

module TitanTaskSum
{
	uses interface Titan;
}
implementation
{
	// the data used by the task should always be 
	// organized in a structure.
	typedef struct SumTaskData
	{
	  int16_t iSum; //int32_t iSum;
	  uint8_t uiWindowSize;
	  uint8_t uiWindowShift;
	  uint8_t uiDataCounter;
	  int16_t *pCurEntry;
	} SumTaskData;

	
    event error_t Titan.init()
	{
      return SUCCESS;
    }

	
	/**
	* Returns the universal task identifier of the task. These identifiers are 
	* stored in TitanTaskUIDs.h and should be statically programmed.
	* @return Universal Task Identifier
	*/
	event TitanTaskUID Titan.getTaskUID()
	{
		return TITAN_TASK_SUM;
	}

	
	/**
	* Initializes a component and passes a configuration structure. In the pInOut 
	* structure, the task returns its configuration, ie. how many input and output 
	* ports it reserves according to the configuration received.
	* @param pConfig Pointer to the configuration structure for the task
	* @param pInOut  Pointer to the resulting input and output port configuration
	* @return Whether the task has successfully been initialized
	*/
	event error_t Titan.configure(TitanTaskConfig* pConfig)
	{
		SumTaskData* pData;
		uint8_t uiWindowSize, uiWindowShift;
		int16_t *pFilterData;
		uint16_t i;

		// define number of i/o-ports		
		pConfig->inPorts  = 1;
		pConfig->outPorts = 1;
		
		
		////////////////////////////////////////////////////////////////////////
		// Application dependent code
		
		if (pConfig->configLength != 2)
		{
		  dbg( "TitanTask", "Task Sum: Unknown configuration size.\n");
  		  call Titan.issueError(ERROR_CONFIG);
		  return FALSE;
		}
		
		uiWindowSize = pConfig->configData[0];
		uiWindowShift = pConfig->configData[1];
		
		// allocate memory for the data structure
		pConfig->pTaskData = call Titan.allocMemory(pConfig->taskID, (uiWindowSize)*sizeof(int16_t)+sizeof(SumTaskData));
		if (pConfig->pTaskData == NULL)
		{
			call Titan.issueError(ERROR_NO_MEMORY);
		}
		pData = (SumTaskData*)pConfig->pTaskData;

        pData->iSum = 0;
		pData->uiWindowSize = uiWindowSize;
		pData->uiWindowShift = uiWindowShift;
		pData->uiDataCounter = 1;
		
		pFilterData = pData->pCurEntry = (int16_t*)(pData+1);
		
		// init data
		for (i=0; i<((int16_t)uiWindowSize); i++)
		{
		  *(pFilterData++) = 0;
		}
	
		return SUCCESS;
	}
	
	
	/**
	* Indicates that the task will be terminated. After this event has been 
	* received, the task should process no more data and free all resources 
	* allocated.
	* @return Whether the task has successfully been terminated.
	*/
	event error_t Titan.terminate(TitanTaskConfig* pConfig)
	{
		error_t result = call Titan.freeMemory(pConfig->taskID, pConfig->pTaskData);
	
		////////////////////////////////////////////////////////////////////////
		// Application dependent code
	
		return result;
	}
	
	
	/**
	* Starts the operation of a task. This could also wake it up again.
	*/
	event error_t Titan.start(TitanTaskConfig* pConfig)
	{
	    dbg("TitanTask", "Starting Sum Task\n");
		return SUCCESS;
	}
	
	
	/**
	* Stops the operation of a task. It should then prepare a sleep mode.
	*/
	event error_t Titan.stop(TitanTaskConfig* pConfig)
	{
		return SUCCESS;
	}
	
	////////////////////////////////////////////////////////////////////////////
	// This is the actual working thread	
	event void Titan.execute( TitanTaskConfig* pConfig, uint8_t uiPortID )
	{
        // get context stuff
		SumTaskData *pData;
		TitanPacket *pPacketIn;
		
		int16_t *pBuffer;
		int16_t iSum; //int32_t iSum;
		uint16_t uiWindowSize;
		uint16_t uiWindowShift;
		uint16_t uiDataCounter;
		int i, length, lengthOut;

		if ( pConfig == NULL ) {
			dbg("TitanTask","WARNING: Got no context in task Sum\n");
			return;
		}
		
		pData = (SumTaskData*)pConfig->pTaskData;
		
		// get in/out packets
		pPacketIn = call Titan.getNextPacket(pConfig->taskID, 0);

		
		uiWindowSize = pData->uiWindowSize;
		uiWindowShift = pData->uiWindowShift;
		uiDataCounter = pData->uiDataCounter;

		dbg("TitanTask", "Task Sum: Running\n" );

        if (pPacketIn == NULL)
		{
          call Titan.issueError(ERROR_PACKET);
          return;
        }

		/* TODO: handle different formats (AB: problem is that type is not correctly transmitted from task to task) */
		/*
		if (pPacketIn->type == TT_INT16 || pPacketIn->type == TT_UINT16)
		{
			int16_t *pSource = (int16_t*)pPacketIn->data;
			pBuffer = pData->pCurEntry;
			iSum = pData->iSum;
			length = pPacketIn->length>>1;
			lengthOut = 2;

			dbg( "TitanTask", "Task Sum: 2 bytes input, starting for %i entries...\n", length);
			
			for (i=0; i < length; i++)
			{
				iSum += *pSource - *pBuffer;
				*pBuffer = *pSource;
          
				if (uiDataCounter >= uiWindowShift)
				{
					TitanPacket *pPacketOut;
					pPacketOut = call Titan.allocPacket( pConfig->taskID, 0 );

					// queue full?
					if ( pPacketOut == NULL )
					{
						call Titan.issueError(ERROR_PACKET);
					}
					else
					{
						*((int16_t*)pPacketOut->data) = iSum;

						dbg( "TitanTask", "Task Sum: sending packet with sum = %i\n", iSum);

						pPacketOut->length = lengthOut;
						pPacketOut->type = TT_INT16;
						call Titan.sendPacket( pConfig->taskID, 0, pPacketOut );
					}

					uiDataCounter = 0;
				}
				
				uiDataCounter++;
      
				pSource++;
				pBuffer++;
          
				// check wraparound
				if (pBuffer-(int16_t*)(pData+1) >= uiWindowSize)
				{
				pBuffer = (int16_t*)(pData+1);
				}
			}
		}
		*/
		//else if (pPacketIn->type == TT_INT8 || pPacketIn->type == TT_UINT8)
		{
			int8_t *pSource = (int8_t*)pPacketIn->data;
			pBuffer = pData->pCurEntry;
			iSum = pData->iSum;
			length = pPacketIn->length;
			lengthOut = 2;
			
			dbg( "TitanTask", "Task Sum: 1 byte input, starting for %i entries...\n", length);
			
			for (i=0; i < length; i++)
			{
				int16_t source = (*pSource) & 0x00FF;
				iSum += source - *pBuffer;
				*pBuffer = source;
          
				if (uiDataCounter >= uiWindowShift)
				{
					TitanPacket *pPacketOut;
					pPacketOut = call Titan.allocPacket( pConfig->taskID, 0 );

					// queue full?
					if ( pPacketOut == NULL )
					{
						call Titan.issueError(ERROR_PACKET);
					}
					else
					{
						*((int16_t*)pPacketOut->data) = iSum;

						dbg( "TitanTask", "Task Sum: sending packet with sum = %i\n", iSum);

						pPacketOut->length = lengthOut;
						pPacketOut->type = TT_INT16;
						call Titan.sendPacket( pConfig->taskID, 0, pPacketOut );
					}

					uiDataCounter = 0;
				}
				
				uiDataCounter++;
      
				pSource++;
				pBuffer++;
          
				// check wraparound
				if (pBuffer-(int16_t*)(pData+1) >= uiWindowSize)
				{
					pBuffer = (int16_t*)(pData+1);
				}
			}
		}
		/*
		else
		{
			dbg( "TitanTask", "Sum: unknown data type\n" );
			call Titan.issueError(ERROR_TYPE);
			return;
		}
		*/
		
		pData->iSum     = iSum;
		pData->pCurEntry = pBuffer;
    
		pData->uiDataCounter = uiDataCounter;
		
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
