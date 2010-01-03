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
  * TitanConfigCacheM.nc
  *
  * Administers configuration caches.
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  */

#include "TitanComm.h"



module TitanConfigCacheM {
	provides interface TitanConfigCache;
}

implementation {

#warning TitanCache has only implemented basic getCacheEntry

    // configuration cache, initial values for the dice game
    // must initialize TITAN_CONFIG_CACHE_ENTRIES entries
    TitanConfigCacheBlock m_ConfigCache[] = {
      //     config data         accelerometer  magnitude                duplicator variance         zerocross         decision tree                                                                     | CONNECTIONS 
//      { 1, {16,135,11,0,0,22, 0,18,0,1,50, 0,21,1,6,3,8,0,0,1,0, 0,3,2,0, 0,8,3,2,20,10, 0,10,4,6,20,10, 0,29,5,18,133,5,4,0,0,219,48,0,1,1,13,147,48,0,2,48,0,3, 0,0,6,3,0,0,6, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0} },
      //score    configuration   6t   acceleration    variance         variance         variance         decision tree                                                                                                                       communication task   7c
      { 1, 13, {16,6,7,0,0, 22, 0,18,0,1,100, 0,5,1,2,10,10, 0,5,2,2,10,10, 0,5,3,2,10,10, 0,29,4,42,141,13,3,18,9,96,48,0,1,16,8,192,48,0,2,17,8,192,48,0,4,1,5,120,48,0,3,0,5,220,48,0,5,2,7,8,48,0,6,48,0,0, 0,0,5,3,0,0,0,0,  23, 0,0,1,0, 0,1,2,0, 0,2,3,0, 1,0,4,0, 2,0,4,1, 3,0,4,2, 4,0,5,0, 0,    0,0,0,0,0,0,0,0,0,0,0,0,0} },
      //pickup    configuration          
      { 2, 11, {16,5,6,0,0, 37, 0,18,0,1,50, 0,21,1,6,3,8,0,0,1,0, 0,8,2,3,20,10,4, 0,7,3,3,1,0, 50, 0,0,4,3,0,0,0, 38, 0,0,1,0, 0,1,1,1, 0,2,1,2, 1,0,2,0, 2,0,3,0, 3,0,4,0,    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0} },
      // shake
      { 3, 11, {16,5,6,0,0, 53, 0,18,0,1,50, 0,21,1,6,3,8,0,0,1,0, 0,8,2,3,20,10,4, 0,7,3,3,1,0,100, 0,0,4,3,0,0,0, 54, 0,0,1,0, 0,1,1,1, 0,2,1,2, 1,0,2,0, 2,0,3,0, 3,0,4,0,    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0} },
      // roll
      { 4, 11, {16,5,6,0,0, 69, 0,18,0,1,50, 0,21,1,6,3,8,0,0,1,0, 0,8,2,3,20,10,4, 0,7,3,3,1,0,  4, 0,0,4,3,0,0,0, 70, 0,0,1,0, 0,1,1,1, 0,2,1,2, 1,0,2,0, 2,0,3,0, 3,0,4,0,    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0} },
      // backup
      { 5,  3, {16,2,1,0,0, 82, 0,1,0,0, 0,4,1,0, 81, 0,0,1,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0} },
    }; ///< configuration caches
  

  /**
  * Retrieves a full configuration from a TITANCOMM_CACHE_START message
  * @param pMsg a TITANCOMM_CACHE_START message specifying the configuration to load
  * @return the configuration entry, or NULL if not existing.
  */
  command TCCMsg* TitanConfigCache.getCacheEntry( TCCMsg* pConfigMsg ) {
    
    uint8_t i;
    
    TCCConfigMsg *pConfigData = (TCCConfigMsg*)(pConfigMsg+1);
    TCCTaskMsg   *pTaskMsg    = (TCCTaskMsg*)(pConfigData+1);
    
    uint8_t uiTaskNum = pConfigData->numTasks;
    uint8_t uiConnNum = pConfigData->numConnections;

    // check message type
    if ( (pConfigMsg->verType & 0x0F) != TITANCOMM_CACHE_START ) return NULL;
    
    for (i=0; i<TITAN_CONFIG_CACHE_ENTRIES;i++) {
    
      // if the configuration ID with the same footprint is available, use that one
      if ( pTaskMsg->configID == m_ConfigCache[i].configID && 
          m_ConfigCache[i].TasksAndConnections == (uiTaskNum+uiConnNum) ) {
           
           //CAUTION: this seems somehow not to work when m_ConfigCache[i].data has !=126 entries (= one cache line != 128 bytes)
          uint8_t* pData = m_ConfigCache[i].data; 

          dbg("TitanCache","Found matching cache entry at %i\n", i );
           
           // overwrite pointers to cached message
          return (TCCMsg*)pData;
      }
    }
    
    dbg("TitanCache","No matching cache entry found for %i, (%i+%i)\n", pTaskMsg->configID, uiTaskNum, uiConnNum);
    
    return NULL;
  }

  /**
  * Stores a TITANCOMM_CACHE_STORE to the cache for later startup
  * @param pMsg a message containing the partial configuration to be stored
  * @return whether successfull
  */
  command error_t TitanConfigCache.storeCacheEntry( TCCMsg* pMsg ) {
    return FAIL;
  }
  
  /**
  * Clears all configurations from the cache
  */
  command void TitanConfigCache.clearAll() {
  }

  //#define TITAN_USE_CONFIG_CACHE
  #ifdef TITAN_USE_CONFIG_CACHE
  //TODO: save items to cache
  if (pTaskMsg->numTasks <=1) {
    for (i=0; i<TITAN_CONFIG_CACHE_ENTRIES;i++) {
    
      // if the configuration ID with the same footprint is available, use that one
      if ( pTaskMsg->configID == m_ConfigCache[i].configID && 
          m_ConfigCache[i].TasksAndConnections == (uiTaskNum+uiConnNum) ) {
           
           //CAUTION: this seems somehow not to work when m_ConfigCache[i].data has >126 entries
          uint8_t* pData = m_ConfigCache[i].data; //(uint8_t*)&(m_ConfigCache[i].data[0]); // CL: take care of this conversion!!! Seems not to work without &data[0] for mspgcc!!!
           
          // check whether there are any configuration updates (only communication task allowed)
          if ( pTaskMsg->numTasks == 1 ) {
            //TCCMsg* pCfgMsg = (TCCMsg*)pData;
            //TODO: search for the comm task in the cache and replace its config
          }
           
           // overwrite pointers to cached message
          pConfigMsg = (TCCMsg*)pData;
          pConfigData = (TCCConfigMsg*)(pConfigMsg+1);
          pTaskMsg    = (TCCTaskMsg*)(pConfigData+1);

          #ifdef CL_DEBUG_CACHE
             m_debug[0] =111;
             
             //m_debug[1] = (uint16_t)(pData)&0xFF; //147
             //m_debug[2] = (((uint16_t)(pData))>>8)&0xFF; //16
             //m_debug[1] = (uint16_t)(pConfigMsg)&0xFF; // 147
             //m_debug[2] = (((uint16_t)(pConfigMsg))>>8)&0xFF; // 16
             
             //m_debug[3] = (uint16_t)(m_ConfigCache[i].data)&0xFF; //147
             //m_debug[4] = (((uint16_t)(m_ConfigCache[i].data))>>8)&0xFF; //16
             //m_debug[3] = (uint16_t)(pTaskMsg)&0xFF; // 152
             //m_debug[4] = (((uint16_t)(pTaskMsg))>>8)&0xFF; // 16
             
             //m_debug[5] = (uint16_t)(m_ConfigCache)&0xFF; // 16
             //m_debug[6] = (((uint16_t)(m_ConfigCache))>>8)&0xFF; //17
             //m_debug[5] = pConfigData->numConnections; // 255
             //m_debug[6] = pTaskMsg->numTasks; // 15
             
             m_debug[1] = ((uint16_t)&(m_ConfigCache[i].data[1]))&0xFF; // 16
             m_debug[2] = ((((uint16_t)&(m_ConfigCache[i].data[1])))>>8)&0xFF; //17
             m_debug[3] = ((uint16_t)&pData[1])&0xFF; // 16
             m_debug[4] = ((((uint16_t)&pData[1]))>>8)&0xFF; //17


             m_debug[4] = pData[0]; // 255   m_ConfigCache[i].data[0]; // 16
             m_debug[5] = pData[1]; // 255   m_ConfigCache[i].data[1]; // 5
             m_debug[6] = pData[2]; // 255   m_ConfigCache[i].data[2]; // 6
             m_debug[7] = pData[3]; // 255   m_ConfigCache[i].data[0]; // 16
             m_debug[8] = pData[4]; // 255   m_ConfigCache[i].data[1]; // 5
             m_debug[9] = pData[5]; // 255   m_ConfigCache[i].data[2]; // 6

             signal TitanConfigure.issueError(pConfigMsg->verType,pConfigData->numConnections);
          #endif

           break;
      }
    }
  }
  #endif


}
