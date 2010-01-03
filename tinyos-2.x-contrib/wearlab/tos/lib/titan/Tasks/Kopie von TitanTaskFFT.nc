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
  * TitanTaskFFT.nc
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  *
  * FFT Task performs a FFT on a power-of-two window sized buffer of maximum 
  * 128 samples.
  * 
  * Note that this process is very time consuming.
  * 
  */

module TitanTaskFFT {
	uses interface Titan;
}

implementation {

	#include "fft.c"

	// the data used by the task should always be 
	// organized in a structure.
	typedef struct FFTTaskData {
		uint8_t uiLogWindowSize;
		int32_t  *pFFTData;
		uint16_t *pInBufferPos;
		uint16_t *pOutBufferPos;
	} FFTTaskData;
	
    event error_t Titan.init() {
      return SUCCESS;
    }

	/**
	* Returns the universal task identifier of the task. These identifiers are 
	* stored in TitanTaskUIDs.h and should be statically programmed.
	* @return Universal Task Identifier
	*/
	event TitanTaskUID Titan.getTaskUID() {
		return TITAN_TASK_FFT;
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

		FFTTaskData* pData;
		uint16_t     uiWindowSize;

		// define number of i/o-ports		
		pConfig->inPorts  = 1;
		pConfig->outPorts = 1;
		
		if ( pConfig->configLength != 1 ) {
		  call Titan.issueError(ERROR_CONFIG);
		  return FAIL;
		}
		
        uiWindowSize = 1<<pConfig->configData[0];
        
		// allocate memory for the data structure
		pConfig->pTaskData = call Titan.allocMemory(pConfig->taskID,
		                                            sizeof(FFTTaskData) +
		                                            (uiWindowSize+(uiWindowSize>>1))*sizeof(uint16_t) +
		                                            uiWindowSize*sizeof(int32_t)
		                                            );
		                                            
		                                            
		if ( pConfig->pTaskData == NULL ) {
			call Titan.issueError(ERROR_NO_MEMORY);
			return FAIL;
		}
		
		// for easy access		
		pData = (FFTTaskData*)pConfig->pTaskData;
		
		////////////////////////////////////////////////////////////////////////
		// Application dependent code
		
		pData->uiLogWindowSize  = pConfig->configData[0];
		pData->pInBufferPos  = (uint16_t*)(pData+1);
		pData->pOutBufferPos = pData->pInBufferPos + uiWindowSize;
		pData->pFFTData      = (int32_t*)(pData->pOutBufferPos + (uiWindowSize>>1));
		
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
	
	void sendPackets(TitanTaskConfig* pConfig) {
		FFTTaskData *pData  = (FFTTaskData*)pConfig->pTaskData;
		
		// send all data until everything is gone
		while ( (uint8_t*)pData->pOutBufferPos - (uint8_t*)pData - sizeof(FFTTaskData) < 
             (1<<(pData->uiLogWindowSize+1))+(1<<pData->uiLogWindowSize) ) { // input buffer 128x16bit output: 64x16 bit 
            
            int16_t* pDataOut;
            uint16_t uiRemainingElements;
            uint16_t i;

	    	TitanPacket *pPacketOut = call Titan.allocPacket( pConfig->taskID, 0 );
	    	
	    	// check if there is a packet
	    	if ( pPacketOut == NULL ) {
	        call Titan.postExecution(pConfig,2);  
  	      return; //cl: this makes the motes run?
	    	}
            
            // calculate the number of 16 bit values remaining to output
            uiRemainingElements = (uint8_t*)pData + sizeof(FFTTaskData) + 
                                  (1<<(pData->uiLogWindowSize+1))+(1<<pData->uiLogWindowSize) - 
                                  (uint8_t*)pData->pOutBufferPos;
            uiRemainingElements = (uiRemainingElements > TITAN_PACKET_SIZE)? 
                                                         TITAN_PACKET_SIZE>>1 :
                                                         uiRemainingElements>>1;
                                                         

    		// output
	    	pDataOut = (int16_t*)pPacketOut->data;
	    	for ( i=0; i < uiRemainingElements; i++ ) {
	    	  *(pDataOut++) = *(pData->pOutBufferPos++);
	    	}

	    	pPacketOut->length = uiRemainingElements*2;
	    	pPacketOut->type   = TT_INT16;
	    	
	    	//cl: test
	    	pPacketOut->data[0] = (uiRemainingElements==24)? 5 : 3;
            dbg( "TitanTask", "TaskFFT:sendPackets: sending...");

		    call Titan.sendPacket( pConfig->taskID, 0, pPacketOut );
		    
        } 

        // set buffer pointers back to the beginning        
		pData->pOutBufferPos = pData->pInBufferPos + (1<<pData->uiLogWindowSize);
		
	}
	
	void doFFT(TitanTaskConfig* pConfig) {
		FFTTaskData *pData  = (FFTTaskData*)pConfig->pTaskData;
		int16_t i;
		int16_t* pDataBuf;
		int32_t* pFFTData;
		
		// copy data to FFT working buffer
		pDataBuf = (uint16_t*)(pData+1);
		pFFTData = pData->pFFTData;
		for ( i=0; i < (1<<pData->uiLogWindowSize); i++ ) {
          *(pFFTData++) = ((int32_t)(*(pDataBuf++)));
        }
		
		// perform FFT
#ifdef MEASURE_CYCLES
        P6OUT |=  BIT1;  // set on
#endif

 		adapt_int32_array(pData->pFFTData, (1<<pData->uiLogWindowSize), 24);
		fixpt32_micfft( pData->uiLogWindowSize, pData->pFFTData );
		adapt_int32_array(pData->pFFTData, (1<<pData->uiLogWindowSize), 25);
		comp2real( pData->uiLogWindowSize, pData->pFFTData );
		adapt_int32_array(pData->pFFTData, (1<<pData->uiLogWindowSize), 15);
		compabs( pData->uiLogWindowSize, ((uint16_t*)(pData+1)) + (1<<pData->uiLogWindowSize), pData->pFFTData );

#ifdef MEASURE_CYCLES
        P6OUT &= ~BIT1;  // set off
#endif
		
//#define DEBUG_FFT		
#if defined(DEBUG_FFT) && defined(PACKET_SIM_H_INCLUDED)
        {
            int16_t *pIn  = (uint16_t*)(pData+1);
            uint16_t *pOut = ((uint16_t*)(pData+1)) + (1<<pData->uiLogWindowSize);
            dbg( "TitanTask", "*************************** FFT debug start\n");
	        for ( i=0; i < (1<<(pData->uiLogWindowSize-1)); i++ ) {
	          dbg( "TitanTask", "%10i      %10u\n", *(pIn++), *(pOut++) );
	        }
	        for ( ; i < (1<<pData->uiLogWindowSize); i++ ) {
	          dbg( "TitanTask", "%10i\n", *(pIn++) );
	        }
            dbg( "TitanTask", "*************************** FFT debug end\n");
            fflush(stdout);
        }
#endif		
		
		// ok, now output all data
		call Titan.postExecution(pConfig,2) 

		pData->pInBufferPos  = (uint16_t*)(pData+1);
		
	}
	
	// This is the actual working thread	
	void readPacket (TitanTaskConfig* pConfig) {

		FFTTaskData *pData;
		TitanPacket *pPacketIn;
		uint8_t i;
		uint16_t *pBuf;
		
		if ( pConfig == NULL ) {
			dbg("TitanTask","WARNING: Got no context in task FFT\n");
			return;
		}
		
		pData  = (FFTTaskData*)pConfig->pTaskData;
		

		// check whether the buffer is already full
        if ( (uint8_t*)pData->pInBufferPos - (uint8_t*)pData - sizeof(FFTTaskData) >= 
             (1<<pData->uiLogWindowSize)*2 ) {
          // reschedule this task, and get the packet later
          call Titan.postExecution(pConfig,0)
          return; 
        }
        
		// input
		pPacketIn = call Titan.getNextPacket( pConfig->taskID, 0 );
		
		if ( pPacketIn == NULL ) {
		  dbg( "TitanTask", "Task FFT: NULL pointer received in readPacket()\n" );
		  call Titan.issueError(ERROR_PACKET);
		  return;
		}

        if ( pPacketIn->type != TT_INT16 ) { 
          dbg( "TitanTask", "Task FFT: Invalid packet type\n" );
		  call Titan.issueError(ERROR_PACKET);
		  return;
        }
        
        // TODO: SAVE DATA IF PACKET DOES NOT EXACTLY FILL THE BUFFER!!!
        
        pBuf = (uint16_t*)pPacketIn->data;
        for( i=0; i < pPacketIn->length/2; i++ ) {
          *(pData->pInBufferPos++) = *(pBuf++);
        }
        
        // if the buffer is full, start the FFT
        if ( (uint8_t*)pData->pInBufferPos - (uint8_t*)pData - sizeof(FFTTaskData) >= 
             (1<<pData->uiLogWindowSize)*2 ) {

          dbg( "TitanTask", "FFT: Buffer full, starting FFT\n");
          // reschedule this task
          call Titan.postExecution(pConfig,1); // fft
        }
		
        // TinyOS 2.0 scheduler drops out
        if ( call Titan.hasPacket( pConfig->taskID, 0 ) > 0 ) {
          call Titan.postExecution(pConfig,0);
        }

	}
  
  event void Titan.execute( TitanTaskConfig* pConfig, uint8_t uiPortID ) {
    switch(uiPortID) {
      case 0: readPacket(pConfig);break;
      case 1: doFFT(pConfig);break;
      case 2: sendpackets(pConfig;break;
      default:
    }
  }
	
	/**
	* Is issued when a new packet arrives at the input port.
	* @param  iPort Port where the packet arrives
	* @return SUCCESS if the packet will be processed
	*/
	async event error_t Titan.packetAvailable( TitanTaskConfig* pConfig, uint8_t uiPort ) {

		// start the working task
		if (call Titan.postExecution(pConfig,0) == FAIL ) {
		  call Titan.issueError(6);
		  return FAIL;
		}


		return SUCCESS;
	}

}
