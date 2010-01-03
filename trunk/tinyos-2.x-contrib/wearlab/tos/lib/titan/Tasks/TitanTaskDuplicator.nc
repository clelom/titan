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
  * TitanTaskTemplate.nc
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  *
  * Duplicates an incoming packet to two or number of configured outgoing ports.
  * 
  * Configuration data:
  * [ uint8_t uiOutPorts ]
  */

module TitanTaskDuplicator {
	uses interface Titan;
}

implementation {

    event error_t Titan.init() {
      return SUCCESS;
    }

	event TitanTaskUID Titan.getTaskUID() {
		return TITAN_TASK_DUPLICATOR;
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
		pConfig->outPorts = (pConfig->configLength > 0)? pConfig->configData[0]:2;
	
		dbg( "TitanTask", "New task with ID: %i: Duplicator (%u outputs)\n", 
		     pConfig->taskID, pConfig->outPorts );

		return SUCCESS;
	}
	
	
	/**
	* Indicates that the task will be terminated. After this event has been 
	* received, the task should process no more data and free all resources 
	* allocated.
	* @return Whether the task has successfully been terminated.
	*/
	event error_t Titan.terminate( TitanTaskConfig* pConfig ) {
	
		return SUCCESS;
	}
	
	/**
	* Starts the operation of a task. This could also wake it up again.
	*/
	event error_t Titan.start( TitanTaskConfig* pConfig ) {
		dbg( "TitanTask", "Starting TitanTaskDuplicator.\n" );
		return SUCCESS;
	}
	
	/**
	* Stops the operation of a task. It should then prepare a sleep mode.
	*/
	event error_t Titan.stop( TitanTaskConfig* pConfig ) {
		return SUCCESS;
	}
	
	////////////////////////////////////////////////////////////////////////////
	// This is the actual working thread	
	event void Titan.execute( TitanTaskConfig* pConfig, uint8_t uiPortID ) {

		TitanPacket *pInPacket;
		
		int i;
		
		if ( pConfig == NULL ) {
			dbg("TitanTask","WARNING: Got no context in task Duplicator\n");
			return;
		}

		// get the input packet
		pInPacket = call Titan.getNextPacket( pConfig->taskID, 0 );

    if ( pInPacket == NULL ) {
			dbg("TitanTask","Task duplicator: did not get message from input port\n");
      call Titan.issueError(ERROR_PACKET);
      return;
    }
		
		// copy packet to all outputs
#ifdef MEASURE_CYCLES
        P6OUT |=  BIT1;  // set on
#endif
#ifdef MEASURE_CONFIG
  P6OUT |= BIT1;
#endif
		for ( i=0; i < pConfig->outPorts; i++ ) {

			TitanPacket *pOutPacket = call Titan.allocPacket( pConfig->taskID, i );
			uint8_t *pDataIn = pInPacket->data;
      uint8_t *pDataOut;
			int j;
      
			if ( pOutPacket == NULL ) {
        dbg("TitanTask", "Task Duplicator: Output queue full on port %i\n", i );
        call Titan.issueError(ERROR_OUT_FIFO_FULL);
        return;
			}

			pDataOut = pOutPacket->data;

			
			// copy the data
			for ( j=0; j < pInPacket->length; j++ ) {
				*pDataOut = *pDataIn;
				pDataOut++;pDataIn++;
			}
			pOutPacket->length = pInPacket->length;
			pOutPacket->type   = pInPacket->type;
			
			// send the packet	
//            dbg("TitanTask", "Task Duplicator: Sending packet to port %i\n", i );
			call Titan.sendPacket( pConfig->taskID, i, pOutPacket );
			
		}

#ifdef MEASURE_CYCLES
        P6OUT &= ~BIT1;  // set off
#endif
#ifdef MEASURE_CONFIG
  P6OUT &= ~BIT1;
#endif

        // TinyOS 2.0 scheduler drops out
        if ( call Titan.hasPacket( pConfig->taskID, uiPortID ) > 0 ) {
          call Titan.postExecution(pConfig,uiPortID);
        }

	}

	/**
	* Is issued when a new packet arrives at the input port.
	* @param  iPort Port where the packet arrives
	* @return SUCCESS if the packet will be processed
	*/
	async event error_t Titan.packetAvailable( TitanTaskConfig* pConfig, uint8_t uiPort ) {

		// start the working task
		if (call Titan.postExecution(pConfig,uiPort) == FAIL ) {
		  call Titan.issueError(ERROR_PACKET);
		  return FAIL;
		}

		return SUCCESS;
	}

}
