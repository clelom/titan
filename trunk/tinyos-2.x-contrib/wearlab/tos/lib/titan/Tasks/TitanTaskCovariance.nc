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
  * TitanTaskCovariance.nc
  *
  * @author Andreas Breitenmoser
  *
  * Computes the covariance over a defined length of data.
  * Always follows on a merge task. The inputs must be equal in size.
  * All input is assumed to be uint16_t.
  * 
  * Configuration is two bytes:
  * [uiWindowSize, uiWindowShift]
  *
  * uiWindowSize specifies over how many data items the sliding window goes: length of sliding window = 2^uiWindowSize.
  * uiWindowShift sets the number of data items the sliding window is shifted before the next variance value is calculated.  
  * 
  * Created: 30.01.08
  * Last modified: 30.01.08
  */

module TitanTaskCovariance
{
	uses interface Titan;
}
implementation
{
	#define ESCAPE_VALUE 0xAA

	// the data used by the task should always be 
	// organized in a structure.
	typedef struct CovarianceTaskData {
	  int32_t iSum1;
	  int32_t iSum2;
	  int32_t iMultSum;
	  uint8_t uiWindowSize;
	  uint8_t uiWindowShift;
	  uint8_t uiDataCounter;
	  int16_t *pCurEntry1;
	  int16_t *pCurEntry2;
	} CovarianceTaskData;

	
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
		return TITAN_TASK_COVARIANCE;
	}

	
	/**
	* Initializes a component and passes a configuration structure. In the pInOut 
	* structure, the task returns its configuration, ie. how many input and output 
	* ports it reserves according to the configuration received.
	* @param pConfig Pointer to the configuration structure for the task
	* @return Whether the task has successfully been initialized
	*/
	event error_t Titan.configure(TitanTaskConfig* pConfig)
	{
		CovarianceTaskData* pData;
		uint8_t uiWindowSize, uiWindowShift, i;
		uint16_t *pFilterData1;
		uint16_t *pFilterData2;

		// define number of i/o-ports		
		pConfig->inPorts  = 1;
		pConfig->outPorts = 1;
		
		////////////////////////////////////////////////////////////////////////
		// Application dependent code
		
		if (pConfig->configLength != 2)
		{
		  dbg( "TitanTask", "Task Variance: Unknown configuration size\n");
  		  call Titan.issueError(ERROR_CONFIG);
		  return FALSE;
		}
		
		uiWindowSize = pConfig->configData[0];
		uiWindowShift = pConfig->configData[1];
		
		// allocate memory for the data structure
		pConfig->pTaskData = call Titan.allocMemory(pConfig->taskID, 2*(1<<uiWindowSize)*sizeof(uint16_t)+sizeof(CovarianceTaskData));
		if ( pConfig->pTaskData == NULL )
		{
			call Titan.issueError(ERROR_NO_MEMORY);
		}
		pData = (CovarianceTaskData*)pConfig->pTaskData;

        pData->iSum1 = 0;
		pData->iSum2 = 0;
        pData->iMultSum = 0;
		pData->uiWindowSize = uiWindowSize;
		pData->uiWindowShift = uiWindowShift;
		//pData->uiDataCounter = 1;
		
		pFilterData1 = pData->pCurEntry1 = (uint16_t*)(pData+1);
		pFilterData2 = pData->pCurEntry2 = pFilterData1 + (1<<uiWindowSize);
		
		// init data
		for (i=0;i<(1<<uiWindowSize);i++)
		{
		  *(pFilterData1++) = 0;
		  *(pFilterData2++) = 0;
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
		
		return result;
	}
	
	
	/**
	* Starts the operation of a task. This could also wake it up again.
	*/
	event error_t Titan.start( TitanTaskConfig* pConfig )
	{
		return SUCCESS;
	}
	
	
	/**
	* Stops the operation of a task. It should then prepare a sleep mode.
	*/
	event error_t Titan.stop( TitanTaskConfig* pConfig )
	{
		return SUCCESS;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	// This is the actual working thread	
	event void Titan.execute( TitanTaskConfig* pConfig, uint8_t uiPortID )
	{
        // get context stuff
		CovarianceTaskData *pData;
		TitanPacket *pPacketIn;
		
		uint8_t *pPInData;
		
		int16_t *pSource1, *pSource2, *pBuffer1, *pBuffer2;
		int32_t iSum1, iSum2;
		int32_t iMultSum;
		uint16_t uiWindowSize;
		uint16_t uiWindowShift;
		uint16_t uiDataCounter;
		int i, j, lengthIn, lengthOut;
		
		if ( pConfig == NULL ) {
			dbg("TitanTask","WARNING: Got no context in task Covariance\n");
			return;
		}
		
		pData  = (CovarianceTaskData*)pConfig->pTaskData;
		
		// get in/out packets
		pPacketIn  = call Titan.getNextPacket(pConfig->taskID, 0);

		pPInData = pPacketIn->data;
		
		uiWindowSize = pData->uiWindowSize;
		uiWindowShift = pData->uiWindowShift;
		uiDataCounter = pData->uiDataCounter;

		if ( pPacketIn == NULL ) {
		  call Titan.issueError(ERROR_PACKET);
		  return;
		}
		
		// data acquisition
		/* following code holds for two input sources only (input: merge(2)) */
		
		// first data item should be an ESCAPE_VALUE
		if (*pPInData == ESCAPE_VALUE)
		{
				pPInData++;
				pSource1 = (int16_t*)pPInData++;
		}
		else
		{
			call Titan.issueError(ERROR_PACKET);
			return;
        }
		
		pSource2 = pSource1; // in this case calculate variance
		// get the data from the second source
		for (j = 0; j < pPacketIn->length; j++)
		{
			if (pPInData[j] == ESCAPE_VALUE)
			{
				pPInData++;
				pSource2 = (int16_t*)pPInData;
			}
		}
		
		// computation of covariance
		pBuffer1 = pData->pCurEntry1;
		pBuffer2 = pData->pCurEntry2;
		iSum1 = pData->iSum1;
		iSum2 = pData->iSum2;
		iMultSum = pData->iMultSum;
		lengthIn  = (pPacketIn->length)>>1;
		lengthOut = 2;

        for (i=0; i < lengthIn; i++)
		{
          int32_t uiMult = (*pSource1)*(*pSource2);
          iMultSum += uiMult - (*pBuffer1)*(*pBuffer2);
          iSum1 += *pSource1 - *pBuffer1;
          iSum2 += *pSource2 - *pBuffer2;
          dbg("TitanTask", "buffer1: %i buffer2: %i mult: %i   ", *pBuffer1, *pBuffer2, uiMult);
          *pBuffer1 = *pSource1;
          *pBuffer2 = *pSource2;
		  
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
				*((uint16_t*)pPacketOut->data) = (iMultSum - ((iSum1 * iSum2)>>uiWindowSize))>>uiWindowSize;
				dbg("TitanTask", "Covariance: +%3i +%3i => s1=%4i s2=%4i ms=%4i => %i\n",
									*pSource1, *pSource2, iSum1, iSum2, iMultSum, *((uint16_t*)pPacketOut->data));
				pPacketOut->length = lengthOut;
				pPacketOut->type = TT_UINT16;
				call Titan.sendPacket( pConfig->taskID, 0, pPacketOut );
			}
			
			uiDataCounter = 0;
		  }
		  
		  uiDataCounter++;
		  
          pSource1++;
          pSource2++;
          pBuffer1++;
          pBuffer2++;
          
          // check wraparound
          if (pBuffer1-(int16_t*)(pData+1) >= (1<<uiWindowSize))
		  {
            pBuffer1 = (int16_t*)(pData+1);
            pBuffer2 = pBuffer1 + (1<<uiWindowSize);
          }
        }
        
        pData->iSum1 = iSum1;
        pData->iSum2 = iSum2;
        pData->iMultSum = iMultSum;
        pData->pCurEntry1 = pBuffer1;
        pData->pCurEntry2 = pBuffer2;
		
		pData->uiDataCounter = uiDataCounter;
		
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
	async event error_t Titan.packetAvailable(TitanTaskConfig* pConfig, uint8_t uiPort)
	{
		// start the working task
		if (call Titan.postExecution(pConfig,uiPort) == FAIL ) {
		  call Titan.issueError(ERROR_PACKET);
		  return FAIL;
		}

		return SUCCESS;
	}
}
