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
  * TossimAccelerometerP.nc
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  *
  * This is a Tossim acceleration simulation that reads values from a file with the name 
  * "adcXXX.txt", where XXX is the NodeID (with leadings zeros). The file format is ASCII, 
  * per line: 
  * 
  * [Time ValX ValY ValZ]
  * 
  * Where Time is a long and ValX/Y/Z are uint12
  *
  * Where time is given in milliseconds from the beginning of the simulation. When sampled, the 
  * timestamp of the least smaller line is taken.
  *
  */

#include "sim_tossim.h"  
  
module TossimAccelerometerP {
	uses interface Titan;
  uses interface Timer<TMilli>;
}

implementation {

  typedef struct AccFileLine {
    long time;
    uint16_t x;
    uint16_t y;
    uint16_t z;
  } AccFileLine;

	// the data used by the task should always be 
	// organized in a structure.
	typedef struct TitanTaskAccelerometerData {
    uint8_t     uiTaskID;
    uint16_t    uiSamplePeriod;
    uint8_t     uiSamplesPerPacket;
    AccFileLine nextFileLine;
    AccFileLine curFileLine;
    AccFileLine *pNextValue;
    AccFileLine *pValues;
    uint8_t     uiCounter;
    FILE*       pFile;
	} TitanTaskAccelerometerData;
  
  TitanTaskConfig* m_pConfig;
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

    uint16_t uiSamplePeriod=0;
    uint8_t uiSamplesPerPacket = 1;
    uint8_t *pCfgData = pConfig->configData;

    m_pConfig = pConfig;

    // only instantiate once
    if ( m_pData != NULL ) {
      return FAIL;
    }

		// define number of i/o-ports		
		pConfig->inPorts  = 0;
		pConfig->outPorts = 3;

    // check the configuration size to determine what is available
    switch ( pConfig->configLength ) {
      case 3:
        uiSamplesPerPacket = pConfig->configData[2];
      case 2:
        uiSamplePeriod = ((uint16_t)*pCfgData)<<8;
        pCfgData++;
      case 1:
        uiSamplePeriod += *pCfgData;
        break;
      default:
        call Titan.issueError(ERROR_CONFIG);
        dbgerror("TitanTask","Acceleration task: configuration error\n");
        return FAIL;
    }
    
		// allocate memory for the data structure
		pConfig->pTaskData = call Titan.allocMemory(pConfig->taskID,sizeof(TitanTaskAccelerometerData)+
                                                uiSamplesPerPacket*sizeof(AccFileLine));
		if ( pConfig->pTaskData == NULL ) {
			call Titan.issueError(ERROR_NO_MEMORY);
		}

		// init context structure
		m_pData = (TitanTaskAccelerometerData*)pConfig->pTaskData;
    m_pData->uiSamplePeriod = uiSamplePeriod;
    m_pData->uiTaskID = pConfig->taskID;
    m_pData->uiSamplesPerPacket = uiSamplesPerPacket;
    m_pData->pValues = (AccFileLine*)(m_pData+1); // set appending to this structure
    m_pData->pNextValue = m_pData->pValues;
    m_pData->nextFileLine.time = 0;

    // open file
#ifdef FROM_FILE
    {
      char strFilename[256];
      sprintf(strFilename,"adc%03u.txt",(unsigned)sim_node());
      m_pData->pFile = fopen(strFilename,"r");
      
      if (m_pData->pFile == NULL) {
        dbgerror("TitanTask","Task Accelerometer: Could not open file: %s\n", strFilename);
        return FAIL;
      }
    }
#endif
    dbg( "TitanTask", "Task Accelerometer: successfully configured (sampleperiod=%u, samplesperpacket=%u)\n", m_pData->uiSamplePeriod,m_pData->uiSamplesPerPacket);

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
    
    dbg("ADC","Terminated\n");

    call Timer.stop();

		return result;
	}
	
	/**
	* Starts the operation of a task. This could also wake it up again.
	*/
	event error_t Titan.start( TitanTaskConfig* pConfig ) {
    if (m_pData == NULL ) return FAIL;
#ifdef FROM_FILE
    if (m_pData->pFile == NULL ) return FAIL;
#endif
    dbg( "TitanTask", "Task Accelerometer: starting timer with sampleperiod=%u", m_pData->uiSamplePeriod);
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
	event void Titan.execute(TitanTaskConfig* pConfig, uint8_t uiPortID) {

	// reserve a packet
	TitanPacket *pPacketOutX = call Titan.allocPacket( m_pData->uiTaskID, 0 );
	TitanPacket *pPacketOutY = call Titan.allocPacket( m_pData->uiTaskID, 1 );
	TitanPacket *pPacketOutZ = call Titan.allocPacket( m_pData->uiTaskID, 2 );
    
    if (pPacketOutX == NULL ||  pPacketOutY == NULL || pPacketOutZ == NULL ) {
      return;
    }

    // fill data
    pPacketOutX->type   = TT_INT16;
    pPacketOutY->type   = TT_INT16;
    pPacketOutZ->type   = TT_INT16;
    
    // send all available data
    atomic {
    
      uint8_t i=0,j;
      AccFileLine *pData = m_pData->pValues;
      for ( j=0; j<m_pData->uiSamplesPerPacket; j++ ) {
        pPacketOutX->data[i  ] = ( pData->x &0xFF);
        pPacketOutX->data[i+1] = ((pData->x >>8)&0xFF);
        pPacketOutY->data[i] = ( pData->y &0xFF);
        pPacketOutY->data[i+1] = ((pData->y >>8)&0xFF);
        pPacketOutZ->data[i] = ( pData->z &0xFF);
        pPacketOutZ->data[i+1] = ((pData->z >>8)&0xFF);
        pData++;
        i+=2;
      }
      pPacketOutX->length = i;
      pPacketOutY->length = i;
      pPacketOutZ->length = i;
    }
    
    dbg("TitanTask","AccelerationTask: Sending sampling results");
    
    // send packet
		call Titan.sendPacket( m_pData->uiTaskID, 0, pPacketOutX );
		call Titan.sendPacket( m_pData->uiTaskID, 1, pPacketOutY );
		call Titan.sendPacket( m_pData->uiTaskID, 2, pPacketOutZ );

	}

	/**
	* Is issued when a new packet arrives at the input port.
	* @param  iPort Port where the packet arrives
	* @return SUCCESS if the packet will be processed
	*/
	async event error_t Titan.packetAvailable( TitanTaskConfig* pConfig, uint8_t uiPort ) {
    call Titan.issueError(ERROR_INPUT);
    dbg("TitanTask","AccelerationTask: Error: received input!");
    return FAIL;
	}
  
  /* ********************************************************************** */
  
  event void Timer.fired() {
  
    // get miliseconds now
    sim_time_t t = sim_time();
    long mSec = t/sim_ticks_per_sec();
    dbg("ADC","Sampling at time %u\n", mSec);
#ifdef FROM_FILE
    // make sure we got the latest reading line
    while ( mSec <= m_pData->curFileLine.time ) {
      unsigned int varsRead,mytime,x,y,z;
      m_pData->curFileLine = m_pData->nextFileLine;
      varsRead = fscanf( m_pData->pFile, "%u %u %u %u", &mytime,&x,&y,&z);
      m_pData->nextFileLine.time = (long)mytime;
      m_pData->nextFileLine.x=(uint16_t)x;
      m_pData->nextFileLine.y=(uint16_t)y;
      m_pData->nextFileLine.z=(uint16_t)z;
      if ( varsRead != 4 ) {
        dbgerror("ADC","Error while reading file: %u\n", varsRead);
      }
    }

    m_pData->pNextValue->x = m_pData->curFileLine.x;
    m_pData->pNextValue->y = m_pData->curFileLine.y;
    m_pData->pNextValue->z = m_pData->curFileLine.z;
#else
    m_pData->pNextValue->x = 1;
    m_pData->pNextValue->y = 2;
    m_pData->pNextValue->z = 3;
#endif  
    // check whether a packet is full
    m_pData->pNextValue++;
    if ( m_pData->pNextValue >= m_pData->pValues + m_pData->uiSamplesPerPacket ) {
      call Titan.postExecution(m_pConfig,0);
      m_pData->pNextValue = m_pData->pValues;
    }
  }

}
