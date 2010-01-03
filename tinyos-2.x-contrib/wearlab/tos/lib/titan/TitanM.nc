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
  * TitanM.nc
  *
  * Core functionality of the Tiny Task Network (Titan).
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  */

#include "TitanInternal.h"

#define TITAN_DO_CHECKS /* does some checks on tasks consistency */

module TitanM {

  provides interface TitanConfigure;
  provides interface Init;
  provides interface Titan[uint8_t uiID];
   
  uses interface TitanMemory;
	uses interface TitanSchedule;
  uses interface Leds;

}

implementation {

  bool m_bReconfiguring;

  /* ********************************************************************** */
  /* *                       Memory reservation                           * */
  /* ********************************************************************** */

  TitanFIFO             m_FIFOs[TITAN_MAX_FIFOS];
  TitanTaskInfo         m_TaskInfo[TITAN_MAX_TASKS];
  TitanTaskUID          m_TaskUIDs[ uniqueCount("Titan") ];

  /* ********************************************************************** */
  /* *                    Memory handling pointers                        * */
  /* ********************************************************************** */
  
  uint8_t m_uiTasksRunning;

  /* ********************************************************************** */
  /* *                       Initialization                               * */
  /* ********************************************************************** */

  
  void initStructures() {
    int i,j;
    
    atomic {
      call TitanSchedule.clearAll(); //cl:ContextQueueProblem
      
      // init FIFOs
      for (i=0; i < TITAN_MAX_FIFOS; i++ ) { // all FIFOs
        for (j=0; j < TITAN_FIFO_SIZE; j++ ) { // all FIFO slots
          m_FIFOs[i].slots[j].busy = 0; // set free
          #ifdef TITANDEBUG  
          m_FIFOs[i].slots[j].packet.cTestPattern = TITAN_TEST_PATTERN;
          #endif
        }
        m_FIFOs[i].inTaskID  = -1;
        m_FIFOs[i].outTaskID = -1;
        m_FIFOs[i].pFree = m_FIFOs[i].pNext = m_FIFOs[i].slots;
      }
    
      // init TitanTaskInfo
      for (i=0; i < TITAN_MAX_TASKS; i++ ) {
        m_TaskInfo[i].taskInterface = (uint8_t)-1;
        m_TaskInfo[i].config.taskType = -1;
      }

    } //atomic
  }

  command error_t Init.init() {

    // initialize structures
    int i;
    
    m_uiTasksRunning = 0;
    
    initStructures();
//cl:ContextQueue    initContextQueue();
    
    // determine task UIDs of all registered task types
    for ( i=0; i < uniqueCount("Titan"); i++ ) {
        m_TaskUIDs[i] = signal Titan.getTaskUID[i]();
        signal Titan.init[i]();
    }
    
    // initialize memory module
    call TitanMemory.init();

    dbg( "Titan", "***************************************\n" );
    dbg( "Titan", "Titan initialization\n" );
    dbg( "Titan", "\n" );
    dbg( "Titan", "Memory usage\n" );
    dbg( "Titan", "------------\n" );
    dbg( "Titan", "FIFO memory   :  %4u\n", sizeof(m_FIFOs) );
    dbg( "Titan", "Task memory   :  %4u\n", TITAN_TASK_MEMORY_SIZE );
    dbg( "Titan", "Task Info     :  %4u\n", sizeof(m_TaskInfo) );
    dbg( "Titan", "                 ----\n");
    dbg( "Titan", "Total         :  %4u\n", 
       sizeof(m_FIFOs) + TITAN_TASK_MEMORY_SIZE +
       sizeof(m_TaskInfo) );
    dbg( "Titan", "\n" );
    dbg( "Titan", "***************************************\n" );
    
    return SUCCESS;
  }
  

  /* ********************************************************************** */
  /* *                          Packet handling                           * */
  /* ********************************************************************** */

  command uint8_t Titan.hasPacket[uint8_t uiID]( uint8_t uiTaskID, uint8_t uiPort ){

    uint8_t uiTargetFIFO;
    uint8_t uiPortMapIndex;
    uint8_t uiPacketNumber;
    uint8_t i;

#ifdef TITAN_DO_CHECKS
    if ( uiTaskID > m_uiTasksRunning ) {
      dbg( "TitanPacket", "ERROR: Task ID %i is not running (only %i tasks)\n", uiTaskID, m_uiTasksRunning );
    }

    if ( m_TaskInfo[uiTaskID].taskInterface != uiID ) {
      dbg( "TitanPacket", "ERROR: Task %i is not of type %i\n", uiTaskID, uiID );
    }
    
    if ( uiPort >= m_TaskInfo[uiTaskID].config.inPorts ) {
      dbg( "TitanPacket", "ERROR: Task %i (Type:%i) accesses input port %i > %i reserved\n", 
         uiTaskID, uiID, uiPort, m_TaskInfo[uiTaskID].config.inPorts );
    }
#endif

    uiPortMapIndex = TITAN_MAX_PORTS_PER_TASK - 1 - uiPort;
    uiTargetFIFO   = m_TaskInfo[uiTaskID].portMap[uiPortMapIndex];

    // look for the number of used packet slots
    uiPacketNumber = 0;
    atomic {
      for ( i=0; i< TITAN_FIFO_SIZE; i++ ) {
          if ( m_FIFOs[uiTargetFIFO].slots[i].busy != 0 ) uiPacketNumber++;
      }
    }
    
    return uiPacketNumber;

  }

  async command TitanPacket* Titan.getNextPacket[uint8_t uiID]( uint8_t uiTaskID, uint8_t uiInPort ) {
  
    uint8_t uiTargetFIFO;
    uint8_t uiPortMapIndex;
    TitanFIFOEntry *pCurEntry;

#ifdef TITAN_DO_CHECKS
    if ( uiTaskID > m_uiTasksRunning ) {
      dbg( "TitanPacket", "ERROR: Task ID %i is not running (only %i tasks)\n", uiTaskID, m_uiTasksRunning );
    }

    if ( m_TaskInfo[uiTaskID].taskInterface != uiID ) {
      dbg( "TitanPacket", "ERROR: Task %i is not of type %i\n", uiTaskID, uiID );
    }
    
    if ( uiInPort >= m_TaskInfo[uiTaskID].config.inPorts ) {
      dbg( "TitanPacket", "ERROR: Task %i (Type:%i) accesses input port %i > %i reserved", 
         uiTaskID, uiID, uiInPort, m_TaskInfo[uiTaskID].config.inPorts );
    }
#endif
    atomic {
      // only return packets that are not reserved anymore to avoid data corruption!
      uiPortMapIndex = TITAN_MAX_PORTS_PER_TASK - 1 - uiInPort;
      uiTargetFIFO   = m_TaskInfo[uiTaskID].portMap[uiPortMapIndex];

          //cll: test
        dbg( "TitanPacket", "Get on FIFO %i by Task %i: Next: %i\tFree: %i\n", uiTargetFIFO, uiTaskID, m_FIFOs[uiTargetFIFO].pNext, m_FIFOs[uiTargetFIFO].pFree );
/*        {
          int i;
          for(i=0;i<TITAN_FIFO_SIZE;i++){
            dbg("TitanPacket", "Reservation: %i %i\n", m_FIFOs[uiTargetFIFO].slots[i].reserved,  m_FIFOs[uiTargetFIFO].slots[i].busy);
          }
        }
*/
      // find the next packet that is not reserved anymore
      pCurEntry = m_FIFOs[uiTargetFIFO].pNext;
      do {
        if ( pCurEntry->reserved == 0 ) break;
        pCurEntry = ( pCurEntry - m_FIFOs[uiTargetFIFO].slots >= TITAN_FIFO_SIZE-1 )?
               m_FIFOs[uiTargetFIFO].slots : pCurEntry + 1;

      } while ( pCurEntry != m_FIFOs[uiTargetFIFO].pFree );

      // check whether there is a valid packet (if full pCurEntry == pFree)
      if ( pCurEntry != m_FIFOs[uiTargetFIFO].pFree || (pCurEntry->busy == 1 && pCurEntry->reserved == 0) ) {

        // if the current packet is returned, move the pNext pointer    
        if ( pCurEntry == m_FIFOs[uiTargetFIFO].pNext ) {
          m_FIFOs[uiTargetFIFO].pNext++;
          
          // wraparound
          if ( m_FIFOs[uiTargetFIFO].pNext - m_FIFOs[uiTargetFIFO].slots  >= TITAN_FIFO_SIZE ) {
            m_FIFOs[uiTargetFIFO].pNext = m_FIFOs[uiTargetFIFO].slots;
          }
        }
      
        // packet is released and can be rewritten
        pCurEntry->busy = 0;
        return &(pCurEntry->packet);
      }
    } // atomic
    return NULL;
  }

  command TitanPacket* Titan.allocPacket[uint8_t uiID]( uint8_t uiTaskID, uint8_t uiOutPort ) {

    uint8_t uiTargetFIFO;

#ifdef TITAN_DO_CHECKS
    if ( uiTaskID > m_uiTasksRunning ) {
      dbg( "TitanPacket", "ERROR: Task ID %i is not running (only %i tasks)\n", uiTaskID, m_uiTasksRunning );
    }

    if ( m_TaskInfo[uiTaskID].taskInterface != uiID ) {
      dbg( "TitanPacket", "ERROR: Task %i is not of type %i\n", uiTaskID, uiID );
    }
    
    if ( uiOutPort >= m_TaskInfo[uiTaskID].config.outPorts ) {
      dbg( "TitanPacket", "ERROR: ALLOC: Task %i (Type:%i) accesses output port %i > %i reserved\n", 
         uiTaskID, uiID, uiOutPort, m_TaskInfo[uiTaskID].config.outPorts );
    }
#endif

    atomic {
      // Determine the FIFO
      uiTargetFIFO = m_TaskInfo[uiTaskID].portMap[uiOutPort];


          //cll: test
//        dbg( "TitanPacket", "Alloc on FIFO %i by Task %i: Next: %i\tFree: %i\n", uiTargetFIFO, uiTaskID, m_FIFOs[uiTargetFIFO].pNext,  m_FIFOs[uiTargetFIFO].pFree );
//        {
//          int i;
//          for(i=0;i<TITAN_FIFO_SIZE;i++){
//            dbg("TitanPacket", "Reservation: %i %i\n", m_FIFOs[uiTargetFIFO].slots[i].reserved,  m_FIFOs[uiTargetFIFO].slots[i].busy);
//          }
//        }


      // check whether the free entry is really free
      if ( m_FIFOs[uiTargetFIFO].pFree->busy == 1 ) {
        dbg( "TitanPacket", "Warning: FIFO %i (from task %i to task %i) full!\n", 
           uiTargetFIFO, m_FIFOs[uiTargetFIFO].inTaskID, 
           m_FIFOs[uiTargetFIFO].outTaskID );
        return NULL; // not free, FIFO full
      } else {
        TitanFIFO      *pTargetFIFO = &(m_FIFOs[uiTargetFIFO]);
        TitanFIFOEntry *pFree       = pTargetFIFO->pFree;
        
        pTargetFIFO->pFree++;

              // check for overflows      
        if ( pTargetFIFO->pFree-pTargetFIFO->slots >= TITAN_FIFO_SIZE ) {
          pTargetFIFO->pFree = pTargetFIFO->slots;
        }
        
        // set packet busy
        pFree->busy = 1;
        pFree->reserved = 1;

//            dbg( "TitanPacket", "Allocating packet %i in FIFO %i (%i,%i)->(%i,%i)\n",
//                 pFree-pTargetFIFO->slots, uiTargetFIFO, uiTaskID, uiOutPort, 
//                 m_FIFOs[uiTargetFIFO].outTaskID, m_FIFOs[uiTargetFIFO].outPort );
        
        return &(pFree->packet);
      }

    } // atomic
    return NULL;
  }

  command error_t Titan.sendPacket[uint8_t uiID]( uint8_t uiTaskID, uint8_t uiOutPort, TitanPacket* pPacket ) {
  
    uint8_t uiTargetFIFO;
    uint8_t uiTargetTask;
    uint8_t uiTargetPort;
    TitanTaskConfig *pConfig;

#ifdef TITAN_DO_CHECKS
    if ( uiTaskID > m_uiTasksRunning ) {
      dbg( "TitanPacket", "ERROR: Task ID %i is not running (only %i tasks)\n", uiTaskID, m_uiTasksRunning );
    }

    if ( m_TaskInfo[uiTaskID].taskInterface != uiID ) {
      dbg( "TitanPacket", "ERROR: Task %i is not of type %i\n", uiTaskID, uiID );
    }
    
    if ( uiOutPort >= m_TaskInfo[uiTaskID].config.outPorts ) {
      dbg( "TitanPacket", "ERROR: SEND: Task %i (Type:%i) accesses output port %i > %i reserved\n", 
         uiTaskID, uiID, uiOutPort, m_TaskInfo[uiTaskID].config.outPorts );
    }
    
    #ifdef TITANDEBUG
    if ( pPacket->cTestPattern != TITAN_TEST_PATTERN ) {
      dbg( "TitanPacket", "ERROR:TESTPATTERN OVERWRITTEN!!!\n" );
    }
    #endif
#endif

//cl test
//        { 
//          uint16_t i;
//          dbg( "TitanPacket", "Checking on packet from task %i on interface %i, port %i:\n", uiTaskID, uiID, uiOutPort );
//        
//          for (i=0; i<TITAN_MAX_FIFOS; i++ ) {
//            dbg("TitanPacket", "FIFO %i: %i -> %i %i\n", i, m_FIFOs[i].inTaskID, m_FIFOs[i].outTaskID, m_FIFOs[i].outPort );
//          }
//          for (i=0; i<TITAN_MAX_TASKS; i++ ) {
//            dbg("TitanPacket", "Task %i: Port map 0->%i 1->%i\n", i, m_TaskInfo[i].portMap[0], m_TaskInfo[i].portMap[1] );
//          }
//        }

    // Determine the FIFO
    uiTargetFIFO = m_TaskInfo[uiTaskID].portMap[uiOutPort];
    
    // find the packet and release the reserved flag
    // IDEA: this could be done without a loop by calculating the offset 
    //       of the flag from the packet pointer
//    for ( i=0; i<TITAN_FIFO_SIZE; i++ ) {
//      if ( &(m_FIFOs[uiTargetFIFO].slots[i].packet) == pPacket ) {
//        m_FIFOs[uiTargetFIFO].slots[i].reserved = 0;
//        break;
//      }
//    }

//    if ( i == TITAN_FIFO_SIZE ) {
//      dbg( DBG_USR3, "ERROR: Could not find reserved packet in queue\n" );
//    }

    // some small performance hack, does the same as the above
    //TODO: add checks to see whether the pointer is in reasonable bounds
    {
      TitanFIFOEntry * pFIFOEntry = (TitanFIFOEntry*)pPacket;
      pFIFOEntry->reserved = 0;
    }
    
    atomic {
      uiTargetTask = m_FIFOs[uiTargetFIFO].outTaskID;
      uiTargetPort = m_FIFOs[uiTargetFIFO].outPort;
    }
    
    pConfig = &(m_TaskInfo[uiTargetTask].config);

    dbg( "Titan", "FIFO %i: Sending packet to %i interface %i port %i\n", 
         uiTargetFIFO, uiTargetTask, m_TaskInfo[uiTargetTask].taskInterface, uiTargetPort );

    // notify the receiving task of the reception of the data
    signal Titan.packetAvailable[m_TaskInfo[uiTargetTask].taskInterface](pConfig,uiTargetPort);

    return SUCCESS;
  }
  
  async command error_t Titan.issueError[uint8_t uiID]( uint16_t iErrorID ) {
  
      // display error on led - red: error happened
//      call Leds.led0On();
		dbg("Titan","ERROR: Source %i, ID %i\n",uiID,iErrorID);
  
    if ( uiID < uniqueCount("Titan") ) {
      dbg("TitanPacket", "ERROR: Received issueError from task type %i with ID %i\n", m_TaskUIDs[uiID], iErrorID );
      signal TitanConfigure.issueError( m_TaskUIDs[uiID], iErrorID );
    } else {
      dbg("TitanPacket", "ERROR: Received issueError from UNKNOWN task interface %i with ID %i\n", uiID, iErrorID );
      signal TitanConfigure.issueError( -7, iErrorID );
    }

    return SUCCESS;
  }
  
  event error_t TitanMemory.issueError( uint16_t iErrorID ) {
  
    dbg("TitanPacket", "ERROR: Received issueError from Memory Management with ID %i\n", iErrorID );

    return SUCCESS;
  }

  /* ********************************************************************** */
  /* *                             Scheduling                             * */
  /* ********************************************************************** */
  
	async command error_t Titan.postExecution[uint8_t uiID]( TitanTaskConfig* pConfig, uint8_t uiPortID ) {
		return call TitanSchedule.postExecution(pConfig,uiPortID);
	}
	
	event void TitanSchedule.execute(TitanTaskConfig* pConfig, uint8_t uiPortID ) {
		TitanTaskInfo* pTaskInfo = (TitanTaskInfo*)pConfig;
    if ( m_bReconfiguring ) return; // don't execute any more tasks when reconfiguring
		return signal Titan.execute[pTaskInfo->taskInterface]( pConfig, uiPortID );
	}

  /* ********************************************************************** */
  /* *                           Memory handling                          * */
  /* ********************************************************************** */
  
  uint8_t uiTestCount;
  
  command void* Titan.allocMemory[uint8_t uiID](uint8_t uiTaskID, uint16_t uiSize) {
    uiTestCount = 0;
    return call TitanMemory.allocMemory(uiTaskID,uiSize);
  }
  
  command error_t Titan.freeMemory[uint8_t uiID](uint8_t uiTaskID, void* pData) {
    uiTestCount++;
    return call TitanMemory.freeMemory(uiTaskID,pData);
  }

  /* ********************************************************************** */
  /* *                       Configuration interface                      * */
  /* ********************************************************************** */

    task void resetAllTasks() {
      int i;
      dbg( "TitanConfig", "Reset task\n");
      
      // go through the list of tasks and let all terminate      
      for ( i=0; i < m_uiTasksRunning; i++ ) {
        
        signal Titan.terminate[m_TaskInfo[i].taskInterface]( &(m_TaskInfo[i].config) );

        //atomic m_TaskInfo[i].config.pTaskData = NULL;        
        atomic m_TaskInfo[i].config.taskType = -1;
      }
      
      // free the memory
      //CL:new call TitanMemory.freeAll();
      
      // initialize all structures
      initStructures(); //reset task may come after 1 packet config!!! This task is then too delayed

      // free the memory
      call TitanMemory.freeAll();

      atomic m_uiTasksRunning = 0;
      
      m_bReconfiguring = FALSE;
      signal TitanConfigure.resetConfigurationDone();
    }

  /**
   * immediateResetAllTasks() resets all structures without a task. This takes 
   * longer to process but guarantees everything is executed before the a single 
   * message reconfiguration has been completed.
   */
    
  void immediateResetAllTasks() {

      int i;
      dbg( "TitanConfig", "Immediate reset task\n");
      
      // go through the list of tasks and let all terminate      
      for ( i=0; i < m_uiTasksRunning; i++ ) {
        
        signal Titan.terminate[m_TaskInfo[i].taskInterface]( &(m_TaskInfo[i].config) );

        //atomic m_TaskInfo[i].config.pTaskData = NULL; // a task might still be scheduled - don't set this to NULL!
        atomic m_TaskInfo[i].config.taskType = -1;
      }
      
      // reinitialize all structures
      initStructures(); //CL: this is the interesting change
      
      // free the memory
      call TitanMemory.freeAll();
      
      atomic m_uiTasksRunning = 0;
    }

  command error_t TitanConfigure.resetConfiguration() {

    // don't allow any more tasks to be run
//    TOSH_clear_tasks();
    
    dbg( "TitanConfig", "Resetting configuration\n");
    m_bReconfiguring = TRUE; // this makes sure no task execution starts anymore until the reset task
    
    //immediateResetAllTasks(); // DANGEROUS: this might delete task contexes while they are running!
    
    
//    return SUCCESS;
    return post resetAllTasks();
  }
  
  command error_t TitanConfigure.isTaskConfigured( uint16_t uiTaskID, uint8_t uiRunID ) {
    return (m_TaskInfo[ uiRunID ].config.taskType == uiTaskID)? SUCCESS : FAIL;
  }
    
  command error_t TitanConfigure.processTaskConfig( uint16_t uiTaskID, uint8_t uiRunID, uint8_t uiDataLength, uint8_t *data ) {
    int i;
    
    dbg( "TitanConfig", "Configuring task %u, RunID %u\n", uiTaskID, uiRunID );
    
    // find the task type
    for ( i=0; i < uniqueCount("Titan"); i++ ) {
      if ( m_TaskUIDs[i] == uiTaskID ) break;
    }
    
    // check whether the task has been found
    if ( i == uniqueCount("Titan") ) {
      dbg( "TitanConfig", "ERROR: Task type %u not found!\n", uiTaskID );
      return FAIL;
    }
    
    // copy configuration
    atomic {
      m_TaskInfo[ uiRunID ].config.taskType = uiTaskID;
      m_TaskInfo[ uiRunID ].config.taskID   = uiRunID;
      m_TaskInfo[ uiRunID ].config.configLength = uiDataLength;
    
      for ( i=0; (i < uiDataLength) && (i < TITAN_MAX_CONFIG_LENGTH); i++ ) {
          m_TaskInfo[uiRunID].config.configData[i] = data[i];
      }
      
      for ( i=0; i < uniqueCount("Titan"); i++ ) {
        if ( m_TaskUIDs[i] == m_TaskInfo[uiRunID].config.taskType ) break;
      }
      m_TaskInfo[uiRunID].taskInterface = i;
      
    }
    
    // notify the task that there is new configuration information
    signal Titan.configure[ m_TaskInfo[uiRunID].taskInterface ]( &(m_TaskInfo[uiRunID].config) );
    
    // check settings
    
    // not more than TITAN_MAX_PORTS_PER_TASK allowed, except for communication and sink task
    if( (m_TaskInfo[uiRunID].config.taskType != TITAN_COMM_MODULE) && (m_TaskInfo[uiRunID].config.taskType != TITAN_TASK_SINK) &&
      (m_TaskInfo[uiRunID].config.inPorts + m_TaskInfo[uiRunID].config.outPorts  > TITAN_MAX_PORTS_PER_TASK) ) {
      call Titan.issueError[0](ERROR_CONFIG);
      dbg("TitanConfig", "ERROR: Task %i (%i) has too many ports (%i+%i of maximal allowed %i)\n", 
          uiRunID, m_TaskInfo[uiRunID].config.taskType,
          m_TaskInfo[uiRunID].config.inPorts, m_TaskInfo[uiRunID].config.outPorts, 
          TITAN_MAX_PORTS_PER_TASK);
          atomic {
            m_TaskInfo[uiRunID].config.inPorts  = 0;
            m_TaskInfo[uiRunID].config.outPorts = 0;
          }
      return FAIL;
    }
    
    return SUCCESS;
  }
    
  command error_t TitanConfigure.processConnection( uint8_t sourceTask, uint8_t sourcePort, 
                                                     uint8_t destTask, uint8_t destPort ) {

    int j;

    dbg( "TitanConfig", "Configuring connection (%i,%i) -> (%i,%i)\n", 
         sourceTask, sourcePort, destTask, destPort );
         
    atomic {
        // find a free FIFO
      for ( j=0; j<TITAN_MAX_FIFOS; j++ ) {
        if ( m_FIFOs[j].inTaskID == (uint8_t)-1 ) break;
      }
        
      // check whether a free FIFO has been found
      if ( j==TITAN_MAX_FIFOS ) {
        dbg( "TitanConfig", "TitanConfigure: Out of FIFOs\n" );
        return FAIL;
      }
        
        // hook up FIFO information
        m_FIFOs[j].inTaskID  = sourceTask;
        m_FIFOs[j].outTaskID = destTask;
        m_FIFOs[j].outPort   = destPort;
        
        dbg( "TitanConfig", "Setting FIFO %i: source: %i srcPort: %i dest: %i destPort: %i\n",
             j, sourceTask, sourcePort, destTask, destPort);
        
      // update port mappings
      atomic {
        m_TaskInfo[ sourceTask ].portMap[ sourcePort ] = j; // output
        m_TaskInfo[ destTask   ].portMap[ TITAN_MAX_PORTS_PER_TASK-1-destPort ] = j; // input
      }
    } // atomic
      return SUCCESS;
  }
  
  uint8_t m_TaskToStart;
  
  task void startupConfig() {
    int i;

    dbg( "TitanConfig", "Start task\n" );
#ifdef MEASURE_CYCLES
    P6OUT |=  BIT1;  // set on
#endif
      
    if ( m_uiTasksRunning != 0 ) {
      dbg( "TitanConfig", "WARNING: Some tasks are still running on startupConfig()\n");
    }

    atomic m_uiTasksRunning=0;
       
    // start all tasks
    for (i=0; i < m_TaskToStart; i++ ) {
      error_t result;
    
      // don't start tasks without ports
      if ( (m_TaskInfo[i].config.inPorts  == 0) && (m_TaskInfo[i].config.outPorts) == 0 ) {
        call Titan.issueError[0](ERROR_CONFIG);
        dbg("TitanConfig","ERROR: Attempt to start task without ports\n");
      }
    
      result = signal Titan.start[m_TaskInfo[i].taskInterface]( &(m_TaskInfo[i].config) );
      if ( result == FAIL ) {
        dbg( "TitanConfig", "ERROR: Task %i (type: %i) failed to start.\n", i, m_TaskInfo[i].config.taskType );
        call Titan.issueError[0](ERROR_CONFIG);
      }
      atomic m_uiTasksRunning++;
    }

#ifdef MEASURE_CYCLES
        P6OUT &= ~BIT1;  // set on
#endif
    dbg("TitanConfig","Start task done\n");
  }
    
  command int8_t TitanConfigure.startupConfiguration( uint8_t uiTasks ) {

    // set running again
    m_TaskToStart = uiTasks;
    
    post startupConfig();
        
    return SUCCESS;
  }
  
  command uint8_t TitanConfigure.getTaskTypes( TitanTaskUID** pTaskUIDs ) {
    if ( pTaskUIDs != NULL ) {
      *pTaskUIDs = m_TaskUIDs;
    }
    return uniqueCount("Titan");
  }

  /* ********************************************************************** */
  /* *                       Default event handlers                       * */
  /* ********************************************************************** */
  
  default event error_t Titan.configure[uint8_t uiID]( TitanTaskConfig* pConfig ) {
    return SUCCESS;
  }
  
  default event error_t Titan.terminate[uint8_t uiID]( TitanTaskConfig* pConfig ) {
    return SUCCESS;
  }
  
  default event error_t Titan.start[uint8_t uiID]( TitanTaskConfig* pConfig ) {
    return SUCCESS;
  }
  
  default event error_t Titan.stop[uint8_t uiID]( TitanTaskConfig* pConfig ) {
    return SUCCESS;
  }
  
  default async event error_t Titan.packetAvailable[uint8_t uiID]( TitanTaskConfig* pConfig, uint8_t uiPort ) {
    return SUCCESS;
  }

  default event TitanTaskUID Titan.getTaskUID[uint8_t uiID]( ) {
    return (TitanTaskUID)-1;
  }

  default event error_t Titan.init[uint8_t uiID]( ) {
    return SUCCESS;
  }

  default event void Titan.execute[uint8_t uiID]( TitanTaskConfig* pConfig, uint8_t uiParameter ) {
  }
}
