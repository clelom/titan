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
  * TitanTaskAccelerometer.nc
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  *
  * The accelerometer samples an accelerometer residing on an extension board.
  */

module TAAccelerometerM {
	uses interface Titan;
  uses interface GeneralIO as ACC;
  uses interface ReadSeq<uint16_t> as Read;
  uses interface Timer<TMilli>;
}

implementation {

	// the data used by the task should always be 
	// organized in a structure.
	typedef struct TitanTaskAccelerometerData {
    uint8_t    uiTaskID;
		uint16_t   uiWindowSize;
    uint16_t   uiSamplePeriod;
    uint16_t*  pNextData;
    uint16_t*  data1;
    uint16_t*  data2;
	} TitanTaskAccelerometerData;
  
  TitanTaskAccelerometerData* m_pData;

    event error_t Titan.init() {
      m_pData = NULL;
      return SUCCESS;
    }

	/**
	* Returns the universal task identifier of the task. These identifiers are 
	* stored in TitanTaskUIDs.h and should be statically programmed.
	* @return Universal Task Identifier
	*/
	event TitanTaskUID Titan.getTaskUID() {
		return TITAN_TASK_ACCELEROMETER;
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

    uint16_t uiSamplePeriod, uiWindowSize;
    uint16_t uiWindowMemorySize;

		// define number of i/o-ports		
		pConfig->inPorts  = 0;
		pConfig->outPorts = 1;
    
    if ( pConfig->configLength != 4 ) {
      call Titan.issueError(ERROR_CONFIG);
      return FAIL;
    }
    
    // only instantiate once
    if ( m_pData != NULL ) {
      return FAIL;
    }

    uiWindowSize = ((uint16_t)pConfig->configData[0]<<8) + pConfig->configData[1];
    uiSamplePeriod = ((uint16_t)pConfig->configData[2]<<8) + pConfig->configData[3];

		// allocate memory for the data structure + 2 windows of 16 bytes times 3 channels
    uiWindowMemorySize = (uiWindowSize<<1)*sizeof(uint16_t);
    uiWindowMemorySize = (uiWindowMemorySize<<1)+uiWindowMemorySize; // mult by 3
		pConfig->pTaskData = call Titan.allocMemory(pConfig->taskID,sizeof(TitanTaskAccelerometerData)+uiWindowMemorySize);
		if ( pConfig->pTaskData == NULL ) {
			call Titan.issueError(ERROR_NO_MEMORY);
		}

		// init context structure
		m_pData = (TitanTaskAccelerometerData*)pConfig->pTaskData;
    m_pData->uiWindowSize = uiWindowSize;
    m_pData->uiSamplePeriod = uiSamplePeriod;
    m_pData->data1 = (uint16_t*)(pConfig->pTaskData + sizeof(TitanTaskAccelerometerData));
    m_pData->data2 = m_pData->data1 + uiWindowSize;
    m_pData->pNextData  = m_pData->data1;
    
    call ACC.makeOutput();
    call ACC.set();

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
    m_pData = NULL;

    call ACC.clr();

		return result;
	}
	
	/**
	* Starts the operation of a task. This could also wake it up again.
	*/
	event error_t Titan.start( TitanTaskConfig* pConfig ) {
    call Timer.startPeriodic( m_pData->uiSamplePeriod );
		return SUCCESS;
	}
	
	/**
	* Stops the operation of a task. It should then prepare a sleep mode.
	*/
	event error_t Titan.stop( TitanTaskConfig* pConfig ) {
    call Timer.stop();
		return SUCCESS;
	}
	
	////////////////////////////////////////////////////////////////////////////
	// This is the actual working thread	
	task void run() {

		TitanTaskConfig *pConfig = call Titan.getContext();
		TitanTaskAccelerometerData *pData  = (TitanTaskAccelerometerData*)pConfig->pTaskData;
		
		////////////////////////////////////////////////////////////////////////
		// Application dependent code

		// use these functions to access packets		

		// input
//		TitanPacket *pPacketIn = call Titan.getNextPacket( pConfig->taskID, iPort );

		// output
//		TitanPacket *pPacketOut = call Titan.allocPacket( pConfig->taskID, oPort );
//		call Titan.sendPacket( pConfig->taskID, oPort, pPacketOut );

        // repost task if more packets are waiting
//        if ( Titan.hasPacket( pConfig->taskID, iPort ) > 0 ) {
//          post run();
//        }

	}

	/**
	* Is issued when a new packet arrives at the input port.
	* @param  iPort Port where the packet arrives
	* @return SUCCESS if the packet will be processed
	*/
	async event error_t Titan.packetAvailable( TitanTaskConfig* pConfig, uint8_t uiPort ) {
    call Titan.issueError(ERROR_INPUT);
    return FAIL;
	}
  
  /* ********************************************************************** */
  
  uint16_t m_uiResultBuffer[4];
  
  event void Read.readDone(error_t result0, uint16_t* buf, uint8_t num) {
    //TODO: what and how to save this data!!!
  }
  
  event void Timer.fired() {
    call Read.read(m_uiResultBuffer, 3, 6);
  }

}
