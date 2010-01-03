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
  * TitanTaskAverage.nc
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  *
  * Moving average. Issues one output packet per input packet. This performs 
  * the function y[k] = (a-1)*y[k-1] + a*x[k]
  * 
  * Configuration (1 Byte):
  * [ uiFraction ]
  * 
  * a = (1/2)^uiFraction
  *
  */

module TitanTaskAverage {
	uses interface Titan;
}

implementation {

	typedef struct AverageTaskData {
		int32_t iAverage; // fixed point at 24 bit, 8 fraction bits
		uint8_t uiFrac;
	} AverageTaskData;

    event error_t Titan.init() {
      return SUCCESS;
    }

	event TitanTaskUID Titan.getTaskUID() {
		return TITAN_TASK_AVERAGE;
	}

	event error_t Titan.configure( TitanTaskConfig* pConfig ) {
        AverageTaskData* pData;

		dbg( "TitanTask", "New task with ID: %i: Average\n", pConfig->taskID );

		pConfig->inPorts  = 1;
		pConfig->outPorts = 0;
		
		pConfig->pTaskData = call Titan.allocMemory(pConfig->taskID,sizeof(AverageTaskData));
		if ( pConfig->pTaskData == NULL ) {
			call Titan.issueError(ERROR_NO_MEMORY);
		}
		pData = (AverageTaskData*)pConfig->pTaskData;
		
		if ( pConfig->configLength == 0 ) {
		  pData->uiFrac = 10;
		} else if ( pConfig->configLength == 1 ) {
		  pData->uiFrac = pConfig->configData[0];
		} else {
		  dbg( "TitanTask", "Task Average: Unknown configuration size\n");
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
		AverageTaskData* pData = (AverageTaskData*)pConfig->pTaskData;
		dbg( "TitanTask", "Starting TitanTaskAverage.\n" );
		
		pData->iAverage = 0;
		
		return SUCCESS;
	}
	
	event error_t Titan.stop( TitanTaskConfig* pConfig ) {
		return SUCCESS;
	}
	
	event void Titan.execute( TitanTaskConfig* pConfig, uint8_t uiPort ) {
	  
		AverageTaskData *pData  = (AverageTaskData*)pConfig->pTaskData;
		TitanPacket* pPacketIn, *pPacketOut;
		int i;
		
		if ( pConfig == NULL )  {
			call Titan.issueError(0);
			return;
		}
		
		pPacketIn  = call Titan.getNextPacket( pConfig->taskID, 0 );
		pPacketOut = call Titan.allocPacket( pConfig->taskID, 0 );
		
		if ( pPacketIn == NULL || pPacketOut == NULL ) {
			call Titan.issueError(1);
			return;
		}
		
#ifdef MEASURE_CYCLES
        P6OUT |=  BIT1;  // set on
#endif

		if ( pPacketIn->type == TT_INT16 ) {
		    int16_t* pDataIn = (int16_t*)pPacketIn->data;
		    int16_t* pDataOut= (int16_t*)pPacketOut->data;
		  
			for ( i=0; i < (pPacketIn->length>>1); i++ ) {
              int32_t iResult;
              // do some fixpoint shifting to get some more accuracy

              // iResult = 1/a*(1/a-1)*y[k-1]
              iResult = pData->iAverage - (pData->iAverage>>pData->uiFrac);
              dbg( "TitanTask", "iResult0: %3i\n", iResult );
              
              // iResult += x[k]
              iResult += ((*pDataIn)<<8);
              dbg( "TitanTask", "iResult1: %3i\n", iResult );

              pData->iAverage = iResult;

              // y[k] = iResult * a = (1-a)*y[k-1] + a*x[k]
              *pDataOut = ((iResult>>pData->uiFrac)>>8);
              dbg( "TitanTask", "Moving average: %3i\n", *pDataOut );
              pDataOut++; pDataIn++;
	        }
	        
	        pPacketOut->type = TT_INT16;
	        pPacketOut->length = pPacketIn->length;
	        
		} else {
		  dbg( "TitanTask", "Moving Average: unknown data type\n" );
		  return;
		}
		
#ifdef MEASURE_CYCLES
        P6OUT &= ~BIT1;  // set off
#endif
		call Titan.sendPacket( pConfig->taskID, 0, pPacketOut );

        // TinyOS 2.0 scheduler drops out
        if ( call Titan.hasPacket( pConfig->taskID, 0 ) > 0 ) {
          call Titan.postExecution(pConfig,uiPort);
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
