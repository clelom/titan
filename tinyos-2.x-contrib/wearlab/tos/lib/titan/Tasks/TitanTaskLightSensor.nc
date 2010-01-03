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
  * TitanTaskLightSensor.nc
  *
  * @author Andreas Breitenmoser
  *
  * Titanic Smart Objects
  *
  * The light sensor task samples an ambient light sensor. This task can only be instantiated once!
  *
  * Created: 23.01.2008
  * Last modified: 23.01.2008
  */

#include "sensors.h"
  
module TitanTaskLightSensor
{
	uses interface Titan;
	uses interface Read<light_read_t> as Light;
	uses interface GeneralIO as PS_LIGHT;
	uses interface Timer<TMilli>;
	uses interface Leds; //AB: Leds only added for debugging
}
implementation
{
	// the data used by the task should always be 
	// organized in a structure.
	typedef struct TitanTaskLightSensorData {
    uint8_t    uiTaskID;
    uint16_t   uiSamplePeriod;
    uint8_t    uiSamplesPerPacket;
    light_read_t * pNextValue;
    light_read_t * pValues;
    uint8_t    uiCounter;
	} TitanTaskLightSensorData;
  
	TitanTaskLightSensorData* m_pData;

	
	event error_t Titan.init()
	{
	  m_pData = NULL;
	  
	  // turn power of light sensor off
	  call PS_LIGHT.clr();
		
	  return SUCCESS;
	}

	
	/**
	* Returns the universal task identifier of the task. These identifiers are 
	* stored in TitanTaskUIDs.h and should be statically programmed.
	* @return Universal Task Identifier
	*/
	event TitanTaskUID Titan.getTaskUID()
	{
		return TITAN_TASK_LIGHTSENSOR;
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
		uint16_t uiSamplePeriod = 0;
		uint8_t uiSamplesPerPacket = 1;
		uint8_t *pCfgData = pConfig->configData;

		// only instantiate once
		if ( m_pData != NULL )
			return FAIL;

		// define number of i/o-ports		
		pConfig->inPorts  = 0;
		pConfig->outPorts = 1;

		// check the configuration size to determine what is available
		switch (pConfig->configLength)
		{
		case 3:
			uiSamplesPerPacket = pCfgData[2];
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
    
		// allocate memory for the data structure
		pConfig->pTaskData = call Titan.allocMemory(pConfig->taskID,sizeof(TitanTaskLightSensorData)+
                                               uiSamplesPerPacket*sizeof(light_read_t));
		if (pConfig->pTaskData == NULL)
			call Titan.issueError(ERROR_NO_MEMORY);

		// init context structure
		m_pData = (TitanTaskLightSensorData*)pConfig->pTaskData;
		m_pData->uiSamplePeriod = uiSamplePeriod;
		m_pData->uiTaskID = pConfig->taskID;
		m_pData->uiSamplesPerPacket = uiSamplesPerPacket;
		m_pData->pValues = (light_read_t*)(m_pData+1); // set appending to this structure
		m_pData->pNextValue = m_pData->pValues;

		// set pin for supplying the light sensor as output
		call PS_LIGHT.makeOutput();
	
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
		m_pData = NULL;

		// turn power of light sensor off
		call PS_LIGHT.clr();
	
		call Timer.stop();

		return result;
	}
	
	/**
	* Starts the operation of a task. This could also wake it up again.
	*/
	event error_t Titan.start( TitanTaskConfig* pConfig )
	{
		if (m_pData == NULL ) return FAIL;
	
		// turn power of light sensor on
		call PS_LIGHT.set();

		call Timer.startPeriodic( m_pData->uiSamplePeriod );
	
		return SUCCESS;
	}
	
	/**
	* Stops the operation of a task. It should then prepare a sleep mode.
	*/
	event error_t Titan.stop( TitanTaskConfig* pConfig )
	{
		// turn power of light sensor off
		call PS_LIGHT.clr();
	
		call Timer.stop();
	
		return SUCCESS;
	}
	
	////////////////////////////////////////////////////////////////////////////
	// This is the actual working thread	
	task void run()
	{
		uint8_t cnt;
    TitanPacket *pPacketOut;
    
    if ( m_pData == NULL ) {
      return;
    }
  
		// reserve a packet
		pPacketOut = call Titan.allocPacket(m_pData->uiTaskID, 0);
    
		if (pPacketOut == NULL )
			return;

		// fill data
		pPacketOut->type   = TT_INT16;
    
		// send all available data
		atomic
		{
			uint8_t i=0,j;
			light_read_t *pData = m_pData->pValues;
			for (j=0; j<m_pData->uiSamplesPerPacket; j++)
			{
				pPacketOut->data[i++] = (*pData & 0xFF);
				pPacketOut->data[i++] = (((*pData)>>8) & 0xFF);
				pData++;
			}
			pPacketOut->length = i;
		}
    
	    // test code
		//pPacketOut->data[pPacketOut->length++] = m_pData->uiCounter++;
		m_pData->uiCounter++;
		if (m_pData->uiCounter > 127)
			m_pData->uiCounter = 0;
		cnt = m_pData->uiCounter;
		cnt = cnt | (1<<7); // number of the current node is inserted at the MSB of the counter value
		pPacketOut->data[pPacketOut->length++] = cnt;
	    // end test code
    
		// send packet
		call Titan.sendPacket(m_pData->uiTaskID, 0, pPacketOut);
		
	}

	/**
	* Is issued when a new packet arrives at the input port.
	* @param  iPort Port where the packet arrives
	* @return SUCCESS if the packet will be processed
	*/
	async event error_t Titan.packetAvailable( TitanTaskConfig* pConfig, uint8_t uiPort )
	{
		call Titan.issueError(ERROR_INPUT);
		return FAIL;
	}
  
  /* ********************************************************************** */
  
  event void Light.readDone(error_t result, light_read_t val)
  {
    atomic
	{ 
      *(m_pData->pNextValue) = val;
    }
    
	m_pData->pNextValue++;
    
    // check whether a packet is full
    if ( m_pData->pNextValue >= m_pData->pValues + m_pData->uiSamplesPerPacket )
	{
      post run();
      m_pData->pNextValue = m_pData->pValues;
    }
  }
  
  event void Timer.fired()
  {
    call Light.read();
  }
}
