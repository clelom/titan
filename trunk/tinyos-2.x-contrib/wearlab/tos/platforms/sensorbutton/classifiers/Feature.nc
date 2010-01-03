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
 * Features for Sensorbutton
 * provides Mean & Var
 * @author Andreas Bubenhofer
 * 
 */


#include "SBcalc.h"





module Feature {
   provides interface Features;
}
 
implementation {

   uint16_t* resultfeatures;
   uint16_t** samples;
   uint8_t head;
   uint8_t tail;
   // keep in mind uint16_t features[CALC_NUMOFFEATURES * CALC_NUMOFSENSORS];
   task void calculation();
   ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   // Functions  
   uint16_t calc_mean(uint16_t *values, uint8_t val_head, uint8_t val_tail,
                  struct calc_mean_storage_t *ms);
   uint16_t calc_var(uint16_t *values, uint8_t val_head, uint8_t val_tail,
                  struct calc_mean_storage_t *stds, uint16_t mean);
   ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   
   
  
	
   command void Features.getFeatures( uint16_t* pfeatures, uint16_t ** psamples, uint8_t phead ,uint8_t ptail)
  {
     resultfeatures = pfeatures;
	 samples = psamples;
	 head = phead;
	 tail = ptail;
	 
	 post calculation();
  }

  
  task void calculation(){
    
	int16_t i;
  
  /*snext = head - CLASSIFICATION_INTERVAL + 1;
	if (snext<0) snext = CALC_SENSORS_BUF_SIZE - snext;
	tail = snext;
  */
  for(i=0 ; i<CALC_NUMOFSENSORS ; i++)
   // for(i=0 ; i<1 ; i++)
  {
    
    //if (++next >= CALC_SENSORS_BUF_SIZE) next = 0;
	
    
    resultfeatures[i+CALC_NUMOFFEATURES*0] = 1+calc_mean(calc_sensors_buf[i],//i*CALC_NUMOFFEATURES+0calc_sensors_buf
                                              head,
                                              tail,
                                              &calc_mean_storage[i]);
    
    
   
    // scaling: variance is downscaled by 6 bits, but the script downscales it only by 3 bits
    resultfeatures[i+CALC_NUMOFSENSORS*1] = 1+8*calc_var(calc_sensors_buf[i], //i*CALC_NUMOFFEATURES+2 8*8/8
                                              head,
                                              tail,
                                              &calc_std_storage[i],
                                              resultfeatures[i+CALC_NUMOFFEATURES*0]);
    // Energy downscaled by 3 bits
    //resultfeatures[i+CALC_NUMOFSENSORS*2] = resultfeatures[i+CALC_NUMOFSENSORS*1]+(resultfeatures[i+CALC_NUMOFFEATURES*0]/16)*(resultfeatures[i+CALC_NUMOFFEATURES*0]/16);

    
  }

  /*
  resultfeatures[3*CALC_NUMOFFEATURES+0] = calc_mean(calc_sensors_buf[3],
                                              calc_sensors_buf_head[3],
                                              calc_sensors_buf_tail[3],
                                              &calc_mean_storage[3]);
 calc_sensors_buf_tail[3] = calc_sensors_buf_head[3];
*/ 
    signal Features.featuresDone(resultfeatures);
  }
  
  
  
  default event void Features.featuresDone( uint16_t* features){}
   


///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////                  Functions and  Tasks          //////////
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


uint16_t calc_mean(uint16_t *values, uint8_t val_head, uint8_t val_tail,
                  struct calc_mean_storage_t *ms)
{
  uint16_t new_sum = 0;
  uint8_t new_count = 0;
  
  const uint16_t entity_length=16;
  uint16_t no_entities;
  uint16_t no_in_current_entity;
  uint16_t mean;
  

  if(++(ms->position) >= CALC_MEAN_SIZE) ms->position = 0;
  // substract oldest sum in sums[] from total_sum
  ms->total_sum           -= ms->sums[ms->position];
  ms->total_num_of_values -= ms->num_of_values[ms->position];

  no_entities=0;
  no_in_current_entity=0;
  mean=0;
  while (val_tail != val_head)
  {
   
    new_sum += values[val_tail];
    if(++no_in_current_entity>=entity_length) {
      mean=(mean*no_entities+new_sum/entity_length)/(no_entities+1);
      no_entities++;
      new_sum=0;
      no_in_current_entity=0;
    }
    new_count++;
    if(++val_tail >= CALC_SENSORS_BUF_SIZE) val_tail = 0;
  }
  new_sum=mean;
  ms->sums[ms->position]          = new_sum;
  ms->num_of_values[ms->position] = new_count;
  ms->total_sum                   += new_sum;
  ms->total_num_of_values         += new_count;

  if (ms->total_num_of_values == 0) return 0;
  // max (1/N * sum_{i}^{N} (uint12) ) = uint12
  // therefore it is save to shift the values by 4 bits to get bigger resoution
  //return ((ms->total_sum)>>2)/(ms->total_num_of_values);
  return mean;
  
  
  
  /*uint32_t new_sum = 0;
  uint8_t new_count = 0;
  
  if(++(ms->position) >= CALC_MEAN_SIZE) ms->position = 0;
  // substract oldest sum in sums[] from total_sum
  ms->total_sum           -= ms->sums[ms->position];
  ms->total_num_of_values -= ms->num_of_values[ms->position];

  while (val_tail != val_head)
  { 
    new_sum += values[val_tail];
    new_count++;
    if(++val_tail >= CALC_SENSORS_BUF_SIZE) val_tail = 0;
  }
  
  ms->sums[ms->position]          = new_sum;
  ms->num_of_values[ms->position] = new_count;
  ms->total_sum                   += new_sum;
  ms->total_num_of_values         += new_count;

  if (ms->total_num_of_values == 0) return 0;
  // max (1/N * sum_{i}^{N} (uint12) ) = uint12
  // therefore it is save to shift the values by 4 bits to get bigger resoution
  //return ((ms->total_sum))/(ms->total_num_of_values);
  return (uint16_t) (new_sum)/new_count;
  */
}

uint16_t calc_var(uint16_t *values, uint8_t val_head, uint8_t val_tail,
                  struct calc_mean_storage_t *stds, uint16_t mean)
{
 

uint16_t new_sum = 0;
  uint8_t new_count = 0;
const uint16_t entity_length=16;
  uint16_t no_entities;
  uint16_t no_in_current_entity;
  uint16_t std,debug;
  if(++(stds->position) >= CALC_MEAN_SIZE) stds->position = 0;
  // substract oldest sum in sums[] from total_sum
  stds->total_sum           -= stds->sums[stds->position];
  stds->total_num_of_values -= stds->num_of_values[stds->position];

  
 
  no_entities=0;
  no_in_current_entity=0;
  std=0;
  while (val_tail != val_head)
  {
    debug=values[val_tail];
    //new_sum += values[val_tail];
    new_count=0;
    if (debug > mean)
    new_sum += ((debug-mean)>>3);//   division /8
    else new_sum += ((mean-debug)>>3);
    
    if(++no_in_current_entity>=entity_length) {
      std=(std*no_entities+new_sum/entity_length)/(no_entities+1);
      no_entities++;
      new_sum=0;
      no_in_current_entity=0;
    }
    new_count++;
    if(++val_tail >= CALC_SENSORS_BUF_SIZE) val_tail = 0;
  }
   // no_in_current_entity should be close to entity_length
  std=(std*no_entities+new_sum/no_in_current_entity)/(no_entities+1);
  
  new_sum=std;
  stds->sums[stds->position]          = new_sum;
  stds->num_of_values[stds->position] = new_count;
  stds->total_sum                   += new_sum;
  stds->total_num_of_values         += new_count;

  if (stds->total_num_of_values == 0) return 0;
  // max (1/N * sum_{i}^{N} (uint12) ) = uint12
  // therefore it is save to shift the values by 4 bits to get bigger resoution
  return (std);

 /*uint16_t new_sum = 0;
  uint8_t new_count = 0;
  uint16_t entity_length = 16;
  uint16_t no_entities;
  uint16_t no_in_current_entity;
  uint16_t std,debug;
  if(++(stds->position) >= CALC_MEAN_SIZE) stds->position = 0;
  // substract oldest sum in sums[] from total_sum
  stds->total_sum           -= stds->sums[stds->position];
  stds->total_num_of_values -= stds->num_of_values[stds->position];

  
 
  no_entities=0;
  no_in_current_entity=0;
  std=0;
  while (val_tail != val_head)
  {
  
 //dbg
  
    debug=values[val_tail];
    //new_sum += values[val_tail];
    new_count=0;
    if (debug > mean)
    new_sum += ((debug-mean)/8)*((debug-mean)/8);
    else new_sum += ((mean-debug)/8)*((mean-debug)/8);
    
    if(++no_in_current_entity>=entity_length) {
      std=(std*no_entities+new_sum/entity_length)/(no_entities+1);
      no_entities++;
      new_sum=0;
      no_in_current_entity=0;
    }
    new_count++;
    if(++val_tail >= CALC_SENSORS_BUF_SIZE) val_tail = 0;
  }
   // no_in_current_entity should be close to entity_length
  std=(std*no_entities+new_sum/no_in_current_entity)/(no_entities+1);
  
  new_sum=std;
  stds->sums[stds->position]          = new_sum;
  stds->num_of_values[stds->position] = new_count;
  stds->total_sum                   += new_sum;
  stds->total_num_of_values         += new_count;

  if (stds->total_num_of_values == 0) return 0;
  // max (1/N * sum_{i}^{N} (uint12) ) = uint12
  // therefore it is save to shift the values by 4 bits to get bigger resoution
  return (std);
  */
}


}
