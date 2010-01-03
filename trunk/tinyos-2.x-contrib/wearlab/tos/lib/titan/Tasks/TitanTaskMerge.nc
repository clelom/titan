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
  * TitanTaskMerge.nc
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  *
  * Merge Task merges the packets coming in at the input ports and produces a 
  * single output stream.
  */

module TitanTaskMerge {
	uses interface Titan;
}

implementation {

#define ESCAPE_VALUE 0xAA

	// the data used by the task should always be 
	// organized in a structure.
	typedef struct MergeTaskData {
    uint8_t uiCount;
    uint8_t uiBytesPerPacket; // number of bytes stored in a slot per packet
		TitanPacket *pCurPacket; // packet currently in use
	} MergeTaskData;

    event error_t Titan.init() {
      return SUCCESS;
    }

	/**
	* Returns the universal task identifier of the task. These identifiers are 
	* stored in TitanTaskUIDs.h and should be statically programmed.
	* @return Universal Task Identifier
	*/
	event TitanTaskUID Titan.getTaskUID() {
		return TITAN_TASK_MERGE;
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

		MergeTaskData* pData;


    if (pConfig->configLength < 1 || 2 < pConfig->configLength ) {
      call Titan.issueError(ERROR_CONFIG);
      dbg( "TitanTask", "Task Merge: Invalid configuration length\n" );
      return FAIL;
    }

		// define number of i/o-ports		
		pConfig->inPorts  = pConfig->configData[0];
		pConfig->outPorts = 1;

		// allocate memory for the data structure
		pConfig->pTaskData = call Titan.allocMemory(pConfig->taskID,sizeof(MergeTaskData));
		if ( pConfig->pTaskData == NULL ) {
			call Titan.issueError(ERROR_NO_MEMORY);
      return FAIL;
		}

		// for easy access		
		pData = (MergeTaskData*)pConfig->pTaskData;
		pData->pCurPacket = NULL;

    // remember how many bytes to store per incoming packet
    pData->uiBytesPerPacket = (pConfig->configLength == 2 ) ? pConfig->configData[1] : 1;
    pData->uiCount = 0;
    
		dbg( "TitanTask", "New task with ID: %i: Merge (%i inputs with %i bytes)\n", pConfig->taskID, pConfig->inPorts, pData->uiBytesPerPacket );

    if ( pData->uiBytesPerPacket * pConfig->inPorts > TITAN_PACKET_SIZE ) {
      call Titan.issueError(ERROR_CONFIG);
      dbg( "TitanTask", "Task Merge: too large merged packet size\n" );
      return FAIL;
    }
	
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
	
		////////////////////////////////////////////////////////////////////////
		// Application dependent code
	
		return result;
	}
	
	/**
	* Starts the operation of a task. This could also wake it up again.
	*/
	event error_t Titan.start( TitanTaskConfig* pConfig ) {
    MergeTaskData *pData  = (MergeTaskData*)pConfig->pTaskData;
    pData->pCurPacket = call Titan.allocPacket( pConfig->taskID, 0 );

    dbg("TitanTask", "Starting Merge task\n");

    return SUCCESS;
	}
	
	/**
	* Stops the operation of a task. It should then prepare a sleep mode.
	*/
	event error_t Titan.stop( TitanTaskConfig* pConfig ) {
		return SUCCESS;
	}
	
	
	void sendPacket( uint8_t taskID, MergeTaskData* pData, uint8_t ports, uint8_t type ) {
	    pData->pCurPacket->length = pData->uiBytesPerPacket*ports;
	    pData->pCurPacket->type = type;
	    call Titan.sendPacket( taskID, 0, pData->pCurPacket );
	    pData->pCurPacket = call Titan.allocPacket( taskID, 0 );
	    if ( pData->pCurPacket == 0)  {
        call Titan.issueError(ERROR_OUT_FIFO_FULL);
	      dbg( "TitanTask", "Task Merge: ERROR: could not allocate new packet!!!\n" );
	    }
	}
	
	////////////////////////////////////////////////////////////////////////////
	// This is the actual working thread	
	event void Titan.execute( TitanTaskConfig* pConfig, uint8_t iPort ) {

		uint8_t i;
		TitanPacket *pPacketIn;
		MergeTaskData *pData;
		
		if ( pConfig == NULL ) {
      dbg( "TitanTask", "Task Merge: Could not get context\n");
      return;
    }
		
		pData  = (MergeTaskData*)pConfig->pTaskData;
		
		////////////////////////////////////////////////////////////////////////
		// Application dependent code

		// get input packet
		pPacketIn = call Titan.getNextPacket( pConfig->taskID, iPort );

		// check whether packet is too small
    if ( pPacketIn->length < pData->uiBytesPerPacket ) {
      call Titan.issueError(ERROR_CONFIG);
      dbg( "TitanTask", "Task Merge: Packet too short\n",pPacketIn->length, pPacketIn->type);
      pData->uiBytesPerPacket = pPacketIn->length;
    }
    
    // check whether we have an outgoing packet
    if ( pData->pCurPacket == NULL ) {
      pData->pCurPacket = call Titan.allocPacket( pConfig->taskID, 0 );
      if (pData->pCurPacket == NULL) {
        call Titan.issueError(ERROR_OUT_FIFO_FULL);
        dbg( "TitanTask", "Task Merge: could not get an outgoing packet.\n" );
        return;
      }
    }
    
    // copy all data to copy
    {
      uint16_t offset = iPort*pData->uiBytesPerPacket;
      dbg( "TitanTask", "Task Merge: Copying %i data bytes from port %i/%i\n", pData->uiBytesPerPacket,iPort, pPacketIn->type);
      for ( i=0; i<pData->uiBytesPerPacket; i++ ) {
        pData->pCurPacket->data[i+offset] = pPacketIn->data[i];
      }
    }

    // count number of inputs got
    pData->uiCount++;

		// all sources served? -> send packet
		if ( pData->uiCount >= pConfig->inPorts ) {
			dbg( "TitanTask", "Task Merge: Sent packet due to full count (%i)\n",pConfig->inPorts);
			sendPacket( pConfig->taskID, pData, pConfig->inPorts, pPacketIn->type );
			pData->uiCount = 0;
		}
    call Titan.postExecution(pConfig,iPort);
	}

	/**
	* Is issued when a new packet arrives at the input port.
	* @param  iPort Port where the packet arrives
	* @return SUCCESS if the packet will be processed
	*/
	async event error_t Titan.packetAvailable( TitanTaskConfig* pConfig, uint8_t uiPort ) {

    dbg("TitanTask", "Merge: received packet on port %i\n", uiPort);

    // start the working task
    call Titan.postExecution( pConfig, uiPort );

    return SUCCESS;
	}

}
