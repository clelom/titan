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
  * TitanTaskSimilarity.nc
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>, Thomas Meier, Oliver Amft, Martin Kusserow
  *
  * This task describes a similarity search algorithm.
  */

module TitanTaskSimilaritySearch {
	uses interface Titan;
}

implementation {

  #define SS_FEATURE_SUM 1
  #define SS_FEATURE_AVG 2
  #define SS_FEATURE_MAX 3
  #define SS_FEATURE_MIN 4
  #define INT16MAX 32767
  

  typedef struct simsearch_feature {
    uint8_t addrule; // defines how segments can be added up
    uint8_t port;  // port where the feature data comes in
    uint16_t mean; // mean on this feature in feature space
    uint16_t stddev_inv; // inversed standard deviation on the feature
  }simsearch_feature;
  
  typedef struct simsearch_class {
    uint8_t classID;       // identifier for the class
    uint8_t minlookback;   // minimum depth of the history
    uint8_t maxlookback;   // maximum depth of the history - search goes in segments from min to max
    uint16_t threshold;    // the threshold for acceptance of the class
    uint16_t FeatureFlags; // enables features for this class-> feature 0 on -> LSB = 1
  }simsearch_class;

	// the data used by the task should always be 
	// organized in a structure.
	typedef struct SimSearchData {
    uint8_t num_features;
    uint8_t num_classes;
    simsearch_feature* pFeatures;
    simsearch_class*   pClasses;
    uint16_t*          pBuffer;
    uint16_t*          pNextValue;
    uint8_t            buffer_depth;
    uint16_t inputs_valid; // keeps track on which ports have delivered data since last classification
	} SimSearchData;

  event error_t Titan.init() {
    return SUCCESS;
  }

	/**
	* Returns the universal task identifier of the task. These identifiers are 
	* stored in TitanTaskUIDs.h and should be statically programmed.
	* @return Universal Task Identifier
	*/
	event TitanTaskUID Titan.getTaskUID() {
		return TITAN_TASK_SIMILARITY;
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
		SimSearchData* pData;
    uint8_t num_features=0,num_classes=0,msg_features,msg_classes,buffer_depth=0;
    uint8_t* pCfgData=NULL;
    uint16_t i;

    if ( pConfig->configLength < 5 ) {
      call Titan.issueError(ERROR_CONFIG);
      dbg("TitanTaskSS", "Invalid configuration message received\n");
      return FAIL;
    }
    
    // check what message type it is
    switch( pConfig->configData[0] & 0xF0 ) {
      case 0x10: // first config message
        num_features = pConfig->configData[1];
        num_classes  = pConfig->configData[2];
        buffer_depth = pConfig->configData[3];
        msg_features = (pConfig->configData[4]>>4)&0x0F;
        msg_classes  = (pConfig->configData[4])&0x0F;
        pCfgData = &(pConfig->configData[5]);
        break;
      case 0x20: // feature/class configuration message
        msg_features = (pConfig->configData[1]>>4)&0x0F;
        msg_classes  = (pConfig->configData[1])&0x0F;
        pCfgData = &(pConfig->configData[2]);
        break;
      default:
        call Titan.issueError(ERROR_CONFIG);
        dbg("TitanTaskSS", "Unknown configuration message received\n");
        return FAIL;
    }
    
    dbg("TitanTaskSS", "Processing configuration message with %i features, %i tasks\n",msg_features, msg_classes );
    
    // allocate memory for the classification (this should usually be called on the first message)
    if ( pConfig->pTaskData == NULL ) {
    
      uint16_t configSize,bufferSize;
      
      if ( num_features == 0 ) {
        dbg("TitanTaskSS", "No feature number\n");
        return FAIL;
      }

      configSize = sizeof(SimSearchData)+num_features*sizeof(simsearch_feature)+num_classes*sizeof(simsearch_class);
      bufferSize = num_features*sizeof(uint16_t)*buffer_depth;
      
      pConfig->pTaskData = call Titan.allocMemory(pConfig->taskID,configSize+bufferSize);
  		if ( pConfig->pTaskData == NULL ) {
        dbg("TitanTaskSS", "No memory\n");
  			call Titan.issueError(ERROR_NO_MEMORY);
        return FAIL;
  		}

  		// define number of i/o-ports		
  		pConfig->inPorts  = num_features;
  		pConfig->outPorts = 1;
      
      // initialize data
      pData = (SimSearchData*)pConfig->pTaskData;
      pData->num_features = 0;
      pData->num_classes  = 0;
      pData->pFeatures = (simsearch_feature*)(pData+1);
      pData->pClasses  = (simsearch_class*)(pData->pFeatures+num_features);
      pData->pBuffer   = (uint16_t*)(pData->pClasses+num_classes);
      pData->pNextValue   = pData->pBuffer;
      pData->buffer_depth = buffer_depth;
      
    } else { // pConfig->pTaskData != NULL
      // reconstruct data number
      //TODO: THIS IS *UNSAFE*: SOME CONFIGURATIONS ARE NOT ACCEPTED LATER ON...
      pData = (SimSearchData*)pConfig->pTaskData;
      num_classes   = ((uint8_t*)pData->pBuffer  - (uint8_t*)pData->pClasses )/sizeof(simsearch_class);
      num_features  = ((uint8_t*)pData->pClasses - (uint8_t*)pData->pFeatures)/sizeof(simsearch_feature);
    }
    
    dbg("TitanTaskSS", "Features: %i/%i, Classes: %i/%i\n",pData->num_features + msg_features, num_features, pData->num_classes + msg_classes, num_classes );
    
    // check against overflows
    if ( pData->num_features + msg_features > num_features ) {
      dbg("TitanTaskSS", "Too many features received\n");
      call Titan.issueError(ERROR_CONFIG);
      return FAIL;
    }
    if ( pData->num_classes + msg_classes > num_classes ) {
      dbg("TitanTaskSS", "Too many classes received\n");
      call Titan.issueError(ERROR_CONFIG);
      return FAIL;
    }
    
    // first part of the message are features
    for (i=0; i < msg_features; i++) {
      pData->pFeatures[pData->num_features].addrule = *(pCfgData++);
      pData->pFeatures[pData->num_features].port    = *(pCfgData++);
      pData->pFeatures[pData->num_features].mean    = (*(pCfgData++)<<8)&0xFF00;
      pData->pFeatures[pData->num_features].mean   |= (*(pCfgData++)   )&0x00FF;
      pData->pFeatures[pData->num_features].stddev_inv    = (*(pCfgData++)<<8)&0xFF00;
      pData->pFeatures[pData->num_features].stddev_inv   |= (*(pCfgData++)   )&0x00FF;
      pData->num_features++;
    }
    
    // second part of the message are classes
    for (i=0; i < msg_classes; i++) {
      pData->pClasses[pData->num_classes].classID        = *(pCfgData++);
      pData->pClasses[pData->num_classes].minlookback    = *(pCfgData++);
      pData->pClasses[pData->num_classes].maxlookback    = *(pCfgData++);
      pData->pClasses[pData->num_classes].threshold      = (*(pCfgData++)<<8)&0xFF00;
      pData->pClasses[pData->num_classes].threshold     |= (*(pCfgData++)   )&0x00FF;
      pData->pClasses[pData->num_classes].FeatureFlags   = (*(pCfgData++)<<8)&0xFF00;
      pData->pClasses[pData->num_classes].FeatureFlags  |= (*(pCfgData++)   )&0x00FF;
      pData->num_classes++;
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
  
  inline uint16_t calc_distance(uint16_t value, simsearch_feature* pFeature) {
    int16_t t;
  
    value -= pFeature->mean;
    t = value*pFeature->stddev_inv;
    
    // transformation
/*    t += 0x0080;
    t >>= 8;
    t *= t;
    t += 0x80;
    
    t>>=8;
  
    if (t>INT16MAX) t=INT16MAX;
*/
    return (int16_t)t; // return positive and negative values
  }
  
  
  inline uint16_t getfeature( uint16_t nFeature, uint16_t nLookback, SimSearchData* pData ) {
    uint16_t i;
    uint16_t tot_value=0;
    int16_t iCurPos;
    uint16_t* pCurValue;
    
    iCurPos = (pData->pNextValue - pData->pBuffer)/pData->num_features;
    pCurValue = pData->pNextValue;
    
    // add up features
    for (i=0; i < nLookback; i++) {
    
      // get back a step
      iCurPos--;
      pCurValue -= pData->num_features;
      if ( iCurPos < 0 ) { 
        iCurPos += pData->buffer_depth;
        pCurValue += pData->buffer_depth*pData->num_features;
      }
      
      //debug
	  dbg("TitanTaskSS","  Feature %i, lookback %i/%i, value:%i at 0x%X\n",nFeature,i,nLookback,pCurValue[nFeature],pCurValue);


      // check how to add data
      switch(pData->pFeatures[nFeature].addrule) {
        case SS_FEATURE_SUM:
          tot_value += pCurValue[nFeature];
          break;
        case SS_FEATURE_AVG:
          tot_value += (pCurValue[nFeature])/nLookback;
          break;
        case SS_FEATURE_MAX:
          if ( tot_value < pCurValue[nFeature] ) tot_value = pCurValue[nFeature];
          break;
        case SS_FEATURE_MIN:
          if ( tot_value > pCurValue[nFeature] ) tot_value = pCurValue[nFeature];
          break;
        default:
          dbg("TitanTaskSS","Don't know how to add features<n");
      } // switch addrule
    
    } // foreach entry

    return tot_value;
  }
  
  
  /**
   * Goes through all classes and computes a likelihood, then selects the class 
   * with the highest quality and returns that label.
   *
   *
   */
  
  void similaritySearch(SimSearchData* pData, uint16_t* pClassifications) {
    uint16_t i,j,k;
    uint16_t dist1, bestdist, distsum=0;
	int16_t dist2;
    uint16_t disttemp;
  
  	for(i=0;i<pData->num_classes;i++) { 		//work on every single class
  		
      //debug
	  dbg("TitanTaskSS","Class %i: featureflag: %i\n",pData->pClasses[i].classID, pData->pClasses[i].FeatureFlags);
      
      bestdist=INT16MAX;
      for(j=pData->pClasses[i].minlookback; j<=pData->pClasses[i].maxlookback; j++) {
        disttemp=0;
        
        for(k=0;k<pData->num_features;k++) {			//all features
        
          if ( (pData->pClasses[i].FeatureFlags & (0x1<<k))  == 0 ) continue;

          dist1 = getfeature(k,j,pData);
          dist2=calc_distance(dist1,&(pData->pFeatures[k]));
          //debug
		  dbg("TitanTaskSS","  Class %i: lookback: %i, feature: %i dist1=%i dist2=%i disttemp=%i\n",pData->pClasses[i].classID, j,k,dist1,dist2,disttemp);
         
		// calculate the distance metric
		/* euclidean distance */
		//disttemp += dist2*dist2; //sum up distances of all classes
		
		/* manhattan distance */
		if (dist2 < 0)
			dist2 = (-1)*dist2;
        disttemp += dist2; //sum up distances of all classes
		
        }

        //debug
		dbg("TitanTaskSS","Class %i: lookback: %i, disttemp=%i\n",pData->pClasses[i].classID, j,disttemp);
        
//        if(disttemp > INT16MAX) disttemp=INT16MAX;		//if too big, we take max of our number format

        distsum=disttemp;

        if((distsum < bestdist) && (distsum <= pData->pClasses[i].threshold)) {	//only keep the best hit, and only below threshold
          bestdist=distsum;
        }
      } // for(j=classes

      dbg("TitanTaskSS","Class %i, bestdist=%i\n",pData->pClasses[i].classID,bestdist);
      pClassifications[i] = bestdist;
      
    } // for all classes

  }
  
  
  
  
	event void Titan.execute( TitanTaskConfig* pConfig, uint8_t uiPortID ) {

		SimSearchData *pData;
		TitanPacket *pPacketOut;
		
		if ( pConfig == NULL ) {
			dbg("TitanTask","WARNING: Got no context in task Similarity Search\n");
			return;
		}
		
		pData  = (SimSearchData*)pConfig->pTaskData;


    // get packet
    pPacketOut = call Titan.allocPacket(pConfig->taskID, 0 );
	
    if ( pPacketOut == NULL ) {
      dbg("TitanTaskSS","Packet out queue full!\n");
      return;
    }
    
//    P6OUT |=  BIT1;  // set on

    pPacketOut->type   = TT_UINT16;
    pPacketOut->length = pData->num_classes*sizeof(uint16_t);

    // perform similarity search
    similaritySearch(pData,(uint16_t*)pPacketOut->data);

	dbg("TitanTaskSS","PacketOut length: %i\n", pPacketOut->length);
	
	call Titan.sendPacket( pConfig->taskID, 0, pPacketOut );

//    P6OUT &= ~BIT1;  // set off

	// TinyOS 2.0 scheduler drops out
    if ( call Titan.hasPacket( pConfig->taskID, 0 ) > 0 ) {
        call Titan.postExecution( pConfig, 0 );
    }

	}

	/**
	* Is issued when a new packet arrives at the input port.
	* @param  iPort Port where the packet arrives
	* @return SUCCESS if the packet will be processed
	*/
	async event error_t Titan.packetAvailable( TitanTaskConfig* pConfig, uint8_t uiPort ) {
		SimSearchData *pData  = (SimSearchData*)pConfig->pTaskData;
		TitanPacket *pPacketIn = call Titan.getNextPacket( pConfig->taskID, uiPort );

	/* TODO: handle different formats (AB: problem is that type is not correctly transmitted from task to task) */
	/*
    if ( ((pPacketIn->type== TT_UINT16) && (pPacketIn->length != 2 )) || 
         ((pPacketIn->type== TT_UINT8 ) && (pPacketIn->length != 1 )) 
       ) {
       dbg("TitanTaskSS", "Unknown packet format at input port %i (type=%i) with length %i\n", uiPort, pPacketIn->type, pPacketIn->length);
       call Titan.issueError(ERROR_TYPE);
       return FAIL;
    }
	*/
	
    // store feature input
    pData->pNextValue[uiPort] =  *(uint16_t*)pPacketIn->data;
	
	/* TODO: handle different formats (AB: problem is that type is not correctly transmitted from task to task) */
	//(pPacketIn->type==TT_UINT16 || pPacketIn->type==TT_INT16 )? *(uint16_t*)pPacketIn->data : 
	//(pPacketIn->type==TT_UINT8  || pPacketIn->type==TT_INT8  )? *(uint8_t*) pPacketIn->data : 
	//-1;
	
    //debug
	dbg("TitanTaskSS","PacketIn: type = %i, length = %i, next value = %i\n", pPacketIn->type, pPacketIn->length, *(uint16_t*)pPacketIn->data );
	dbg("TitanTaskSS","Feature %i: got new value %i, storing at 0x%X\n", uiPort, pData->pNextValue[uiPort], &(pData->pNextValue[uiPort]) );

    // check feature overwrite (2x write between classifications)
    if (pData->inputs_valid & (0x1<<uiPort) ) dbg("TitanTaskSS", "Warning: overwriting feature number %i (0x%x)\n",uiPort,pData->inputs_valid);

    // signal new feature values
    pData->inputs_valid |= 0x1<<uiPort;

    // check whether all features have new values
    if ( pData->inputs_valid == (0x1<<pData->num_features)-1 ) {
    
      //debug
	  dbg("TitanTaskSS","All inputs have new values - starting classification\n\n");
      
      // start classifier
      call Titan.postExecution( pConfig, uiPort );
      
      // reset input values
      pData->inputs_valid = 0;

      // move data pointer value
      pData->pNextValue+=pData->num_features;
      if (pData->pNextValue >= pData->pBuffer + pData->buffer_depth*pData->num_features ) pData->pNextValue = pData->pBuffer;
    
    }
		return SUCCESS;

  }
}
