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
  * TitanTaskMax.nc
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  *
  * Finds the maximum in a window of given size.
  * All input is assumed to be uint16_t.
  * 
  * Configuration:
  * [ uiWindowSizeH, uiWindowSizeL ]
  * 
  */
  
module TitanTaskMax {
	uses interface Titan;
}

implementation {

	typedef struct MaxTaskData {
	  uint16_t uiWindow;
	  uint16_t uiWindowShift;
	  uint16_t iMax; // current maximum value
	  uint16_t uiCurEntry; // points to current entry in the buffer
	  uint16_t uiCounter; // points to current position with respect to shift
	} MaxTaskData;

    event error_t Titan.init() {
      return SUCCESS;
    }

	event TitanTaskUID Titan.getTaskUID() {
		return TITAN_TASK_MAX;
	}

	event error_t Titan.configure( TitanTaskConfig* pConfig ) {
        MaxTaskData* pData;
		uint16_t uiWindow,i;
		uint16_t* pFilterData;

		dbg( "TitanTask", "New task with ID: %i: Max\n", pConfig->taskID );

		pConfig->inPorts  = 1;
		pConfig->outPorts = 1;

		if (pConfig->configLength != 2 ) {
		  dbg( "TitanTask", "Task Max: Unknown configuration size\n");
  		  call Titan.issueError(ERROR_CONFIG);
		  return FALSE;
		}
		
		uiWindow = pConfig->configData[0];
		
		pConfig->pTaskData = call Titan.allocMemory(pConfig->taskID,sizeof(MaxTaskData)+uiWindow*sizeof(uint16_t));
		if ( pConfig->pTaskData == NULL ) {
			call Titan.issueError(ERROR_NO_MEMORY);
		}
		pData = (MaxTaskData*)pConfig->pTaskData;
		
		pData->uiWindow = uiWindow;
		pData->uiWindowShift = pConfig->configData[1];
		pData->uiCurEntry = 0;
		pData->uiCounter = 0;
		pData->iMax = 0;
		
		pFilterData = (uint16_t*)(pData+1);
		for (i=0;i<uiWindow;i++) {
			*(pFilterData++) = 0;
		}

		return SUCCESS;
	}
	
	
	event error_t Titan.terminate( TitanTaskConfig* pConfig ) {
		error_t result = call Titan.freeMemory(pConfig->taskID, pConfig->pTaskData);
		return result;
	}
	
	event error_t Titan.start( TitanTaskConfig* pConfig ) {
		dbg( "TitanTask", "Starting TitanTaskMax.\n" );
		
		return SUCCESS;
	}
	
	event error_t Titan.stop( TitanTaskConfig* pConfig ) {
		return SUCCESS;
	}
	
	event void Titan.execute( TitanTaskConfig* pConfig, uint8_t uiPortID ) {
	  
		MaxTaskData *pData;
		TitanPacket* pPacketIn;
		int i;
		
		if ( pConfig == NULL ) {
			dbg("TitanTask","WARNING: Got no context in task Max\n");
			return;
		}
		
		pData  = (MaxTaskData*)pConfig->pTaskData;
		
		if ( pConfig == NULL )  {
			call Titan.issueError(ERROR_CONFIG);
			return;
		}
		
   		pPacketIn = call Titan.getNextPacket( pConfig->taskID, 0 );
   		
		if ( pPacketIn == NULL ) {
			call Titan.issueError(ERROR_PACKET);
			return;
		}

	    if ( pPacketIn->type == TT_UINT16 )
        {
			uint16_t uiMax = pData->iMax;
			uint16_t *pNew = (uint16_t*)pPacketIn->data;
			uint16_t bSearchMax = 0;
			uint16_t* pBuffer = (uint16_t*)(pData+1);
			
			// go through all data in the buffer
			for (i=0; i < pPacketIn->length>>1; i++ ) {
				uint16_t buf = pBuffer[pData->uiCurEntry];
				pBuffer[pData->uiCurEntry] = *pNew;
				
				// check whether we have a maximum value
				if ( *pNew > uiMax ) {
					uiMax = *pNew;
					bSearchMax = 0;
				} else if ( uiMax == buf ) { // maximum drops out
					bSearchMax = 1;
				}

				// increment counters
                pData->uiCurEntry = (pData->uiCurEntry+1)%pData->uiWindow;
				pData->uiCounter++;
				
				// check whether a window shift has been completed
				if (pData->uiCounter >= pData->uiWindowShift) {
					TitanPacket* pPacketOut = NULL;
					
					// if maximum is unknown, find it again
					if ( bSearchMax != 0 ) {
						uint16_t j,*pCur = pBuffer;
						uiMax = 0;
						for (j=0;j<pData->uiWindow;j++) {
							if (uiMax < *pCur ) uiMax = *pCur;
							pCur++;
						}
						bSearchMax = 0;
					}
					
					// put together packet
		            pPacketOut = call Titan.allocPacket( pConfig->taskID, 0 );
		            pPacketOut->type = TT_UINT16;
		            pPacketOut->length = 2;
		            *((uint16_t*)&pPacketOut->data[0]) = uiMax;
		            call Titan.sendPacket(pConfig->taskID, 0, pPacketOut);
					
					
					pData->uiCounter = 0;
				}
			} // for data length
			
			// if maximum is unknown, find it again
			if ( bSearchMax != 0 ) {
				uint16_t j,*pCur = pBuffer;
				uiMax = 0;
				for (j=0;j<pData->uiWindow;j++) {
					if (uiMax < *pCur ) uiMax = *pCur;
					pCur++;
				}
				bSearchMax = 0;
			}
			pData->iMax = uiMax;
		}
	    else {
	       dbg( "TitanTask", "Max: Invalid data type (%i) in packet!\n", pPacketIn->type );
	       call Titan.issueError(ERROR_TYPE);
	       return;
	    }

		// TinyOS 2.0 scheduler drops out
        if ( call Titan.hasPacket( pConfig->taskID, 0 ) > 0 ) {
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
