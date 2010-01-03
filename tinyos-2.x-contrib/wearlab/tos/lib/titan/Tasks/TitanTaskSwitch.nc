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
  * TitanTaskSwitch.nc
  *
  * @author Andreas Breitenmoser
  *
  * The switch task controls an analog switch that can be used for various switching actions, such as turning a lamp on.
  *
  * Configuration is  1 byte:
  * [uiNotInverted]
  *
  * uiNotInverted: defines if the switch works in positive logic or negative logic mode, i.e if switch on turns the switch on or off.
		    1 == positive logic (switch on: turns on, switch off: turns off)
		    0 == negative logic (switch on: turns off, switch off: turns on)
  *
  * Created: 28.01.2008
  * Last modified: 29.01.2008
  */

module TitanTaskSwitch
{
	uses interface Titan;
	uses interface Switch;
}
implementation
{
	
    event error_t Titan.init()
	{	  
	  // initialise port settings
	  call Switch.switchInitPort();
		
      return SUCCESS;
    }

	
	/**
	* Returns the universal task identifier of the task. These identifiers are 
	* stoled0 in TitanTaskUIDs.h and should be statically programmed.
	* @return Universal Task Identifier
	*/
	event TitanTaskUID Titan.getTaskUID()
	{
		return TITAN_TASK_SWITCH;
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
		uint8_t uiNotInverted;
		
		// define number of i/o-ports		
		pConfig->inPorts  = 1;
		pConfig->outPorts = 0;
		
		if (pConfig->configLength != 1) {
		  dbg( "TitanTask", "Task Switch: Unknown configuration size\n");
  		  call Titan.issueError(ERROR_CONFIG);
		  return FALSE;
		}
		
		uiNotInverted = pConfig->configData[0];
		
		// enable negative logic if parameter is 1
		//if (uiNotInverted == 1)
		//	call Switch.switchNotInverted();
		// enable negative logic if parameter is 0
		//else if (uiNotInverted == 0)
		//	call  Switch.switchInverted();
		//else
		//{
		//  dbg( "TitanTask", "Task Switch: Unknown configuration parameter\n");
  		//  call Titan.issueError(ERROR_CONFIG);
		//  return FALSE;
		//}

		dbg( "TitanTask", "New Task with ID %i: Switch\n", pConfig->taskID );
		
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
    call Switch.switchToggle();
			
		return SUCCESS;
	}
	
	/**
	* Starts the operation of a task. This could also wake it up again.
	*/
	event error_t Titan.start(TitanTaskConfig* pConfig)
	{
		dbg( "TitanTask", "Starting task Switch.\n" );
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
		// get context stuff
		TitanPacket *pPacketIn;
		
		if ( pConfig == NULL ) {
			dbg("TitanTask","WARNING: Got no context in task Switch\n");
			return;
		}
		
		// get in packets
		pPacketIn  = call Titan.getNextPacket(pConfig->taskID, 0);
		
		if (pPacketIn == NULL)
		{
          call Titan.issueError(ERROR_PACKET);
          return;
        }
		
		
		// get action from received packets
		// TODO: implement real actions
		
		if (pPacketIn->length == 2)
				call Switch.switchToggle();

		// TinyOS 2.0 scheduler drops out
        if ( call Titan.hasPacket( pConfig->taskID, 0 ) > 0 ) {
          call Titan.postExecution(pConfig,uiPort);
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
