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
  * TitanTaskZeroCross.nc
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  * @author Andreas Breitenmoser
  *
  * Counts the number of zero crossings inside a sliding window. The sliding window is continuously shifted with uiWindowShift.
  * Zero crossings are incremented each time the input signal crosses the band [mean+uiThreshUp, mean-uiThreshLow] around the average of the current window.
  * All input is assumed to be uint16_t. Outputs one packet per window. 
  * 
  * Configuration:
  * [uiWindowSize, uiWindowShift, uiThreshLow, uiThreshUp]
  * 
  * uiWindowSize specifies over how many data items the sliding window goes: length of sliding window = uiWindowSize (max. 256).
  * uiWindowShift sets the number of data items the sliding window is shifted before the next number of zero crossings is calculated.  
  * 
  * Last modified: 27.02.08
  */
  
module TitanTaskZeroCross {
	uses interface Titan;
}
implementation {

	typedef struct ZeroCrossTaskData {
	  uint8_t zcState; // 1 == high, 0 == low, 2 == in midband
	  uint16_t iSum; //int32_t iSum;
	  uint8_t uiWindowSize;
	  uint8_t uiWindowShift;
	  uint16_t uiThreshLow;	  
	  uint16_t uiThreshUp;
	  uint8_t uiDataCounter;
	  uint16_t uiCrossings;
	  uint16_t *pCurEntry;
	} ZeroCrossTaskData;
	
	
    event error_t Titan.init() {
      return SUCCESS;
    }

	
	event TitanTaskUID Titan.getTaskUID() {
		return TITAN_TASK_ZEROCROSS;
	}

	
	event error_t Titan.configure( TitanTaskConfig* pConfig ) {
        ZeroCrossTaskData* pData;
		
		uint8_t uiWindowSize, uiWindowShift, i;
		uint16_t uiThreshLow, uiThreshUp;
        uint16_t *pFilterData;

		dbg( "TitanTask", "New task with ID: %i: ZeroCross\n", pConfig->taskID );

		// define number of i/o-ports 
		pConfig->inPorts  = 1;
		pConfig->outPorts = 1;
		
		////////////////////////////////////////////////////////////////////////
		// Application dependent code
		
		if (pConfig->configLength != 6) {
			dbg( "TitanTask", "Task ZeroCross: Unknown configuration size\n");
			call Titan.issueError(ERROR_CONFIG);
			return FALSE;
		}
		
		uiWindowSize = pConfig->configData[0];
		uiWindowShift = pConfig->configData[1];
		
		uiThreshLow = ((uint16_t)pConfig->configData[2]<<8) + ((uint16_t)pConfig->configData[3]);
		uiThreshUp = ((uint16_t)pConfig->configData[4]<<8) + ((uint16_t)pConfig->configData[5]);
		
		
		// allocate memory for the data structure
		pConfig->pTaskData = call Titan.allocMemory(pConfig->taskID,uiWindowSize*sizeof(uint16_t)+sizeof(ZeroCrossTaskData));
		if ( pConfig->pTaskData == NULL ) {
			call Titan.issueError(ERROR_NO_MEMORY);
			return FALSE;
		}
		
		pData = (ZeroCrossTaskData*)pConfig->pTaskData;
		
		pData->iSum = 0;
		pData->uiWindowSize = uiWindowSize;
		pData->uiWindowShift = uiWindowShift;
		pData->uiThreshUp = uiThreshUp;
		pData->uiThreshLow = uiThreshLow;
		pData->uiDataCounter = 1;
		pData->uiCrossings = 0;
		pData->zcState = 2;
		
		pFilterData = pData->pCurEntry = (uint16_t*)(pData+1);
		
		// init data
        for (i=0;i<uiWindowSize;i++) {
			*(pFilterData++) = 0;
		}

		dbg( "TitanTask", "Task ZeroCross: Configured with window size = %i, shift = %i, ThreshLow = %i, ThreshUp = %i\n",uiWindowSize,uiWindowShift,uiThreshLow,uiThreshUp);
		
		return SUCCESS;
	}
	
	
	event error_t Titan.terminate( TitanTaskConfig* pConfig ) {
		error_t result = call Titan.freeMemory(pConfig->taskID, pConfig->pTaskData);
		return result;
	}
	
	event error_t Titan.start( TitanTaskConfig* pConfig ) {
		dbg( "TitanTask", "Starting TitanTaskZeroCross.\n" );
		
		return SUCCESS;
	}
	
	event error_t Titan.stop( TitanTaskConfig* pConfig ) {
		return SUCCESS;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	// This is the actual working thread	
	event void Titan.execute( TitanTaskConfig* pConfig, uint8_t uiPortID ) {
	  
		ZeroCrossTaskData *pData;
		TitanPacket* pPacketIn;
		int i, lengthIn, lengthOut;
		int32_t maxThreshUp;
		uint16_t threshUp, threshLow;
		uint16_t iMean;
		
		uint16_t *pSource, *pBuffer, *pStart;
		
		uint16_t iSum; //int32_t iSum;
		uint8_t zcState; 
		uint16_t uiWindowSize;
		uint16_t uiWindowShift;
		uint16_t uiThreshUp;
		uint16_t uiThreshLow;
		uint16_t uiDataCounter;
		uint8_t  uiCrossings;
		
		if ( pConfig == NULL ) {
			dbg("TitanTask","WARNING: Got no context in task ZeroCross\n");
			return;
		}

		pData  = (ZeroCrossTaskData*)pConfig->pTaskData;
		
		
		
		if ( pConfig == NULL )  {
			call Titan.issueError(ERROR_PACKET);
			return;
		}
		
		// get in/out packets
		pPacketIn = call Titan.getNextPacket( pConfig->taskID, 0 );
		
		if ( pPacketIn == NULL ) {
			call Titan.issueError(ERROR_PACKET);
			return;
		}

		uiWindowSize = pData->uiWindowSize;
		uiWindowShift = pData->uiWindowShift;
		uiThreshUp = pData->uiThreshUp;
		uiThreshLow = pData->uiThreshLow;
		uiDataCounter = pData->uiDataCounter;
		uiCrossings = pData->uiCrossings;
		
		zcState = pData->zcState;
		iSum = pData->iSum;
		
		lengthIn  = pPacketIn->length>>1;
		lengthOut = 2;
		
		pSource = (uint16_t*)pPacketIn->data;
		pBuffer = pData->pCurEntry;
		pStart = pBuffer + 1;
		// check wraparound
		if ( pStart-(uint16_t*)(pData+1) >= uiWindowSize ) {
			pStart = (uint16_t*)(pData+1);
		}
		
		
		dbg( "TitanTask", "Task ZeroCross: Running\n" );
	
	
        for ( i=0; i < lengthIn; i++ ) {
	        iSum += *pSource - *pBuffer;
	        *pBuffer = *pSource;
			
			if (uiDataCounter >= uiWindowShift) {
				TitanPacket *pPacketOut;
				
				/* calculate mean */
				iMean = iSum / uiWindowSize;
				
				dbg( "TitanTask", "ZeroCross: current mean = %3i\n", iMean );
				
				// consider saturations
				if (uiThreshLow >= iMean)
					threshLow = 0;
				else
					threshLow = iMean - uiThreshLow;
					
				maxThreshUp = iMean+uiThreshUp;
				if (maxThreshUp < iMean) // check for overflow
					threshUp = TT_UINT16_MAX;
				else
					threshUp = iMean + uiThreshUp;
				
				/* calculate zero crossings */
				if (*pStart > threshUp)
					zcState = 1;
				else if (*pStart < threshLow)
					zcState = 0;
				else
					zcState = 2;
					
				pStart++;
				// check wraparound
				if ( pStart-(uint16_t*)(pData+1) >= uiWindowSize ) {
					pStart = (uint16_t*)(pData+1);
				}
					
				for ( i=0; i < (uiWindowSize-1); i++ ) {
									
					if (zcState == 2) 
					{
						if (*pStart > threshUp)
							zcState = 1;
						else if (*pStart < threshLow)
							zcState = 0;
					}				
					else if ( zcState == 1 && (*pStart < threshLow) )
					{
						zcState = 0;
						uiCrossings++;
					}
					else if ( zcState == 0 && (*pStart > threshUp ) )
					{
						zcState = 1;
						uiCrossings++;
					}
					
					pStart++;
					// check wraparound
					if ( pStart-(uint16_t*)(pData+1) >= uiWindowSize ) {
						pStart = (uint16_t*)(pData+1);
					}
				}
					
				pPacketOut = call Titan.allocPacket( pConfig->taskID, 0 );

				// queue full?
				if ( pPacketOut == NULL ) {
					call Titan.issueError(ERROR_PACKET);
				} else {
					*((uint16_t*)pPacketOut->data) = uiCrossings;
					
					dbg( "TitanTask", "ZeroCross: number of zc = %3i\n", *((uint16_t*)pPacketOut->data) );
	          
					pPacketOut->length = lengthOut;
					pPacketOut->type = TT_UINT16;
					call Titan.sendPacket( pConfig->taskID, 0, pPacketOut );
				}
				
				uiCrossings = 0;
				uiDataCounter = 0;
				zcState = 2;
			} else {
//        P2OUT &= ~BIT0; //off
      }
      
			uiDataCounter++;
      
			pSource++;
			pBuffer++;
			pStart++;
          
			// check wraparound
			if ( pStart-(uint16_t*)(pData+1) >= uiWindowSize ) {
				pStart = (uint16_t*)(pData+1);
			}
			else if ( pBuffer-(uint16_t*)(pData+1) >= uiWindowSize ) {
				pBuffer = (uint16_t*)(pData+1);
			}
        }

		pData->iSum = iSum;
        pData->pCurEntry = pBuffer;
		
		pData->uiDataCounter = uiDataCounter;
		pData->zcState = zcState;
		pData->uiCrossings = uiCrossings;

        // TinyOS 2.0 scheduler drops out
        if ( call Titan.hasPacket( pConfig->taskID, uiPortID ) > 0 ) {
          call Titan.postExecution(pConfig,uiPortID);
        }
	}

	async event error_t Titan.packetAvailable( TitanTaskConfig* pConfig, uint8_t uiPort ) {
		if (call Titan.postExecution(pConfig,uiPort) == FAIL ) {
		  call Titan.issueError(ERROR_PACKET);
		  return FAIL;
		}
		return SUCCESS;
	}
	
}
