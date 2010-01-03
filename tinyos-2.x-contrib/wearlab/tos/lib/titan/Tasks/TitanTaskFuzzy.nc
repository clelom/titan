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
  * TitanTaskFuzzy.nc
  *
  * @author Andreas Breitenmoser
  *
  * Makes decisions in function of the input values. The precise decision rule must be adjusted for the specific application.
  * As input 8 bit data is expected, 16 bit value is output. 
  * 
  * Configuration:
  *   [uiSelApp]
  * 
  * uiSelApp is 1 byte, it defines which application is selected, 
  *
  * Created: 20.02.2008
  * Last modified: 20.02.2008
  *
  */

module TitanTaskFuzzy
{
	uses interface Titan;
}

implementation
{
	// the data used by the task should always be 
	// organized in a structure.
	typedef struct FuzzyTaskData {
		uint8_t uiSelApp;
	} FuzzyTaskData;

  
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
		return TITAN_TASK_FUZZY;
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
		FuzzyTaskData* pData;
		
        if (pConfig->configLength != 1)
		{
		  dbg( "TitanTask", "Task Fuzzy: Unknown configuration size.\n");
  		  call Titan.issueError(ERROR_CONFIG);
		  return FALSE;
		}

		// define number of i/o-ports		
		pConfig->inPorts  = 1;
		pConfig->outPorts = 1;
		
		// allocate memory for the data structure
		pConfig->pTaskData = call Titan.allocMemory(pConfig->taskID, sizeof(FuzzyTaskData));
		if (pConfig->pTaskData == NULL)
		{
			call Titan.issueError(ERROR_NO_MEMORY);
		}

		// for easy access		
		pData = (FuzzyTaskData*)pConfig->pTaskData;
		
		////////////////////////////////////////////////////////////////////////
		// Application dependent code
		
		pData->uiSelApp = pConfig->configData[0];
		
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
		FuzzyTaskData *pData;
		TitanPacket *pPacketIn;
		TitanPacket *pPacketOut;
		
		uint8_t uiSelApp;
		
		if ( pConfig == NULL ) {
			dbg("TitanTask","WARNING: Got no context in task Fuzzy\n");
			return;
		}
		
		pData  = (FuzzyTaskData*)pConfig->pTaskData;
		
		////////////////////////////////////////////////////////////////////////
		// Application dependent code

		// use these functions to access packets		

		// input
		pPacketIn = call Titan.getNextPacket(pConfig->taskID, 0);
		
		uiSelApp = pData->uiSelApp;
		
		if (pPacketIn == NULL)
		{
			call Titan.issueError(ERROR_IN_FIFO_EMPTY);
			dbg("TitanTask", "Task Fuzzy: NULL packet received\n");
			return;
		}
		
		pPacketOut = call Titan.allocPacket(pConfig->taskID, 0);
		
		if (pPacketOut == NULL)
		{
			call Titan.issueError(ERROR_OUT_FIFO_FULL);
			dbg("TitanTask", "Task Fuzzy: NULL packet received\n");
			return;
		}

		if (pPacketIn->type == TT_INT8 || pPacketIn->type == TT_UINT8)
		{
			int8_t  *pDataIn  = (int8_t*)pPacketIn->data;
			int16_t *pDataOut = (int16_t*)pPacketOut->data;
			int16_t label = 0;
			
			/* "fuzzy" functions */
			switch (uiSelApp)
			{
				// application foam dice
				case 1:		label = 0x100*pDataIn[0] + 0x010*pDataIn[1] + 0x001*pDataIn[2];

							// classification
							switch (label)
							{
								case 0x112 : *(pDataOut) = 1; break;
								case 0x011 : *(pDataOut) = 2; break;
								case 0x101 : *(pDataOut) = 3; break;
								case 0x121 : *(pDataOut) = 4; break;
								case 0x211 : *(pDataOut) = 5; break;
								case 0x110 : *(pDataOut) = 6; break;
								default : *(pDataOut) = 0;
							}
							
							dbg("TitanTask", "Fuzzy: label = %i, score = %i.\n", label, *(pDataOut));
							break;
	
				// further appliations	
				case 2:		//...
							//break;
				case 3:		//...
							//break;
				default:	dbg("TitanTask", "Fuzzy: no application is selected.\n");
							call Titan.issueError(ERROR_TYPE);
			}
		}
		else 
		{
		  dbg( "TitanTask", "Fuzzy: unknown data type\n" );
		  call Titan.issueError(ERROR_TYPE);
		  return;
		}
		
		pPacketOut->length = 2;
		pPacketOut->type = TT_INT16;
		dbg("TitanTask", "Fuzzy: type = %i.\n", pPacketOut->type);
		
		call Titan.sendPacket( pConfig->taskID, 0, pPacketOut );
		
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
	async event error_t Titan.packetAvailable(TitanTaskConfig* pConfig, uint8_t uiPort)
	{
		// start the working task
		if (call Titan.postExecution(pConfig,uiPort) == FAIL ) {
		  call Titan.issueError(6);
		  return FAIL;
		}
		return SUCCESS;
	}
}
