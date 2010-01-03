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
  * TitanTaskVoiceC.nc
  *
  * @author Andreas Breitenmoser
  *
  * The voice task controls V-Stamp, a text-to-speech voice synthesiser module.
  *
  * Configuration is  5 bytes (1, 1, 1, 1, 1):
  * [uiTextNum. uiSongNum, uiVolume, uiVoice, uiSpeed]
  *
  * uiTextNum: number of stored text.
  * uiSongNum: number of stored song. 
  * uiVolume: adjust volume of V-Stamp (0 - 9)
  * uiVoice: select one of the standard voices of V-Stamp (0 - 10)
  * uiSpeed: adjust speed of V-Stamp (0 - 13)
  *
  * Created: 22.01.2008
  * Last modified: 22.01.2008
  */

#include "TitanTaskVoice.h"
  
module TitanTaskVoice
{
	uses interface Titan;
	uses interface VStamp;
	uses interface Timer<TMilli>;
	uses interface Leds;
}
implementation
{
	// the data used by the task should always be 
	// organized in a structure.
	typedef struct TitanTaskVoiceData {
	uint8_t   uiSelApp;
	uint8_t	  uiCounter1;
	uint8_t   uiCounter2;
	uint8_t   uiCurScore;
	uint8_t	  uiVolume;
	uint8_t	  uiVoice;
	uint8_t	  uiSpeed;
	} TitanTaskVoiceData;
  
  TitanTaskVoiceData* m_pData;

  // global variable that indicates if voice synthesiser is ready
  bool vStampReady;
  
  // global variable that holds led state
  bool ledToggle;
  
  
	event error_t Titan.init()
	{
		m_pData = NULL;
		
		// initialise V-Stamp
		call Leds.led0On();
		call Leds.led1On();
		
		//ledToggle = FALSE;
		
		// init V-Stamp
		vStampReady = FALSE;
		call VStamp.init(FALSE);
		
	    return SUCCESS;
	}

	
	/**
	* Returns the universal task identifier of the task. These identifiers are 
	* stored in TitanTaskUIDs.h and should be statically programmed.
	* @return Universal Task Identifier
	*/
	event TitanTaskUID Titan.getTaskUID()
	{
		return TITAN_TASK_VOICE;
	}

	
	/**
	* Initializes a component and passes a configuration structure. In the pInOut 
	* structure, the task returns its configuration, ie. how many input and output 
	* ports it reserves according to the configuration received.
	* @param pConfig Pointer to the configuration structure for the task
	* @return Whether the task has successfully been initialized
	*/
	event error_t Titan.configure( TitanTaskConfig* pConfig )
	{
	    uint8_t uiSpeed = 5;
	    uint8_t uiVoice = 0;
	    uint8_t uiVolume = 5;
		uint8_t uiSelApp;
		uint8_t uiCounter1 = 0;
		uint8_t uiCounter2 = 0;
		uint8_t uiCurScore = 20; // != [0 ... 12]
	    uint8_t *pCfgData = pConfig->configData;
		TitanTaskVoiceData* pData;

	    // only instantiate once
	    if ( m_pData != NULL )
		    return FAIL;
	    
		// define number of i/o-ports
		pConfig->inPorts  = 1;
		pConfig->outPorts = 0;

	    // check the configuration size to determine what is available
	    switch (pConfig->configLength)
		{
	      case 4:
	        uiSpeed = pCfgData[3];
	      case 3:
	        uiVoice = pCfgData[2];
		  case 2:
	        uiVolume = pCfgData[1];
		  case 1:
			uiSelApp = pCfgData[0];
	        break;
	      default:
	        call Titan.issueError(ERROR_CONFIG);
	        return FAIL;
	    }
	    
		// allocate memory for the data structure
		pConfig->pTaskData = call Titan.allocMemory(pConfig->taskID, sizeof(TitanTaskVoiceData));
		if ( pConfig->pTaskData == NULL )
			call Titan.issueError(ERROR_NO_MEMORY);	
			
		// init context structure
		pData = (TitanTaskVoiceData*)pConfig->pTaskData;
	    pData->uiSpeed = uiSpeed;
	    pData->uiVoice = uiVoice;
	    pData->uiVolume = uiVolume;
		pData->uiSelApp = uiSelApp;
		pData->uiCounter1 = uiCounter1;
		pData->uiCounter2 = uiCounter2;
		pData->uiCurScore = uiCurScore;
		
		dbg("TitanTask", "Task Voice: configured ...", uiCounter1, uiCounter2);
		
		return SUCCESS;
	}
	
	
	/**
	* Indicates that the task will be terminated. After this event has been 
	* received, the task should process no more data and free all resources 
	* allocated.
	* @return Whether the task has successfully been terminated.
	*/
	event error_t Titan.terminate( TitanTaskConfig* pConfig )
	{
		error_t result = call Titan.freeMemory(pConfig->taskID, pConfig->pTaskData);
	    m_pData = NULL;

		// Shut down V-Stamp's audiosystem 
		//call Timer.stop();
		call Leds.led0On();
	    call Leds.led1On();
		//call VStamp.suspend();
		
		return result;
	}
	
	
	/**
	* Starts the operation of a task. This could also wake it up again.
	*/
	event error_t Titan.start( TitanTaskConfig* pConfig )
	{
		if (m_pData == NULL )
			return FAIL;
	
		// Wake V-Stamp up again
		//while(!vStampReady) {
			/* wait */
		//}
		
		//ledToggle = FALSE;
		//call Timer.startPeriodic(100);
		//call VStamp.resume();
	
		return SUCCESS;
	}
	
	
	/**
	* Stops the operation of a task. It should then prepare a sleep mode.
	*/
	event error_t Titan.stop( TitanTaskConfig* pConfig )
	{
		// Shut down V-Stamp's audiosystem 
		//call Timer.stop();
		
		//call VStamp.suspend();
	
		return SUCCESS;
	}
	
	
	////////////////////////////////////////////////////////////////////////////
	// This is the actual working thread	
	event void Titan.execute( TitanTaskConfig* pConfig, uint8_t uiPortID )
	{
		TitanTaskVoiceData *pData;
		uint16_t * pSource;
		TitanPacket *pPacketIn;
		
		uint8_t uiSpeed;
		uint8_t uiVoice;
		uint8_t uiVolume;
		uint8_t uiSelApp;
		uint8_t uiCounter1;
		uint8_t uiCounter2;
		uint8_t uiCurScore;
		
    call Leds.led0Toggle();
    
		if ( pConfig == NULL ) {
			dbg("TitanTask","WARNING: Got no context in task Voice\n");
			return;
		}
		
		pData = (TitanTaskVoiceData*)pConfig->pTaskData;
		
		// get in/out packets
		pPacketIn  = call Titan.getNextPacket(pConfig->taskID, 0);

		
        if (pPacketIn == NULL)
		{
          call Titan.issueError(ERROR_PACKET);
		  dbg("TitanTask", "Task Voice: NULL packet received\n");
          return;
        }

		uiSpeed = pData->uiSpeed;
		uiVoice = pData->uiVoice;
		uiVolume = pData->uiVolume;
		uiSelApp = pData->uiSelApp;
		uiCounter1 = pData->uiCounter1;
		uiCounter2 = pData->uiCounter2;
		uiCurScore = pData->uiCurScore;
		
		pSource = (uint16_t*)pPacketIn->data;

		dbg( "TitanTask", "Task Voice: starting for %i entries...\n", pPacketIn->length);
		
		////////////////////////////////////////////////////////////////////////
		// Application dependent code
		
		if (vStampReady == FALSE)	// should never be the case
		{
			//call Leds.led0Toggle();
			return;
		}
		
#ifndef ORIG_CODE		
    #warning Using direct text code
    call Leds.led1Toggle();
//    call VStamp.sendText("hello there\r\n");
    {
/*  		char number[5];
      number[0] = '~';
      number[1] = TO_ASCII + *pSource;
      number[2] = '&';
      number[3] = '\0';
      call VStamp.sendText(number);
*/
      call VStamp.stop();
      call VStamp.playSound(TO_ASCII + 0, TO_ASCII + *pSource);
    }

#else
		/* list of behaviour */
		switch (uiSelApp)
		{
			// foamDiceShake: indicate shaking
			case 1:		if (*pSource == 0)
						{
							uiCounter2 = 0;
							uiCounter1++;
							
							dbg("TitanTask", "Task Voice: counter1: %i, counter2: %i", uiCounter1, uiCounter2);
							
							if (uiCounter1 >= 4)
							{
								// configuration of V-Stamp:
								// set speed
								if (uiSpeed < 10)
									call VStamp.setSpeed(TO_ASCII, uiSpeed + TO_ASCII);
								else
									call VStamp.setSpeed(TO_ASCII+1, (uiSpeed-10) + TO_ASCII);
								// set volume	
								call VStamp.setVolume(uiVolume + TO_ASCII);
								// set voice
								if (uiVoice < 10)
									call VStamp.setVoice(TO_ASCII, uiVoice + TO_ASCII);
								else
									call VStamp.setVoice(TO_ASCII+1, (uiVoice-10) + TO_ASCII);
								
								// speak
								call VStamp.sendText(MOVE_2A);
									
								dbg("TitanTask", "Task Voice: speaking: %s", MOVE_2A);
							}
							else if (uiCounter1 >= 10)
							{
								// speak
								call VStamp.sendText(MOVE_2B);
									
								dbg("TitanTask", "Task Voice: speaking: %s", MOVE_2B);
							}
						}
						else
						{
							uiCounter1 = 0;
							uiCounter2++;
						
							dbg("TitanTask", "Task Voice: counter1: %i, counter2: %i", uiCounter1, uiCounter2);
						
							if (uiCounter2 >= 100)
							{
								// configuration of V-Stamp:
								// set speed
								if (uiSpeed < 10)
									call VStamp.setSpeed(TO_ASCII, uiSpeed + TO_ASCII);
								else
									call VStamp.setSpeed(TO_ASCII+1, (uiSpeed-10) + TO_ASCII);
								// set volume	
								call VStamp.setVolume(uiVolume + TO_ASCII);
								// set voice
								if (uiVoice < 10)
									call VStamp.setVoice(TO_ASCII, uiVoice + TO_ASCII);
								else
									call VStamp.setVoice(TO_ASCII+1, (uiVoice-10) + TO_ASCII);
								
								// speak
								call VStamp.sendText(MOVE_1B); break;
									
								dbg("TitanTask", "Task Voice: speaking: %s", MOVE_1B);
							}
						}
						
						break;
	
			// foamDiceScore: output the result
			case 2:		
            if (*pSource == uiCurScore)
						{
                            uiCounter1++;
                                
                            if (uiCounter1 == 4)
							{
                                /* output the score */
                                
								// configuration of V-Stamp:
								// set speed
								if (uiSpeed < 10)
									call VStamp.setSpeed(TO_ASCII, uiSpeed + TO_ASCII);
								else
									call VStamp.setSpeed(TO_ASCII+1, (uiSpeed-10) + TO_ASCII);
								// set volume	
								call VStamp.setVolume(uiVolume + TO_ASCII);
								// set voice
								if (uiVoice < 10)
									call VStamp.setVoice(TO_ASCII, uiVoice + TO_ASCII);
								else
									call VStamp.setVoice(TO_ASCII+1, (uiVoice-10) + TO_ASCII);
								
								// speak
						
								if (*pSource < 5)
								{
									call VStamp.setVoice(TO_ASCII+1, TO_ASCII);
									call VStamp.sendText(SCORE_6); 
								}
								else if (*pSource < 8)
									call VStamp.sendText(SCORE_4);
								else if (*pSource < 11)
									call VStamp.sendText(SCORE_3);
								else
								{
									call VStamp.setVoice(TO_ASCII, 8 + TO_ASCII);
									call VStamp.sendText(SCORE_1);
								}
								
								number[0] = TO_ASCII + *pSource;
								number[2] = '\0';
								
								call VStamp.sendText(number);
								
								dbg("TitanTask", "Task Voice: speaking: %s %s", MOVE_1B, number);
							
								//call VStamp.playSound(TO_ASCII, TO_ASCII + 1);
								
							}
						} 
                        else
						{
                            uiCounter1 = 0;
                            uiCurScore = *pSource;
                        }
					
						break;
			
			// foamDiceMission1
			case 3:		/* test */
						
						call VStamp.setSpeed(TO_ASCII, 5 + TO_ASCII);
						call VStamp.setVolume(5 + TO_ASCII);
						call VStamp.setVoice(TO_ASCII, TO_ASCII);
						
						call VStamp.sendText(MOVE_1A);
						
						break;
			
			default:	dbg("TitanTask", "Task Voice: no application is selected.\n");
						call Titan.issueError(ERROR_TYPE);
		}
		
		pData->uiCounter1 = uiCounter1;
		pData->uiCounter2 = uiCounter2;
		pData->uiCurScore = uiCurScore;
		
		// TinyOS 2.0 scheduler drops out
        if ( call Titan.hasPacket( pConfig->taskID, 0 ) > 0 ) {
          call Titan.postExecution(pConfig,0);
        }
#endif

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

	
	event void Timer.fired()
	{
		call Leds.led0Toggle();
		if (ledToggle)
		{
			call Leds.led1Toggle();
			ledToggle = FALSE;
		}
		else
		{
			ledToggle = TRUE;
		}
	}

	
	event void VStamp.uartReady()
	{
		vStampReady = TRUE;
	}
  
}
