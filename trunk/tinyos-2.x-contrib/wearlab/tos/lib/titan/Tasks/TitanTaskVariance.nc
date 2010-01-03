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
  * TitanTaskVariance.nc
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  * @author Andreas Breitenmoser
  *
  * Computes the squared std. variance over a defined length of data.
  * All input is assumed to be uint16_t.
  * 
  * Configuration is two bytes:
  * [uiWindowSize, uiWindowShift, uiShiftResult]
  *
  * uiWindowSize specifies over how many data items the sliding window goes: length of sliding window = uiWindowSize (max. 256).
  * uiWindowShift sets the number of data items the sliding window is shifted before the next variance value is calculated. 
  * uiShiftResult shifts the resulting 32bit value to the right, such that it fits into a result of 16bit.
  * 
  * NOTE: The task uses two sums:
  *       SquareSum = sum x^2
  *       Sum       = sum x
  *       variance  = Sum^2/windowlength - SquareSum/windowlength
  *       Make sure that neither SquareSum^2 nor Sum^2 exceeds 32bit values!
  *       !!! Only the upper 16 bits of the variance are returned !!!
  *
  * Last modified: 19.02.08
  */

module TitanTaskVariance {
  uses interface Titan;
}

implementation {

  // the data used by the task should always be 
  // organized in a structure.
  typedef struct VarianceTaskData {
    uint32_t iSum; //int32_t iSum;
    uint32_t iSqSum; //int32_t iSqSum;
    uint8_t uiWindowSize;
    uint8_t uiWindowShift;
    uint8_t uiDataCounter;
    uint8_t uiResultShift;
    uint16_t *pCurEntry;
//    int16_t data[];
  } VarianceTaskData;
  
  event error_t Titan.init() {
    return SUCCESS;
  }

  /**
  * Returns the universal task identifier of the task. These identifiers are 
  * stored in TitanTaskUIDs.h and should be statically programmed.
  * @return Universal Task Identifier
  */
  event TitanTaskUID Titan.getTaskUID() {
    return TITAN_TASK_VARIANCE;
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
    VarianceTaskData* pData;
    uint8_t      uiWindowSize, uiWindowShift, i;
    uint16_t     *pFilterData;

    // define number of i/o-ports    
    pConfig->inPorts  = 1;
    pConfig->outPorts = 1;
    
    ////////////////////////////////////////////////////////////////////////
    // Application dependent code
    
    if ((pConfig->configLength != 2) && (pConfig->configLength!=3)) {
      dbg( "TitanTask", "Task Variance: Unknown configuration size\n");
      call Titan.issueError(ERROR_CONFIG);
      return FALSE;
    }
    
    uiWindowSize = pConfig->configData[0];
    uiWindowShift = pConfig->configData[1];
    
    // allocate memory for the data structure
    pConfig->pTaskData = call Titan.allocMemory(pConfig->taskID, uiWindowSize*sizeof(uint16_t)+sizeof(VarianceTaskData));
    if ( pConfig->pTaskData == NULL ) {
      call Titan.issueError(ERROR_NO_MEMORY);
    }
    pData = (VarianceTaskData*)pConfig->pTaskData;

    pData->iSum   = 0;
    pData->iSqSum = 0;
    pData->uiWindowSize = uiWindowSize;
    pData->uiWindowShift = uiWindowShift;
    pData->uiDataCounter = 1;
    pData->uiResultShift = (pConfig->configLength == 3)? pConfig->configData[2] : 0;
    
    pFilterData = pData->pCurEntry = (uint16_t*)(pData+1);
    
    // init data
    for (i=0;i<uiWindowSize;i++) {
      *(pFilterData++) = 0;
    }

    dbg( "TitanTask", "Task Variance: Configured with window size = %i, shift = %i\n",uiWindowSize,uiWindowShift); 
    return SUCCESS;
  }
  
  
  /**
  * Indicates that the task will be terminated. After this event has been 
  * received, the task should process no more data and free all resources 
  * allocated.
  * @return Whether the task has successfully been terminated.
  */
  event error_t Titan.terminate( TitanTaskConfig* pConfig ) {
    error_t result;

    result = call Titan.freeMemory(pConfig->taskID, pConfig->pTaskData);
  
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
  event void Titan.execute( TitanTaskConfig* pConfig, uint8_t uiPortID ) {

    // get context stuff
    VarianceTaskData *pData;
	  TitanPacket *pPacketIn;

	  uint16_t *pSource, *pBuffer;
    uint32_t iSum; //int32_t iSum;
    uint32_t iSqSum; //int32_t iSqSum;
    uint16_t uiWindowSize;
    uint16_t uiWindowShift;
    uint16_t uiDataCounter;
    int i, lengthIn, lengthOut;

  	if ( pConfig == NULL ) {
  		dbg("TitanTask","WARNING: Got no context in task Variance\n");
  		return;
  	}
	  atomic {
      pData  = (VarianceTaskData*)pConfig->pTaskData;
      
      // get in/out packets
      pPacketIn  = call Titan.getNextPacket( pConfig->taskID, 0 );

      if ( pPacketIn == NULL ) {
        call Titan.issueError(ERROR_PACKET);
        return;
      }

      uiWindowSize  = pData->uiWindowSize;
      uiWindowShift = pData->uiWindowShift;
      uiDataCounter = pData->uiDataCounter;

      pSource = (uint16_t*)pPacketIn->data;
      
      pBuffer = pData->pCurEntry;
      iSum    = pData->iSum;
      iSqSum  = pData->iSqSum;
      lengthIn  = pPacketIn->length>>1;
      lengthOut = 2;

      dbg( "TitanTask", "Task Variance: starting for %i entries...\n", lengthIn);
  	
      for ( i=0; i < lengthIn; i++ ) {
      
        // compute variance from squared sum and normal sum
        uint32_t uiSrc = ((uint32_t)(*pSource))&0xFFFF;
        uint32_t uiMult = uiSrc*uiSrc;
        uint32_t uiBuf  = ((uint32_t)(*pBuffer))&0xFFFF;
        uint32_t uiMultBuf = uiBuf*uiBuf;
        iSqSum  += uiMult - uiMultBuf;
        iSum    += uiSrc - uiBuf;
  	  
        dbg( "TitanTask", "Task Variance: buffer: %i mult: %i counter: %i\n", *pBuffer, uiMult,uiDataCounter );
  	  
        *pBuffer = uiSrc; // keep value we have been working with
  	  
        if (uiDataCounter >= uiWindowShift) {
          TitanPacket *pPacketOut;
          pPacketOut = call Titan.allocPacket( pConfig->taskID, 0 );

          // queue full?
          if ( pPacketOut == NULL ) {
            call Titan.issueError(ERROR_PACKET);
          } else {
            //uint16_t q = 0;

            // compute the resulting variance
            uint32_t result = (iSqSum - (iSum*iSum)/uiWindowSize)/uiWindowSize;
            
            // shift for accuracy and check for overflow
            result >>= pData->uiResultShift;
            if ( (result & 0xFFFF0000) != 0 ) {
              result = 0xFFFF;
            }

            *((uint16_t*)pPacketOut->data) = (result&0xFFFF);
            pPacketOut->length = 2;

            dbg( "TitanTask", "Task Variance: sending packet: +%3i => s =%4i qs =%4i => var =%i\n", *pSource, iSum, iSqSum, *((uint16_t*)pPacketOut->data));
      
            pPacketOut->type = TT_UINT16;
            call Titan.sendPacket( pConfig->taskID, 0, pPacketOut );
          }

          uiDataCounter = 0;
        }
        
        uiDataCounter++;
        
        pSource++;
        pBuffer++;
            
        // check wraparound
        if ( pBuffer-(uint16_t*)(pData+1) >= uiWindowSize ) {
          pBuffer = (uint16_t*)(pData+1);
        }
      }
          
      pData->iSum     = iSum;
      pData->iSqSum   = iSqSum;
      pData->pCurEntry = pBuffer;
      
      pData->uiDataCounter = uiDataCounter;

      
#ifdef MEASURE_CYCLES
          P6OUT &= ~BIT1;  // set on
#endif

  	// TinyOS 2.0 scheduler drops out
      if ( call Titan.hasPacket( pConfig->taskID, 0 ) > 0 ) {
          call Titan.postExecution(pConfig,0);
      }
    } // atomic
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
