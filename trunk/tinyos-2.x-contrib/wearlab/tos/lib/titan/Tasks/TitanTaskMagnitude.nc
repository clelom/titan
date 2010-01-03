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
  * TitanTaskMagnitude.nc
  *
  * @author Andreas Breitenmoser
  * @author Clemens Lombriser
  *
  * Computes a scaled squared magnitude (i.e. the root is not taken) .
  * All input is assumed to be int16_t.
  *
  * Configuration is  5 bytes (1, 2, 2, 2):
  * [uiCoordNum, iOffset, iScaleFactor, iResultShift]
  *
  * uiCoordNum: number of coordinates which the magnitude should be calculated for.
  * iOffset: overall offset of the input coordinates
  * iScaleFactor: overall scale factor of the input coordinates
  * iResultShift: the overall 32bit result is cut down to the lower 16 bits, a shift can move a more interesting range into that part
  *
  * Magnitude = SUM( (X-iOffset)/iScaleFactor ) >> iResultShift
  *
  * Created: 17.01.2008
  * Modifications:
  * 16.06.2008 Changed to multiple input ports
  */

  
module TitanTaskMagnitude
{
	uses interface Titan;
}
implementation
{
	// the data used by the task should always be 
	// organized in a structure.
	typedef struct MagnitudeTaskData {
		uint8_t uiCoordNum;
		int16_t iOffset;
		int16_t iScaleFactor;
    uint16_t    inputs_valid;
    uint8_t uiResultShift;
	} MagnitudeTaskData;

	
    event error_t Titan.init()
	{
      return SUCCESS;
    }

	
	/**
	* Returns the universal task identifier of the task. These identifiers are 
	* stored in TitanTaskUIDs.h and should be statically programmed.
	* @return Universal Task Identifier
	*/
	event TitanTaskUID Titan.getTaskUID()
	{
		return TITAN_TASK_MAGNITUDE;
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
		MagnitudeTaskData* pData;
		uint8_t *pCfgData = pConfig->configData;
		uint8_t uiCoordNum;
    uint8_t uiResultShift = 0;
		int16_t iOffset = 0;
		int16_t iScaleFactor = 1;

		// define number of i/o-ports		
		pConfig->outPorts = 1;
		
		// check the configuration size to determine what is available
		switch (pConfig->configLength)
		{
      case 6: uiResultShift = pCfgData[5];
			case 5: iOffset = ((int16_t)pCfgData[1])<<8;
					iOffset += (int16_t)pCfgData[2];
					
					iScaleFactor = ((int16_t)pCfgData[3])<<8;
					iScaleFactor += (int16_t)pCfgData[4];
					
			case 1: uiCoordNum = pCfgData[0];
					break;
			default:
				dbg( "TitanTask", "Task Magnitude: Unknown configuration size\n");
				call Titan.issueError(ERROR_CONFIG);
				return FAIL;
		}
		
		pConfig->inPorts  = uiCoordNum;

		// allocate memory for the data structure
		pConfig->pTaskData = call Titan.allocMemory(pConfig->taskID, sizeof(MagnitudeTaskData));
		if (pConfig->pTaskData == NULL)
			call Titan.issueError(ERROR_NO_MEMORY);

		// for easy access		
		pData = (MagnitudeTaskData*)pConfig->pTaskData;
		
		pData->uiCoordNum = uiCoordNum;
		pData->iOffset = iOffset;
		pData->iScaleFactor = iScaleFactor;
    pData->inputs_valid = 0;
    pData->uiResultShift = uiResultShift;
		
		dbg( "TitanTask", "Task Magnitude: Configured with #coord = %i, off = %i, scale = %i, resShift = %i\n",
                      uiCoordNum,  iOffset, iScaleFactor, uiResultShift);
	
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
		error_t result = call Titan.freeMemory(pConfig->taskID, pConfig->pTaskData);
	
		return result;
	}
	
	
	/**
	* Starts the operation of a task. This could also wake it up again.
	*/
	event error_t Titan.start(TitanTaskConfig* pConfig)
	{
		return SUCCESS;
	}
	
	
	/**
	* Stops the operation of a task. It should then prepare a sleep mode.
	*/
	event error_t Titan.stop(TitanTaskConfig* pConfig)
	{
		return SUCCESS;
	}
	
  /*
   *	Square root by abacus algorithm, Martin Guy @ UKC, June 1985.
   *	From a book on programming abaci by Mr C. Woo.
   *	Argument is a positive integer, as is result.
   *
   *	I have formally proved that on exit:
   *		   2		   2		   2
   *		res  <= x < (res+1)	and	res  + op == x
   *
   *	This is also nine times faster than the library routine (-lm).
   */

  uint16_t mysqrt16(uint16_t x) {
  	
  	register uint16_t op,res,one;

    op = x;
  	res = 0;

  	/* "one" starts at the highest power of four <= than the argument. */

    one = 1<<14;/* second-to-top bit set */
    
    while (one > op) one >>= 2;

  	while (one != 0) {
  		if (op >= res + one) {
  			op = op - (res + one);
  			res = res + (one<<1);
  		}
  		res>>= 1;
  		one>>= 2;
  	}
  	return(res);
  }
   
  uint32_t mysqrt(uint32_t x) {
  	/*
  	 *	Logically, these are unsigned. We need the sign bit to test
  	 *	whether (op - res - one) underflowed.
  	 */
  	
  	uint32_t op,res,one;
    
    // check whether the whole thing can be done faster in 16bit
    if ( ((x>>16)&0xFFFF)  == 0 ) return mysqrt16(x&0xFFFF);

    op = x;
  	res =  0;

  	/* "one" starts at the highest power of four <= than the argument. */

    one = 0x40000000;	/* (1<<30) second-to-top bit set */
    
    while (one > op) one >>= 2;

  	while (one != 0) {
  		if (op >= res + one) {
  			op = op - (res + one);
  			res = res + (one<<1);
  		}
  		res>>= 1;
  		one>>= 2;
  	}

  	return(res);
  }

	
	////////////////////////////////////////////////////////////////////////////
	// This is the actual working thread	
	event void Titan.execute( TitanTaskConfig* pConfig, uint8_t uiPortID )
	{
		MagnitudeTaskData *pData;
		TitanPacket *pPacketOut;
		
		uint8_t uiCoordNum;
		int16_t iOffset;
		int16_t iScaleFactor;
		uint32_t uiMagnitude=0;
		uint8_t i;

		if ( pConfig == NULL ) {
			dbg("TitanTask","WARNING: Got no context in task magnitude\n");
			return;
		}
		pData  = (MagnitudeTaskData*)pConfig->pTaskData;
		
		uiCoordNum = pData->uiCoordNum;
		iOffset = pData->iOffset;
		iScaleFactor = pData->iScaleFactor;
		
		// get in/out packets
    for ( i=0; i < pData->uiCoordNum; i++ ) {
      TitanPacket *pPacketIn  = call Titan.getNextPacket(pConfig->taskID, i);
      uint32_t uiNewMagnitude;
      
      if ( pPacketIn == NULL ) {
        call Titan.issueError(ERROR_PACKET);
        dbg("TitanTask", "Task Magnitude: No input packet at port %i, counting as zero\n", i);
        continue;
      }
      
      // read out the magnitude
      switch( pPacketIn->type ) {
        case TT_UINT16: { uint16_t uiMag = *((uint16_t*)(pPacketIn->data));
                            uiMag = (uiMag-iOffset)/iScaleFactor;
                            uiNewMagnitude = uiMag;
                            uiNewMagnitude *= uiNewMagnitude; // do only this in 32 bit
                        }
                        break;
        case TT_INT16:  { int16_t iMag = *((int16_t*)(pPacketIn->data));
                            iMag = (iMag-iOffset)/iScaleFactor;
                            uiNewMagnitude = iMag;
                            uiNewMagnitude *= uiNewMagnitude; // do only this in 32 bit
                        }
                        break;
        case TT_UINT8:  { uint16_t uiMag = *((uint8_t*)(pPacketIn->data));
                            uiMag = (uiMag-iOffset)/iScaleFactor;
                            uiNewMagnitude = uiMag;
                            uiNewMagnitude *= uiNewMagnitude; // do only this in 32 bit
                        }
                        break;
        case TT_INT8:   { int16_t iMag = *((int8_t*)(pPacketIn->data));
                            iMag = (iMag-iOffset)/iScaleFactor;
                            uiNewMagnitude = iMag;
                            uiNewMagnitude *= uiNewMagnitude; // do only this in 32 bit
                        }
                        break;
        default:        dbg("TitanTask","Task Magnitude: Unknown input type %i. Omitting\n", pPacketIn->type);
                        continue;
      }
      
      // sum up magnitudes
      uiMagnitude += uiNewMagnitude;
      
    }

    pPacketOut = call Titan.allocPacket(pConfig->taskID, 0);
    if (pPacketOut == NULL)
    {
      call Titan.issueError(ERROR_PACKET);
      return;
    }
    
    uiMagnitude = mysqrt(uiMagnitude);
    
    dbg("TitanTask", "Task Magnitude: Magnitude is %i, forwarding %i\n", uiMagnitude, ((uiMagnitude>>pData->uiResultShift)&0xFFFF) );
    
		pPacketOut->type = TT_UINT16;
		pPacketOut->length = 2;	
    *((uint16_t*)pPacketOut->data) = (uint16_t)((uiMagnitude>>pData->uiResultShift)&0xFFFF);
    

		// output
		call Titan.sendPacket(pConfig->taskID, 0, pPacketOut);
	}

	
	/**
	* Is issued when a new packet arrives at the input port.
	* @param  iPort Port where the packet arrives
	* @return SUCCESS if the packet will be processed
	*/
	async event error_t Titan.packetAvailable(TitanTaskConfig* pConfig, uint8_t uiPort)
	{
		MagnitudeTaskData *pData  = (MagnitudeTaskData*)pConfig->pTaskData;
    
    // check feature overwrite (2x write between classifications)
    if (pData->inputs_valid & (0x1<<uiPort) ) dbg("TitanTaskDT", "Warning: overwriting feature number %i (0x%x)\n",uiPort,pData->inputs_valid);

    // signal new feature values
    pData->inputs_valid |= 0x1<<uiPort;

    // check whether all features have new values
    if ( pData->inputs_valid == (0x1<<pData->uiCoordNum)-1 ) {
//cl :dbg    if ( uiPort == 0 ) {
    
      //debug dbg("TitanTaskDT","All inputs have new values - starting classification\n\n");
      
      // start classifier
  		if (call Titan.postExecution(pConfig,uiPort) == FAIL ) {
  		  call Titan.issueError(6);
  		}
      
      // reset input values
      pData->inputs_valid = 0;

    }
		return SUCCESS;

	}

}
