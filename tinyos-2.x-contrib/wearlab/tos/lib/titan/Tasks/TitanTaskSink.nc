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
  * TitanTaskSink.nc
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  *
  * The sink task just removes all incoming messages from the message queue.
  */

module TitanTaskSink {
	uses interface Titan;
}

implementation {

    event error_t Titan.init() {
      return SUCCESS;
    }

	/**
	* Returns the universal task identifier of the task. These identifiers are 
	* stored in TitanTaskUIDs.h and should be statically programmed.
	* @return Universal Task Identifier
	*/
	event TitanTaskUID Titan.getTaskUID() {
		return TITAN_TASK_SINK;
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
		pConfig->inPorts  = 127;
		pConfig->outPorts = 0;
		
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
		return SUCCESS;
	}
	
	/**
	* Stops the operation of a task. It should then prepare a sleep mode.
	*/
	event error_t Titan.stop( TitanTaskConfig* pConfig ) {
		return SUCCESS;
	}
  
  // nothing to run
  event void Titan.execute( TitanTaskConfig* pConfig, uint8_t uiPortID ) {};
	
	async event error_t Titan.packetAvailable( TitanTaskConfig* pConfig, uint8_t uiPort ) {
    #ifdef TOSSIM
      TitanPacket *pPacketIn = call Titan.getNextPacket( pConfig->taskID, uiPort );
      int i;
      char strOut[1024];
      int length;

      if (pPacketIn == NULL ) {
        dbg("TitanTask", "Task Sink: missing packet\n");
      }
      
      length = pPacketIn->length*((pPacketIn->type == TT_UINT16 || pPacketIn->type == TT_INT16)? 2 : 1);
      
      sprintf(strOut,"Task Sink: Packet received with %i bytes: ",length);
      for (i=0; i<length;i++) {
        char strAdd[256];
        sprintf(strAdd,"%02X ", pPacketIn->data[i]);
        strcat(strOut,strAdd);
      }
      strcat(strOut,"\n");

      dbg( "TitanTask", strOut );
    #else
  		// just get the message, but don't do anything with it
  		call Titan.getNextPacket( pConfig->taskID, uiPort );
    #endif

		return SUCCESS;
	}

}
