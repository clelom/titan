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
  * TitanInternal.h
  *
  * This file defines Titan internal structures and constants that define the 
  * overall behavior.
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  */

 #ifndef TITANINTERNAL_H
 #define TITANINTERNAL_H
 
 #include "Titan.h"
 #include "TitanTaskUIDs.h"
  
  /* ********************************************************************** */
  /* *                     Configuration constants                        * */
  /* ********************************************************************** */

  #define TITAN_MAX_TASKS 8 /* number of tasks that can be instantiated */
  #define TITAN_MAX_PORTS_PER_TASK 16 /* maximum sum of input and output ports */
  #define TITAN_MAX_FIFOS 14 /*  number of FIFOs or interconnects allowed */
  #define TITAN_FIFO_SIZE  5 /* /* depth of the FIFO buffers */
  #define TITAN_TASK_MEMORY_SIZE 3072 /* dynamic memory space for the tasks */
  #define TITAN_TASK_CONTEXT_QUEUE_LENGTH 2*TITAN_MAX_TASKS /* queue of scheduled task contexes */

  // Buffersize of rf2serial buffer
  #define SERIAL_BUFFER_SIZE 4

  
  /* Communication channel used. Can be 11-26, default 11 */
  #ifndef TITAN_COMM_CHANNEL
    #define TITAN_COMM_CHANNEL CC2420_DEF_CHANNEL
  #endif

  /* ********************************************************************** */
  /* *                            Structures                              * */
  /* ********************************************************************** */

  typedef struct TitanFIFOEntry {
    TitanPacket packet;  // the actual packet data, CAUTION:MUST BE FIRST ENTRY
    uint8_t     busy : 1;   // used right now
    uint8_t     reserved : 1;
  } TitanFIFOEntry;
  
  typedef struct TitanFIFO {
    TitanFIFOEntry slots[TITAN_FIFO_SIZE];
    TitanFIFOEntry *pFree;
    TitanFIFOEntry *pNext;
    uint8_t     inTaskID;
    uint8_t     outTaskID;
    uint8_t     outPort;
  } TitanFIFO;
  
  typedef struct TitanTaskContextQueue {
    uint8_t taskID;
    uint8_t portID;
    bool    bActive;
  } TitanTaskContextQueue;
  
  typedef struct TitanTaskInfo {
    TitanTaskConfig config;
    uint8_t portMap[TITAN_MAX_PORTS_PER_TASK];
    uint8_t taskInterface;
  } TitanTaskInfo;
 
  /* This block is used for memory checking while debugging new tasks */
  typedef struct TitanMemCheckBlock {
    uint8_t  checkPattern;
    uint8_t  ownerTaskID;
    struct TitanMemCheckBlock *pNext;
  } TitanMemCheckBlock;
 
  #define TITAN_CONFIG_CACHE_ENTRIES 5
  #define TITAN_CONFIG_CACHE_MAXCONFIGSIZE 126
  typedef struct TitanConfigCacheBlock {
    uint8_t configID;
    uint8_t TasksAndConnections;
    uint8_t data[TITAN_CONFIG_CACHE_MAXCONFIGSIZE];
  } TitanConfigCacheBlock;
 
#endif
