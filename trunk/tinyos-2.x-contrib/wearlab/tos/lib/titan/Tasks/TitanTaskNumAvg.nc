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
  * TitanTaskNumAvg.nc
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  *
  * Counts the number of zero crossings per window size. Outputs one packet per 
  * window size.
  * 
  * Configuration:
  * [ uiWindowSizeH, uiWindowSizeL ]
  * 
  */
  
module TitanTaskNumAvg {
	uses interface Titan;
}

implementation {

	typedef struct NumAvgTaskData {
	  uint32_t uiSum;
	  uint8_t uiIndex;
	  uint8_t uiWindow;
	} NumAvgTaskData;


    event error_t Titan.init() {
      return SUCCESS;
    }

	event TitanTaskUID Titan.getTaskUID() {
		return TITAN_TASK_NUMAVG;
	}

	event error_t Titan.configure( TitanTaskConfig* pConfig ) {
        NumAvgTaskData* pData;

		dbg( "TitanTask", "New task with ID: %i: NumAvg\n", pConfig->taskID );

		pConfig->inPorts  = 1;
		pConfig->outPorts = 1;
		
		pConfig->pTaskData = call Titan.allocMemory(pConfig->taskID,sizeof(NumAvgTaskData));

		if ( pConfig->pTaskData == NULL ) {
			call Titan.issueError(ERROR_NO_MEMORY);
		}
		pData = (NumAvgTaskData*)pConfig->pTaskData;
		
		if ( pConfig->configLength == 1 ) {
		  pData->uiSum = 0;
		  pData->uiWindow = pConfig->configData[0];
		  pData->uiIndex = 0;
		  dbg( "TitanTask", "Task NumAvg: window is %i %i\n",pConfig->configData[0],pConfig->configData[1]);
		} else {
		  dbg( "TitanTask", "Task NumAvg: Unknown configuration size\n");
  		  call Titan.issueError(ERROR_CONFIG);
		  return FALSE;
		}
		
		return SUCCESS;
	}
	
	
	event error_t Titan.terminate( TitanTaskConfig* pConfig ) {
		error_t result = call Titan.freeMemory(pConfig->taskID, pConfig->pTaskData);
		return result;
	}
	
	event error_t Titan.start( TitanTaskConfig* pConfig ) {
		dbg( "TitanTask", "Starting TitanTaskNumAvg.\n" );
		
		return SUCCESS;
	}
	
	event error_t Titan.stop( TitanTaskConfig* pConfig ) {
		return SUCCESS;
	}
	
	event void Titan.execute( TitanTaskConfig* pConfig, uint8_t uiPortID ) {
	  
		NumAvgTaskData *pData;
		
		TitanPacket* pPacketIn;
		int i;
		
		if ( pConfig == NULL ) {
			dbg("TitanTask","WARNING: Got no context in task NumAvg\n");
			return;
		}
		
		pData  = (NumAvgTaskData*)pConfig->pTaskData;
		
		
#ifdef MEASURE_CONFIG
  P6OUT |= BIT1;
#endif
		if ( pConfig == NULL )  {
			call Titan.issueError(ERROR_CONFIG);
#ifdef MEASURE_CONFIG
  P6OUT &= ~BIT1;
#endif
			while(1);
			return;
		}
		
		pPacketIn = call Titan.getNextPacket( pConfig->taskID, 0 );
		
		if ( pPacketIn == NULL ) {
			call Titan.issueError(ERROR_PACKET);
			return;
		}

        dbg( "TitanTask", "Task NumAvg: Running\n" );
		
#ifdef MEASURE_CYCLES
        P6OUT |=  BIT1;  // set on
#endif

      if ( pPacketIn->type == TT_INT16 ) {
          uint8_t uiWindow = pData->uiWindow;
          uint8_t uiIndex  = pData->uiIndex;
          int32_t uiSum    = pData->uiSum;
          int16_t  *pDataIn = (int16_t*)&pPacketIn->data[0];
          
			for ( i=0; i < pPacketIn->length>>1; i++ ) {
			  
			  uiSum += *pDataIn;
			  
	          pDataIn++;
	          
	          // increment counter
	          uiIndex++;
	          
	          // if the window is complete, issue a packet and restart the window
	          if ( uiIndex >= uiWindow ) {
	            TitanPacket* pPacketOut = call Titan.allocPacket( pConfig->taskID, 0 );
	            pPacketOut->type = TT_INT16;
	            pPacketOut->length = 2;
	            *((uint16_t*)&pPacketOut->data[0]) = (int16_t)(uiSum/uiWindow);
#ifdef MEASURE_CYCLES
        P6OUT &= ~BIT1;  // set off
#endif
//                dbg( "TitanTask", "Sending packet (%i)\n", uiWindow );
	            call Titan.sendPacket(pConfig->taskID, 0, pPacketOut);
                uiSum = 0;
	            uiIndex = 0;
	          }
			} // for the whole packet

            pData->uiSum   = uiSum;
			pData->uiIndex = uiIndex;
			
	    } else {
	       dbg( "TitanTask", "Task NumAvg: Invalid data type in packet!\n" );
	       call Titan.issueError(ERROR_TYPE);
	       return;
	    }

#ifdef MEASURE_CONFIG
  P6OUT &= ~BIT1;
#endif
        
		// TinyOS 2.0 scheduler drops out
        if ( call Titan.hasPacket( pConfig->taskID, 0 ) > 0 ) {
          call Titan.postExecution( pConfig, uiPortID );
        }
		
	}

	async event error_t Titan.packetAvailable( TitanTaskConfig* pConfig, uint8_t uiPort ) {
		if (call Titan.postExecution( pConfig, uiPort ) == FAIL ) {
		  call Titan.issueError(6);
		  return FAIL;
		}
		return SUCCESS;
	}
	
	

}
