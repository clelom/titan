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
  * @author Andreas Breitenmoser
  *
  * Titanic Smart Objects
  *
  * The accelerometer task samples an accelerometer residing on an extension board. This task can only be instantiated once!
  */

#include "sensors.h"
  
module TSOAccelerometerPPSM
{
	uses interface Titan;
	uses interface Read<acc_read_t*>;
	uses interface GeneralIO as PS_ACC;
	uses interface Timer<TMilli>;
}

implementation
{
	// the data used by the task should always be 
	// organized in a structure.
	typedef struct TitanTaskAccelerometerData {
    uint8_t    uiTaskID;
    uint16_t   uiSamplePeriod;
    uint8_t    uiSamplesPerPacket;
    acc_read_t *pNextValue;
    acc_read_t *pValues;
    uint8_t    uiCounter;
	} TitanTaskAccelerometerData;
  
  TitanTaskConfig* m_pConfig;

  event error_t Titan.init() {
    
	m_pConfig = NULL;
	
	// turn power of accelerometer off
  #ifndef TELOSB
	call PS_ACC.clr();
  #endif
	
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
    TitanTaskAccelerometerData* pData;

    // only instantiate once
    if ( m_pConfig != NULL ) {
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
        return FAIL;
    }
  
  m_pConfig = pConfig;
  
	// allocate memory for the data structure
	pConfig->pTaskData = call Titan.allocMemory(pConfig->taskID,sizeof(TitanTaskAccelerometerData)+
                                               uiSamplesPerPacket*sizeof(acc_read_t));
	if ( pConfig->pTaskData == NULL ) {
		call Titan.issueError(ERROR_NO_MEMORY);
	}

	// init context structure
	  pData = (TitanTaskAccelerometerData*)pConfig->pTaskData;
    pData->uiSamplePeriod = uiSamplePeriod;
    pData->uiTaskID = pConfig->taskID;
    pData->uiSamplesPerPacket = uiSamplesPerPacket;
    pData->pValues = (acc_read_t*)(pData+1); // set appending to this structure
    pData->pNextValue = pData->pValues;

	// set pin for supplying the accelerometer as output
  #ifndef TELOSB
	call PS_ACC.makeOutput();
  #endif
	
	return SUCCESS;
	}
	
	
	/**
	* Indicates that the task will be terminated. After this event has been 
	* received, the task should process no more data and free all resources 
	* allocated.
	* @return Whether the task has successfully been terminated.
	*/
	event error_t Titan.terminate( TitanTaskConfig* pConfig ) {
	
	error_t result;
	atomic{
		call Timer.stop();
	    result = call Titan.freeMemory(pConfig->taskID, pConfig->pTaskData);
  		m_pConfig = NULL;
		}

	// turn power of accelerometer off
  #ifndef TELOSB
	call PS_ACC.clr();
  #endif
	


	return result;
	
	}
	
	/**
	* Starts the operation of a task. This could also wake it up again.
	*/
	event error_t Titan.start( TitanTaskConfig* pConfig ) {
    if (m_pConfig == NULL ) return FAIL;
	
	// turn power of accelerometer on
  #ifndef TELOSB
	call PS_ACC.set();
  #endif

    call Timer.startPeriodic( ((TitanTaskAccelerometerData*)m_pConfig->pTaskData)->uiSamplePeriod );
		return SUCCESS;
	}
	
	/**
	* Stops the operation of a task. It should then prepare a sleep mode.
	*/
	event error_t Titan.stop( TitanTaskConfig* pConfig ) {
	
	// turn power of accelerometer off
  #ifndef TELOSB
	call PS_ACC.clr();
  #endif
	
    call Timer.stop();
		return SUCCESS;
	}
	
	////////////////////////////////////////////////////////////////////////////
	// This is the actual working thread	

  event void Titan.execute( TitanTaskConfig* pConfig, uint8_t uiPortID ) {

  	TitanPacket *pPacketOutX;
  	TitanPacket *pPacketOutY;
  	TitanPacket *pPacketOutZ;
    TitanTaskAccelerometerData* pData;

    atomic {
      uint8_t i=0,j;
      acc_read_t *pAccData;
    
      if ( m_pConfig == NULL ) return;
      pData = (TitanTaskAccelerometerData*)m_pConfig->pTaskData;
    
    	// reserve a packet
    	pPacketOutX = call Titan.allocPacket( pData->uiTaskID, 0 );
    	pPacketOutY = call Titan.allocPacket( pData->uiTaskID, 1 );
    	pPacketOutZ = call Titan.allocPacket( pData->uiTaskID, 2 );
      
      if (pPacketOutX == NULL ||  pPacketOutY == NULL || pPacketOutZ == NULL ) {
        return;
      }

      // fill data
      pPacketOutX->type   = TT_INT16;
      pPacketOutY->type   = TT_INT16;
      pPacketOutZ->type   = TT_INT16;
      
      // send all available data
      i=0;
      pAccData = pData->pValues;
      for ( j=0; j<pData->uiSamplesPerPacket; j++ ) {
        pPacketOutX->data[i  ] = ( pAccData->x &0xFF);
        pPacketOutX->data[i+1] = ((pAccData->x >>8)&0xFF);
        pPacketOutY->data[i] = ( pAccData->y &0xFF);
        pPacketOutY->data[i+1] = ((pAccData->y >>8)&0xFF);
        pPacketOutZ->data[i] = ( pAccData->z &0xFF);
        pPacketOutZ->data[i+1] = ((pAccData->z >>8)&0xFF);
        pAccData++;
        i+=2;
      }
      pPacketOutX->length = i;
      pPacketOutY->length = i;
      pPacketOutZ->length = i;
    
      // send packet
  		call Titan.sendPacket( pData->uiTaskID, 0, pPacketOutX );
  		call Titan.sendPacket( pData->uiTaskID, 1, pPacketOutY );
  		call Titan.sendPacket( pData->uiTaskID, 2, pPacketOutZ );
    }
		
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
  
  event void Read.readDone(error_t result, acc_read_t* val) {

    TitanTaskAccelerometerData* pData;

    P2OUT |= BIT0;
  
    if ( m_pConfig == NULL ) return;
    pData = (TitanTaskAccelerometerData*)m_pConfig->pTaskData;
  
    atomic{ 
      pData->pNextValue->x = val->x;
      pData->pNextValue->y = val->y;
      pData->pNextValue->z = val->z;
    }
    
    pData->pNextValue++;
    
    // check whether a packet is full
    if ( pData->pNextValue >= pData->pValues + pData->uiSamplesPerPacket ) {
      call Titan.postExecution(m_pConfig,0);
      pData->pNextValue = pData->pValues;
    }

  }
  
  event void Timer.fired() {
    call Read.read();
  }

}
