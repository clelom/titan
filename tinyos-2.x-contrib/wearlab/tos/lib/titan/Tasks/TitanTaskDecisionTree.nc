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
  * TitanTaskDecisionTree.nc
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  *
  * This task implements a binary decision tree classifier.
  *
  * Configuration message format:
  *
  * 1st message:
  * [ msgNodes|0x80 totalNodes numFeatures [NODEMSG] ]
  *
  * subsequent messages:
  * [ msgNodes [NODEMSG] ]
  *
  * NODEMSG:
  * [ OPERATOR value ]*msgNodes
  *
  * OPERATOR being defined by TITAN_DECTREE_OPERATOR
  *
  *
  */

module TitanTaskDecisionTree {
	uses interface Titan;
}

implementation {

  typedef enum TITAN_DECTREE_OPERATOR { TDO_SMALLER_THAN=0x00, TDO_LARGER_THAN=0x10, TDO_EQUAL=0x20, TDO_SETCLASS=0x30 } TITAN_DECTREE_OPERATOR;

  typedef struct DecTreeNode {
    TITAN_DECTREE_OPERATOR nodeOperator;  // indicates the operators (upper four bits: operator, lower four bits: feature)
    uint16_t     value;          // parameters for the comparison
    void*        (pChildren[2]);   // link to children, cast to DecTreeNode
  } DecTreeNode;

	// the data used by the task should always be 
	// organized in a structure.
	typedef struct DecTreeData {
    DecTreeNode *pRootNode;
    uint16_t    *pFeatures;
    uint8_t     featureNum;
    uint16_t    inputs_valid;
    
    // config time
    DecTreeNode *pCurConfigNode;
    uint8_t     totalNodes;
	} DecTreeData;

  event error_t Titan.init() {
    return SUCCESS;
  }

	/**
	* Returns the universal task identifier of the task. These identifiers are 
	* stored in TitanTaskUIDs.h and should be statically programmed.
	* @return Universal Task Identifier
	*/
	event TitanTaskUID Titan.getTaskUID() {
		return TITAN_TASK_DECISIONTREE;
	}
  
  /**
   * Recursively goes through a level of the tree and counts the number of nodes stored on 
   * it. 
   * @param  curChildren Child nodes of the nodes on the same level found up to now
   * @param  pCurNode    The node handled at this iteration step
   * @return The number nodes following in the current line. -1 if the tree is somehow not correct
   */
  typedef struct constructTree_t {
    uint16_t levelNodes;
    uint16_t levelChildren;
  } constructTree_t;
  
  constructTree_t configConstructTree( DecTreeNode *pCurNode, uint16_t prevChildren, uint16_t prevLevelNodes, uint16_t totalLevelNodes ) {
    uint16_t localChildren = (pCurNode->nodeOperator == TDO_SETCLASS)? 0:2;
    constructTree_t result;
    
    if (prevLevelNodes >= totalLevelNodes) {
      constructTree_t endresult = {0,0};
      return endresult;
    } 
    
    #ifdef TOSSIM
      // debug output
      dbg("TitanTaskDT", "new node: prevChildren=%i, prevLevelNodes=%i, totalLevelNodes=%i\n",
            prevChildren,prevLevelNodes,totalLevelNodes);
    #endif
    
    result = configConstructTree(  pCurNode+1, prevChildren + localChildren, prevLevelNodes+1, totalLevelNodes );

    // set links if not end node
    if ( localChildren != 0 ) {
      pCurNode->pChildren[0] = pCurNode + result.levelNodes + prevChildren + 1;
      pCurNode->pChildren[1] = pCurNode + result.levelNodes + prevChildren + 2;
      result.levelChildren += localChildren;
    } else {
      pCurNode->pChildren[0] = pCurNode->pChildren[1] = NULL;
    }
    
    #ifdef TOSSIM
      // debug output
      dbg("TitanTaskDT", "Node operator=%i, children=(+%i,+%i) -> levelNodes=%i,levelChildren=%i\n",
            pCurNode->nodeOperator,
            (DecTreeNode*)(pCurNode->pChildren[0]) - pCurNode, 
            (DecTreeNode*)(pCurNode->pChildren[1]) - pCurNode,
            result.levelNodes,
            result.levelChildren );
    #endif

    result.levelNodes+=1;
    
    return result;
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
		DecTreeData* pData;
    DecTreeNode* pCfgNode=NULL;
    uint8_t*     pMsgNode=NULL;
    uint16_t i;
    uint8_t msgNodes;

    if ( pConfig->configLength < 3 ) {
      call Titan.issueError(ERROR_CONFIG);
      dbg("TitanTaskDT", "Invalid configuration message received\n");
      return FAIL;
    }
    
    // check whether this is the first message
    if ( (pConfig->configData[0] & 0x80) != 0 ) {
      uint16_t totalNodes  = pConfig->configData[1];
      uint16_t numFeatures = pConfig->configData[2];
      uint16_t configSize = sizeof(DecTreeData) + totalNodes*sizeof(DecTreeNode) + numFeatures*sizeof(uint16_t);

  		pConfig->inPorts  = numFeatures;
  		pConfig->outPorts = 1;
      
      dbg("TitanTaskDT", "Received tree configuration with %i nodes, %i features\n", totalNodes, numFeatures);

      pConfig->pTaskData = call Titan.allocMemory(pConfig->taskID,configSize);
      if ( pConfig->pTaskData == NULL ) {
        dbg("TitanTaskDT", "No memory\n");
        call Titan.issueError(ERROR_NO_MEMORY);
        return FAIL;
      }
      
      // fill up task data structure
      pData = (DecTreeData*)pConfig->pTaskData;
      pData->pRootNode = (DecTreeNode*)(pData+1);
      pData->pFeatures = (uint16_t*)(pData->pRootNode+totalNodes);
      pData->featureNum = numFeatures;
      pData->inputs_valid = 0;
      pData->pCurConfigNode = pData->pRootNode;
      pData->totalNodes = totalNodes;
      
      // set config values
      pCfgNode = pData->pRootNode;
      pMsgNode = &(pConfig->configData[3]);

    } else if ( pConfig->pTaskData == NULL ) { // first message must have arrived before
      call Titan.issueError(ERROR_CONFIG);
      dbg("TitanTaskDT", "Missing first configuration message.\n");
      return FAIL;
    } else {
      pData = (DecTreeData*)pConfig->pTaskData;
      pCfgNode = pData->pRootNode;
      pMsgNode = &(pConfig->configData[1]);
    }
    msgNodes   = pConfig->configData[0]&0x7F;
    
    dbg("TitanTaskDT", "Processing configuration message with %i tree nodes\n", msgNodes );

    // write data into the structures (just copy operators)
    for (i=0; i<msgNodes; i++) {
      if ( pMsgNode - pConfig->configData > pConfig->configLength ) {
        call Titan.issueError(ERROR_CONFIG);
        dbg("TitanTaskDT", "Not as many nodes in msg as indicated.\n");
        return FAIL;
      }
      
      pData->pCurConfigNode->nodeOperator  = *pMsgNode;
      pData->pCurConfigNode->value         = (*(pMsgNode+1))<<8;
      pData->pCurConfigNode->value        |= (*(pMsgNode+2))&0x00FF;
      pData->pCurConfigNode++;
      pMsgNode += 3;
    }
    
    // check whether all nodes have been passed - construct tree
    if ( pData->pCurConfigNode - pData->pRootNode == pData->totalNodes ) {
      constructTree_t levelNodes = {0,1};
      uint16_t iPrevLevelNodes = 0;
      dbg("TitanTaskDT", "Tree data complete. Constructing tree\n");
    
      pData->pCurConfigNode = pData->pRootNode;
      
      // go through each level and stop when there are no more leave nodes
      while ( (levelNodes = configConstructTree( pData->pCurConfigNode, 0, 0, levelNodes.levelChildren) ).levelChildren != 0 ) {
      
        dbg("TitanTaskDT", "Level has %i nodes and %i children\n", levelNodes.levelNodes, levelNodes.levelChildren);
      
        iPrevLevelNodes += levelNodes.levelNodes;
        pData->pCurConfigNode  += levelNodes.levelNodes;
      
      }
      
      // check whether construction was successful -> it must include all nodes
      if (iPrevLevelNodes+levelNodes.levelNodes != pData->totalNodes) {
        call Titan.issueError(ERROR_CONFIG);
        dbg("TitanTaskDT", "Tree construction failed.\n");
        pData->pRootNode = NULL;
        return FAIL;
      }

      
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
    dbg("TitanTask", "Starting TitanTaskDecisionTree\n");
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
  
  /**
   *
   * Go through the tree
   * @param pCurNode Current position in the tree
   * @return The class identifier computed
   */
   
  uint16_t traverseTree( DecTreeNode* pCurNode, uint16_t features[] ) {
  
    // leave reached?
    if ( pCurNode->nodeOperator == TDO_SETCLASS ) { 
      dbg("TitanTaskDT","Setting class %i\n", pCurNode->value);
      return pCurNode->value;
    }
    
    // continue through the tree
    switch( pCurNode->nodeOperator&0xF0 ) {
      case TDO_SMALLER_THAN:
        dbg("TitanTaskDT","  Testing %u < %u.\n", features[pCurNode->nodeOperator&0x0F], pCurNode->value  );
        return (features[pCurNode->nodeOperator&0x0F] < pCurNode->value) ? 
               traverseTree( pCurNode->pChildren[0], features) : 
               traverseTree( pCurNode->pChildren[1], features);
        break;
      case TDO_LARGER_THAN:
        dbg("TitanTaskDT","  Testing %u > %u.\n", features[pCurNode->nodeOperator&0x0F], pCurNode->value  );
        return (features[pCurNode->nodeOperator&0x0F] > pCurNode->value) ? 
                traverseTree( pCurNode->pChildren[0], features) : 
                traverseTree( pCurNode->pChildren[1], features);
        break;
      case TDO_EQUAL:
        dbg("TitanTaskDT","  Testing %u == %u.\n", features[pCurNode->nodeOperator&0x0F], pCurNode->value  );
        return (features[pCurNode->nodeOperator&0x0F] == pCurNode->value) ? 
                traverseTree( pCurNode->pChildren[0], features) : 
                traverseTree( pCurNode->pChildren[1], features);
        break;
      default:
        dbg("TitanTaskDT","Encountered invalid operator: %u\n", pCurNode->nodeOperator&0xF0);
    }
     
    return -1;
  }
  
	event void Titan.execute( TitanTaskConfig* pConfig, uint8_t uiPortID ) {

    uint16_t resultClass;
		DecTreeData *pData  = (DecTreeData*)pConfig->pTaskData;
    TitanPacket *pPacketOut = NULL;
    
    if ( pConfig == NULL ) {
      call Titan.issueError(ERROR_NO_CONTEXT);
      return;
    }
    

    // get the class
    resultClass = traverseTree( pData->pRootNode, pData->pFeatures );
    
    // get packet
    pPacketOut= call Titan.allocPacket(pConfig->taskID, 0 );
    if ( pPacketOut == NULL ) {
      dbg("TitanTaskDT","Packet out queue full!\n");
      return;
    }

    // fill data into packet
    pPacketOut->type   = TT_UINT16;
    pPacketOut->length = sizeof(uint16_t);

    *((uint16_t*)pPacketOut->data) = resultClass;
    
		call Titan.sendPacket( pConfig->taskID, 0, pPacketOut );
    
//    P2OUT &= ~BIT0;

	}

	/**
	* Is issued when a new packet arrives at the input port.
	* @param  iPort Port where the packet arrives
	* @return SUCCESS if the packet will be processed
	*/
	async event error_t Titan.packetAvailable( TitanTaskConfig* pConfig, uint8_t uiPort ) {
		DecTreeData *pData  = (DecTreeData*)pConfig->pTaskData;
		TitanPacket *pPacketIn = call Titan.getNextPacket( pConfig->taskID, uiPort );
    
    if ( pPacketIn == NULL ) {
      call Titan.issueError(ERROR_PACKET);
      return FAIL;
    }

    if ( ((pPacketIn->type== TT_UINT16) && (pPacketIn->length != 2 )) || 
         ((pPacketIn->type== TT_UINT8 ) && (pPacketIn->length != 1 )) 
       ) {
       dbg("TitanTaskDT", "Unknown packet format at input port %i (type=%i)\n", uiPort, pPacketIn->type);
       call Titan.issueError(ERROR_TYPE);
       return FAIL;
    }
    
    // store feature input
    pData->pFeatures[uiPort] = (pPacketIn->type==TT_UINT16 || pPacketIn->type==TT_INT16 )? *(uint16_t*)pPacketIn->data : 
                                (pPacketIn->type==TT_UINT8  || pPacketIn->type==TT_INT8  )? *(uint8_t*) pPacketIn->data : 
                                -1;
    //debug dbg("TitanTaskDT","Feature %i: got new value %i, storing at 0x%X\n", uiPort, pData->pNextValue[uiPort], &(pData->pNextValue[uiPort]) );

    // check feature overwrite (2x write between classifications)
    if (pData->inputs_valid & (0x1<<uiPort) ) dbg("TitanTaskDT", "Warning: overwriting feature number %i (0x%x)\n",uiPort,pData->inputs_valid);

    // signal new feature values
    pData->inputs_valid |= 0x1<<uiPort;

    // check whether all features have new values
    if ( pData->inputs_valid == (0x1<<pData->featureNum)-1 ) {
    
      //debug dbg("TitanTaskDT","All inputs have new values - starting classification\n\n");
      
      // start classifier
      call Titan.postExecution(pConfig,uiPort);
      
      // reset input values
      pData->inputs_valid = 0;

    }
		return SUCCESS;

  }
}
