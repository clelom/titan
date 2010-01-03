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
  * TitanTaskSimpleWriter.nc
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  *
  * This task writes numbers to the Titan network.
  *
  */

#define COUNTER
//#define JIGSAW
//#define RANDOM
//#define SINE_WAVE

module TitanTaskSimpleWriter {
  uses interface Titan;
  uses interface Timer<TMilli>;
}

implementation {

  typedef struct SimpleWriterData {
    int16_t counter;
    int16_t period;
  } SimpleWriterData;
  
  TitanTaskConfig *m_pConfig;
  
    event error_t Titan.init() {
      return SUCCESS;
    }

  event TitanTaskUID Titan.getTaskUID() {
    return TITAN_TASK_SIMPLEWRITER;
  }

  event error_t Titan.configure( TitanTaskConfig* pConfig ) {
    SimpleWriterData* pData;
    
    m_pConfig = pConfig;
    
#ifdef MEASURE_CYCLES
        P6OUT &= ~BIT1;  // set on
#endif
    dbg( "TitanTask", "New task with ID: %i: SimpleWriter\n", pConfig->taskID );
    
    pConfig->inPorts  = 0;
    pConfig->outPorts = 1;
    
    pConfig->pTaskData = call Titan.allocMemory(pConfig->taskID,sizeof(SimpleWriterData));
    
    if ( pConfig->pTaskData == NULL ) {
      call Titan.issueError(ERROR_NO_MEMORY);
    }
    
    pData = (SimpleWriterData*)pConfig->pTaskData;
    
    pData->counter = 0;
    
    if ( pConfig->configLength == 2 ) {
      pData->period = ((uint16_t)pConfig->configData[0]<<8) | (uint16_t)pConfig->configData[1];
    } else {
      pData->period = 1024; // 1/4 second
    }
  
#ifdef MEASURE_CYCLES
        P6OUT |= BIT1;  // set on
#endif
    return SUCCESS;
  }
  
  
  event error_t Titan.terminate( TitanTaskConfig* pConfig ) {
    call Timer.stop();
    return SUCCESS;
  }
  
  event error_t Titan.start( TitanTaskConfig* pConfig ) {
    dbg( "TitanTask", "Starting TitanTaskSimpleWriter.\n" );
    
    call Timer.startPeriodic( ((SimpleWriterData*)pConfig->pTaskData)->period );
    
    return SUCCESS;
  }
  
  event error_t Titan.stop( TitanTaskConfig* pConfig ) {
    dbg( "TitanTask", "Stopping TitanTaskSimpleWriter.\n" );
    
    call Timer.stop();
    
    return SUCCESS;
  }
  
  // This should not happen!
  async event error_t Titan.packetAvailable( TitanTaskConfig* pConfig, uint8_t uiPort ) {
    dbg( "TitanTask", "ERROR: TitanTaskSimpleWriter received a packet.\n" );
    call Titan.issueError(ERROR_RECEIVED_SOMETHING);
    return SUCCESS;
  }
  
#ifdef SINE_WAVE
    const int16_t WriteSineWave128[192]={0,402,804,1205,1606,2006,2404,2801,3196,3590,3981,4370,4756,5139,5520,5897,6270,6639,7005,7366,7723,8076,8423,8765,9102,9434,9760,10080,10394,10702,11003,11297,11585,11866,12140,12406,12665,12916,13160,13395,13623,13842,14053,14256,14449,14635,14811,14978,15137,15286,15426,15557,15679,15791,15893,15986,16069,16143,16207,16261,16305,16340,16364,16379,16384,16379,16364,16340,16305,16261,16207,16143,16069,15986,15893,15791,15679,15557,15426,15286,15137,14978,14811,14635,14449,14256,14053,13842,13623,13395,13160,12916,12665,12406,12140,11866,11585,11297,11003,10702,10394,10080,9760,9434,9102,8765,8423,8076,7723,7366,7005,6639,6270,5897,5520,5139,4756,4370,3981,3590,3196,2801,2404,2006,1606,1205,804,402,0,-402,-804,-1205,-1606,-2006,-2404,-2801,-3196,-3590,-3981,-4370,-4756,-5139,-5520,-5897,-6270,-6639,-7005,-7366,-7723,-8076,-8423,-8765,-9102,-9434,-9760,-10080,-10394,-10702,-11003,-11297,-11585,-11866,-12140,-12406,-12665,-12916,-13160,-13395,-13623,-13842,-14053,-14256,-14449,-14635,-14811,-14978,-15137,-15286,-15426,-15557,-15679,-15791,-15893,-15986,-16069,-16143,-16207,-16261,-16305,-16340,-16364,-16379}; // 128-FFT (complex), SCALE=14
    const int16_t WriteSineWave32[48]={0,1606,3196,4756,6270,7723,9102,10394,11585,12665,13623,14449,15137,15679,16069,16305,16384,16305,16069,15679,15137,14449,13623,12665,11585,10394,9102,7723,6270,4756,3196,1606,0,-1606,-3196,-4756,-6270,-7723,-9102,-10394,-11585,-12665,-13623,-14449,-15137,-15679,-16069,-16305};  // 32-FFT (complex), SCALE=14
    const int16_t WriteSineWave4[6]={0,11585,16384,11585,0,-11585};   // 4-FFT (complex), SCALE = 14
#endif

  event void Titan.execute( TitanTaskConfig* pConfig, uint8_t uiPortID ){
        SimpleWriterData *pData;
       TitanPacket *pPacket;
    //int i;
        
	if ( pConfig == NULL ) {
		dbg("TitanTask","WARNING: Got no context in task simplewriter\n");
		return;
	}

    pData  = (SimpleWriterData*)pConfig->pTaskData;
    
    pPacket = call Titan.allocPacket( pConfig->taskID, 0 );

    if ( pPacket == NULL ) {
      dbg( "TitanTask", "FATAL ERROR: no packet allocated!\n" );
      return;
    }
	
	dbg( "TitanTask", "SimpleWriter running...\n" );
    
    pPacket->type    = TT_UINT16;
    


#ifdef JIGSAW
    {
	uint16_t i;
        // jigsaw counter
    for ( i=0; i < TITAN_PACKET_SIZE/2; i++ ) {
        *((int16_t*)&(pPacket->data[i*2])) = pData->counter;
        (pData->counter >= 5 )? pData->counter=4 :
        (pData->counter <=-4 )? pData->counter=-3 :
        (pData->counter & 0x1 )? pData->counter+=2 :
                                 (pData->counter-=2) ;
        dbg( "TitanTask", "value: %3i\n", pData->counter );
      }
	}
#endif
      
#ifdef RANDOM
      {
        int16_t* pOut = (int16_t*)(&pPacket->data[0]);
        *(pOut++) = pData->counter;
        pData->counter = (pData->counter==0)? 1:pData->counter;
        pData->counter*=779;
        pData->counter&=0x0FFF;
        pPacket->length  = (pOut - (int16_t*)(pPacket->data))<<1; //TITAN_PACKET_SIZE;

      }
#endif
      
#ifdef COUNTER
        // writes same value multiple times
      {
          int16_t* pOut = (int16_t*)(&pPacket->data[0]);
          *(pOut++) = pData->counter++;
//        *(pOut++) = pData->counter++;
        // *(pOut++) = pData->counter--;
        // *(pOut++) = pData->counter++;
/*        *(pOut++) = pData->counter;
        *(pOut++) = pData->counter++;
        *(pOut++) = pData->counter;
        *(pOut++) = pData->counter;
        *(pOut++) = pData->counter++;
*/
        pPacket->length  = (pOut - (int16_t*)(pPacket->data))<<1; //TITAN_PACKET_SIZE;

      }
#endif

#ifdef SINE_WAVE
    {
		uint16_t i;
		
        // sine wave
        for ( i=0; i < 8; i++ ) {
            if ( pData->counter < 32 ) {
            *((int16_t*)&(pPacket->data[i*2])) = WriteSineWave32[pData->counter++];
          } else if ( pData->counter < 64 ) {
            *((int16_t*)&(pPacket->data[i*2])) = -WriteSineWave32[pData->counter-32];
            pData->counter++;
          } else {
              *((int16_t*)&(pPacket->data[i*2])) = WriteSineWave32[0];
          pData->counter = 1;
        }
        
        }
    }
    pPacket->length  = 16;
#endif

//#define TEST_MUL32
#ifdef TEST_MUL32

  {
    uint16_t i;
    uint16_t mul = pData->counter;
    uint16_t result16 = mul*mul;
    uint32_t result32a = mul*mul;
    uint32_t result32b = ((uint32_t)mul)*((uint32_t)mul);

    i=0;
    pPacket->data[i++] = ((mul>>8)&0xFF);
    pPacket->data[i++] = ((mul>>0)&0xFF);

    pPacket->data[i++] = 0xFF;
    
    pPacket->data[i++] = ((result16>>8)&0xFF);
    pPacket->data[i++] = ((result16>>0)&0xFF);

    pPacket->data[i++] = 0xFF;

    pPacket->data[i++] = ((result32a>>24)&0xFF);
    pPacket->data[i++] = ((result32a>>16)&0xFF);
    pPacket->data[i++] = ((result32a>> 8)&0xFF);
    pPacket->data[i++] = ((result32a>> 0)&0xFF);

    pPacket->data[i++] = 0xFF;

    pPacket->data[i++] = ((result32b>>24)&0xFF);
    pPacket->data[i++] = ((result32b>>16)&0xFF);
    pPacket->data[i++] = ((result32b>> 8)&0xFF);
    pPacket->data[i++] = ((result32b>> 0)&0xFF);
    
    pPacket->length = i;

    pData->counter += 0x08;
  }

#endif
    
#ifdef TOSSIM
    {
      int i;
      char strOut[1024];
      int16_t* pOut = (int16_t*)(&pPacket->data[0]);
      
      sprintf(strOut,"SimpleWriter: Packet length: %i: ",pPacket->length>>1);
      for (i=0; i<pPacket->length>>1;i++) {
        char strAdd[256];
        sprintf(strAdd,"%3i ", *(pOut++));
        strcat(strOut,strAdd);
      }
      strcat(strOut,"\n");

      dbg( "TitanTask", strOut );
    }
#endif

    call Titan.sendPacket( pConfig->taskID, 0, pPacket );
    
  }


  event void Timer.fired() {
    call Titan.postExecution( m_pConfig, 0 );
  }
  
}
