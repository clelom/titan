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
  * TitanTaskLEDs.nc
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  *
  * This task displays the lower 3 bits of the first byte of incoming data 
  * on the mote's LED array.
  */

module TitanTaskLEDs {
	uses interface Titan;
	uses interface Leds;
}

implementation {

    event error_t Titan.init() {
      return SUCCESS;
    }

	/**
	* Returns the universal task identifier of the task. These identifiers are 
	* stoled0 in TitanTaskUIDs.h and should be statically programmed.
	* @return Universal Task Identifier
	*/
	event TitanTaskUID Titan.getTaskUID() {
		return TITAN_TASK_LEDS;
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

		// define number of i/o-ports		
		pConfig->inPorts  = 1;
		pConfig->outPorts = 0;
		
		dbg( "TitanTask", "New Task with ID %i: Led\n", pConfig->taskID );
		
		return SUCCESS;
	}
	
	
	/**
	* Indicates that the task will be terminated. After this event has been 
	* received, the task should process no more data and free all resources 
	* allocated.
	* @return Whether the task has successfully been terminated.
	*/
	event error_t Titan.terminate( TitanTaskConfig* pConfig ) {
	    call Leds.led0Off();
	    call Leds.led1Off();
	    call Leds.led2Off();
		return SUCCESS;
	}
	
	/**
	* Starts the operation of a task. This could also wake it up again.
	*/
	event error_t Titan.start( TitanTaskConfig* pConfig ) {
		dbg( "TitanTask", "Starting task Leds.\n" );
		return SUCCESS;
	}
	
	/**
	* Stops the operation of a task. It should then prepare a sleep mode.
	*/
	event error_t Titan.stop( TitanTaskConfig* pConfig ) {
		return SUCCESS;
	}
	
	event void Titan.execute( TitanTaskConfig* pConfig, uint8_t uiPortID ) {

		uint8_t uiValue;

        TitanPacket *pPacket;
		
		if ( pConfig == NULL ) {
			dbg("TitanTask","WARNING: Got no context in task LEDs\n");
			return;
		}
		
         pPacket = call Titan.getNextPacket( pConfig->taskID, 0 );

#ifdef MEASURE_CONFIG
  P6OUT |= BIT1;
#endif
        if ( pPacket == NULL ) {
          call Titan.issueError(1);
          return;
        }

        dbg("TitanTask","TaskLED: received packet with length %i\n", pPacket->length );

        if ( pPacket->length >= 1 ) {
          uiValue = pPacket->data[0];
					(uiValue&1) != 0 ? call Leds.led0On() : call Leds.led0Off();
					(uiValue&2) != 0 ? call Leds.led1On() : call Leds.led1Off();
					(uiValue&4) != 0 ? call Leds.led2On() : call Leds.led2Off();
        }
        
#ifdef MEASURE_CONFIG
  P6OUT &= ~BIT1;
#endif

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
	async event error_t Titan.packetAvailable( TitanTaskConfig* pConfig, uint8_t uiPort ) {

    dbg("TitanTask","TaskLED: received packet on port %i\n", uiPort );
		if (call Titan.postExecution(pConfig,uiPort) == FAIL ) {
		  call Titan.issueError(6);
		  return FAIL;
		}

		return SUCCESS;
	}

}
