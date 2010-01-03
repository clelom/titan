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
  * TitanSchedulerM.nc
  *
  * Schedules Titan's tasks to be executed
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  */

module TitanSchedulerM {

	provides interface TitanSchedule;

}

implementation {

  int16_t m_iNextItem = -1; // next task in the queue to be executed, -1 if there is none
  int16_t m_iFreeItem = 0;  // next free spot in the queue, -1 if queue is full
  
  TitanTaskConfig* m_configQueue[TITAN_TASK_CONTEXT_QUEUE_LENGTH];
  uint16_t         m_paramQueue[TITAN_TASK_CONTEXT_QUEUE_LENGTH];
  
  /**
   *
   * Task scheduling the tasks of the 
   *
   */
  task void runNext() {

    int16_t iNext;
    TitanTaskConfig* pConfig;
    uint16_t         uiParam;

    // post next in line
    atomic if ( m_iNextItem == -1 ) return;
    
    dbg("TitanScheduler", "Executing task %i (queue state: next=%i free=%i)\n", m_configQueue[m_iNextItem]->taskID, m_iNextItem, m_iFreeItem );
    atomic {
      pConfig = m_configQueue[m_iNextItem];
      uiParam = m_paramQueue[m_iNextItem];
    };
    signal TitanSchedule.execute( pConfig, uiParam );

    atomic {
      // determine next
      iNext = (m_iNextItem+1>= TITAN_TASK_CONTEXT_QUEUE_LENGTH)? 0 : m_iNextItem+1; // next queue position
      if ( m_iFreeItem == -1 ) m_iFreeItem = m_iNextItem; // this spot has just been executed, get to next item
      m_iNextItem = ( iNext == m_iFreeItem )? -1 : iNext;
      
      dbg("TitanScheduler", "Execution done   (queue state: next=%i free=%i)\n", m_iNextItem, m_iFreeItem );


      if ( m_iNextItem != -1 ) post runNext();
    }
  }


	async command error_t TitanSchedule.postExecution( TitanTaskConfig* pConfig, uint8_t uiParameter ) {
    
    dbg("TitanScheduler", "Posting   task %i (queue state: next=%i free=%i)\n", pConfig->taskID, m_iNextItem, m_iFreeItem );
    
    atomic {
      // check if queue is full
      if ( m_iFreeItem == -1 ) return FAIL;
      
      // store data
      m_configQueue[m_iFreeItem] = pConfig;
      m_paramQueue[m_iFreeItem]  = uiParameter;
      
      // if the next item is on empty queue, start it now
      if ( m_iNextItem == -1 ) m_iNextItem = m_iFreeItem;

      // update pointers
      m_iFreeItem = (m_iFreeItem+1 >= TITAN_TASK_CONTEXT_QUEUE_LENGTH)? 0 : m_iFreeItem+1;
      
      // check whether the queue is overflowing
      if ( m_iFreeItem == m_iNextItem ) m_iFreeItem = -1;
    }
    
    post runNext();
    
		return SUCCESS;
	}
	
	command void TitanSchedule.clearAll() {
    m_iNextItem = -1;
    m_iFreeItem = 0;
	}


}
