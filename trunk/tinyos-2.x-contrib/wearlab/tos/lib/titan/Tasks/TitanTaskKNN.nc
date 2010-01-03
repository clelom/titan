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
  * TitanTaskKNN.nc
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  *
  * Implementation of a k-NN algorithm for Titan. It is configurable in terms of 
  * dimensions (feature inputs) and k (number of neighbors) chosen.
  *
  * Configuration: 
  * [ k n fs_size options ] [data]*
  * After the first header, the rest of the data is interpreted as tupels of features. Tupels 
  * always must be complete in a package.
  * 
  */

module TitanTaskKNN {
	uses interface Titan;
}

implementation {

	// the data used by the task should always be 
	// organized in a structure.
	typedef struct kNNTaskData {
		uint8_t  k; // number of neighbors kept for the majority vote
    uint8_t  n; // dimensionality: number of features taken into consideration
    uint16_t fs_size; // feature space size (number of classified examples)
    uint16_t fs_received; // number of samples received up to now
    uint8_t  options; // 2 LSB:  00: 8 bit unsigned, 01: 16 bit unsigned, 10 8 bit signed, 11 16 bit signed
    uint16_t inputs_valid; // keeps track of how many inputs have new values
    uint8_t data[0];
	} kNNTaskData;

#define MAX_K 10
  
  event error_t Titan.init() {
    return SUCCESS;
  }

	/**
	* Returns the universal task identifier of the task. These identifiers are 
	* stored in TitanTaskUIDs.h and should be statically programmed.
	* @return Universal Task Identifier
	*/
	event TitanTaskUID Titan.getTaskUID() {
		return TITAN_TASK_KNN;
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

		kNNTaskData* pData;
    uint8_t* pTupels = pConfig->configData;
    int findex,tupelsize;

    #ifdef TOSSIM
    // char strConfig[4096];
    // strConfig[0]=0;
    // for(findex=0;findex<pConfig->configLength;findex++) {
      // char strTemp[64];
      // sprintf(strTemp,"%3i, ", pConfig->configData[findex]);
      // strcat(strConfig,strTemp);
    // }
    // dbg("TitanTaskKNN", "Received configuration packet with %i bytes: %s\n", pConfig->configLength, strConfig);
    #endif

      // check whether this is the first configuration packet
    if ( pConfig->pTaskData == NULL ) {
    
      // check whether the header is complete
      if ( pConfig->configLength < 5 ) {
        call Titan.issueError(ERROR_CONFIG);
        dbg("TitanTaskKNN", "ERROR: Invalid first configuration message!\n");
        return FAIL;
      } else {
      
        // extract header info
        uint16_t k = (uint16_t)pConfig->configData[0];
        uint16_t n = (uint16_t)pConfig->configData[1];
        uint16_t fs_size = ((uint16_t)pConfig->configData[2]<<8)|(uint16_t)pConfig->configData[3];
        uint8_t options = pConfig->configData[4];
        uint16_t sample_memory_size;

        if ( k > MAX_K ) {
          call Titan.issueError(ERROR_CONFIG);
          dbg("TitanTaskKNN","k-NN task: k is too big\n");
          return FAIL;
        }
        
        // allocate memory needed
        sample_memory_size = (fs_size+1)*(n*((options&0x1)+1)+1); // n features (16/8bit) + 1 class byte (last sample for current feature)
        pConfig->pTaskData = call Titan.allocMemory(pConfig->taskID,sizeof(kNNTaskData)+sample_memory_size);
    		if ( pConfig->pTaskData == NULL ) {
    			call Titan.issueError(ERROR_NO_MEMORY);
          dbg("TitanTaskKNN", "ERROR: Not enough memory!\n");
          return FAIL;
    		}
        
        // save configuration information
        pData = (kNNTaskData*)pConfig->pTaskData;
        pData->k = k;
        pData->n = n;
        pData->fs_size = fs_size;
        pData->fs_received = 0;
        pData->inputs_valid = 0;
        pData->options = options;
        
        pTupels = pConfig->configData+5;
        
      } // end valid config
      
    } // end first packet
    else {
      pData = (kNNTaskData*)pConfig->pTaskData;
    }
    
    
    // copy the feature data
    tupelsize = pData->n*((pData->options&0x1)+1)+1; // + 1 class byte
    findex = pData->fs_received*tupelsize;
    while ( pTupels + tupelsize - pConfig->configData <= pConfig->configLength) {
      int i;
      
      // increment the number of tupels received
      pData->fs_received++;
      
      // make sure we're not overshooting
      if ( pData->fs_received > pData->fs_size ) {
        dbg("TitanTaskKNN", "Too many k-NN samples!\n");
        call Titan.issueError( ERROR_CONFIG );
        break;
      }

      // copy tupel data
      for(i=0; i < tupelsize; i++ ) {
        pData->data[ findex++ ] = *(pTupels++);
      }
    }

		// define number of i/o-ports		
		pConfig->inPorts  = pData->n; // equal to the number of features
		pConfig->outPorts = 1;

	
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
  
    // make sure we have the complete configuration
    kNNTaskData* pData = (kNNTaskData*)pConfig->pTaskData;
    
    if ( pData->fs_size != pData->fs_received ) {
      call Titan.issueError(ERROR_CONFIG);
      dbg("TitanTaskKNN","ERROR: Did not receive all samples\n");
      return FAIL;
    }
    
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

		kNNTaskData *pData;
		
		uint16_t decision = -1;

		uint16_t distances[MAX_K];
		uint8_t  classification[MAX_K];
		int i;
		int tupelsize;
    
		if ( pConfig == NULL ) {
			dbg("TitanTask","WARNING: Got no context in task KNN\n");
			return;
		}
		
		pData  = (kNNTaskData*)pConfig->pTaskData;
    
		tupelsize = pData->n*((pData->options&0x1)+1)+1; // + 1 class byte
    
    // initialize
    for ( i=0; i<pData->k; i++ ) {
      distances[i] = -1;
    }
    
    // check bit width
    if (pData->options & 0x1) { // 16 bit
      int j;
      uint8_t*  curTupel = pData->data;
      uint16_t* pPoint = (uint16_t*)&(pData->data[pData->fs_size*tupelsize]);
      
      // go through the whole feature space
      for ( i=0; i<pData->fs_size; i++ ) {
        uint16_t* curSample = (uint16_t*)curTupel;
        uint16_t distance = 0;
        

        // compute distance
        for ( j=0; j<pData->n; j++ ) {
          distance += (pPoint[j] > curSample[j])? pPoint[j] - curSample[j] : curSample[j] - pPoint[j];
        }

        // check whether it is one of the k best points (note the best point is at the highest index)
        for ( j=0; (j < pData->k) && (distance < distances[j]); j++ ) {
          if ( j!=0 ) { distances[j-1] = distances[j]; classification[j-1] = classification[j]; } //shift worse value
        }
        
        // insert the new point if it has some value
        if ( j > 0 ) {
          distances[j-1] = distance;
          classification[j-1] = *(curTupel+tupelsize-1);
          dbg("TitanTaskKNN","Entering at position %i\n", j);
        }
        
        curTupel += tupelsize;
      } // iterate through feature space
      
    } else { // 8 bit
      dbg("TitanTaskKNN","8 bit k-NN not implemented!\n");
      call Titan.issueError(ERROR_NOT_IMPLEMENTED);
      return;
    }
    
    // now compute the majority value
    {
      uint16_t maxClassifications = 0;
      
      // for every entry of the classification aray, search how many of the 
      // following have the same class, and sum it up
      for (i=0; i < pData->k; i++ ) {
        uint16_t curClassifications = 1,j;
        for (j=i+1; j<pData->k; j++ ) {
          if ( classification[i] == classification[j] ) curClassifications++;
        }
        // if there is a new max, store the decision
        if ( curClassifications >= maxClassifications ) {
          maxClassifications = curClassifications;
          decision = classification[i];
        }
      }
      dbg("TitanTaskKNN","Classification result: %i (majority of %i classes)\n", decision, maxClassifications);
    }
    

    { // generate output
  		TitanPacket *pPacketOut = call Titan.allocPacket( pConfig->taskID, 0 );
      
      pPacketOut->length = 1;
      pPacketOut->type = TT_UINT8;
      pPacketOut->data[0] = (uint8_t)decision;
      
  		call Titan.sendPacket( pConfig->taskID, 0, pPacketOut );
    }

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

    kNNTaskData* pData = (kNNTaskData*)pConfig->pTaskData;
    int tupelsize = pData->n*((pData->options&0x1)+1)+1; // + 1 class byte
		TitanPacket *pPacketIn = call Titan.getNextPacket( pConfig->taskID, uiPort );

    dbg("TitanTaskKNN","Packet received on port %i tupel=%i\n", uiPort,tupelsize);
    
    if ( (((pData->options&0x01) != 1) && (( pPacketIn->type == TT_UINT16 ) || ( pPacketIn->type == TT_INT16 ))) ||
         (((pData->options&0x01) != 0) && (( pPacketIn->type == TT_UINT8 ) || ( pPacketIn->type == TT_INT8 )))
        ) {
      call Titan.issueError(ERROR_TYPE);
      dbg("TitanTaskKNN","Task k-NN: Invalid input: format\n");
      return FAIL;
    } else if ( (((pData->options&0x01) == 1) && pPacketIn->length != 2) ||
                (((pData->options&0x01) == 0) && pPacketIn->length != 1)  ) {
      call Titan.issueError(ERROR_TYPE);
      dbg("TitanTaskKNN","Task k-NN: Invalid input: size (%i)\n",pPacketIn->length);
      return FAIL;
    }
    
    // save new feature value
    if ( pData->options & 0x1 ) {
      // 16 bit
      uint16_t* pSave=(uint16_t*)&(pData->data[pData->fs_size*tupelsize + (uiPort<<1)]);
      *pSave = *(uint16_t*)pPacketIn->data;
      dbg("TitanTaskKNN", "Saved input %i: %i, saved at 0x%x\n", uiPort, *((uint16_t*)pPacketIn->data), pSave);
    } else {
      // 8 bit
      pData->data[pData->fs_size*tupelsize + uiPort*((pData->options&0x1)+1)] = pPacketIn->data[0];
    }
    
    // signal new feature values
    pData->inputs_valid |= 0x1<<uiPort;

    // check whether all features have new values
    if ( pData->inputs_valid == (0x1<<pData->n)-1 ) {
    
      dbg("TitanTaskKNN","All inputs have new values - starting classification\n");
      
      // start classifier
      call Titan.postExecution(pConfig,uiPort);
      
      // reset input values
      pData->inputs_valid = 0;
    }
    
		return SUCCESS;
	}

}
