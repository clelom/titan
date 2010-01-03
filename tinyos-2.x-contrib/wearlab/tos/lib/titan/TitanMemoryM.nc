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
  * TitanMemoryM.nc
  *
  * Titan memory management.
  *
  * TODO: first memory block cannot be removed unless with freeAll()
  * TODO: Align first byte of assigned memory at a 16bit border for 
  *       more performance
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  */

#define TITANMEMORY_DO_CHECKS

#define TM_CHECK_PATTERN 0x55

// error messages
#define TM_MEMORY_CORRUPTION 1
#define TM_MEMORY_FULL 2

#include "TitanInternal.h"

module TitanMemoryM {

	provides interface TitanMemory;

}

implementation {

	uint8_t  m_TaskMemory[TITAN_TASK_MEMORY_SIZE];
	uint8_t* m_pFreeMemory;
	TitanMemCheckBlock* m_pLastBlock;

	command error_t TitanMemory.init() {
		m_pFreeMemory = m_TaskMemory;
		m_pLastBlock = NULL;
		return SUCCESS;
	} 

	command void* TitanMemory.allocMemory(uint8_t uiTaskID, uint16_t uiSize) {

		// check whether enough memory is here
		if ( TITAN_TASK_MEMORY_SIZE - (m_pFreeMemory - m_TaskMemory) - sizeof(TitanMemCheckBlock) <= 0 ) {
			signal TitanMemory.issueError(TM_MEMORY_FULL);
			return NULL;
		}
	
		if ( m_pLastBlock == NULL ) {
			m_pLastBlock = (TitanMemCheckBlock*)m_pFreeMemory;
			
		} else {
		
			TitanMemCheckBlock* pNewLastBlock = (TitanMemCheckBlock*)m_pFreeMemory;

#ifdef TITANMEMORY_DO_CHECKS
			
			if ( m_pLastBlock->checkPattern != TM_CHECK_PATTERN ) {
				dbg( "TitanMemory", "ERROR: While allocating memory for task %i: CheckPattern invalid\n", uiTaskID );
				signal TitanMemory.issueError(TM_MEMORY_CORRUPTION);
				return NULL;
			} else if ( m_pLastBlock->pNext != NULL ) {
				dbg( "TitanMemory", "ERROR: While allocating memory for task %i: last block -> pNext invalid\n", uiTaskID );
				signal TitanMemory.issueError(TM_MEMORY_CORRUPTION);
				return NULL;
			}
			
#endif

			m_pLastBlock->pNext = pNewLastBlock;
			m_pLastBlock = pNewLastBlock;
			
		}

		m_pLastBlock->checkPattern = TM_CHECK_PATTERN;
		m_pLastBlock->ownerTaskID  = uiTaskID;
		m_pLastBlock->pNext        = NULL;
		m_pFreeMemory += uiSize + sizeof(TitanMemCheckBlock);
		
		dbg("TitanMemory", "Task %i reserved %i Bytes. Total Memory usage: %i \n", 
			uiTaskID, uiSize, m_pFreeMemory - m_TaskMemory );
	
		return (void*)m_pLastBlock + sizeof(TitanMemCheckBlock);
	}
	
	command error_t TitanMemory.freeMemory(uint8_t uiTaskID, void* pData) {
	
#ifndef TITANMEMORY_DO_CHECKS
		// before allocating new data, a freeAll() is called anyway
		return SUCCESS;
#else
		// search for the corresponding memory block and delete it.
		int i=0;
		TitanMemCheckBlock* pCurBlock = (TitanMemCheckBlock*)m_TaskMemory;
		TitanMemCheckBlock* pLastBlock = NULL;
		
		// never allocated any memory?
		if ( m_pLastBlock == NULL ) {
			return SUCCESS;
		}
		
		for ( i=0; pCurBlock != NULL; i++ ) {
		
			// overwritten?
			if ( pCurBlock->checkPattern != TM_CHECK_PATTERN ) {
				dbg( "TitanMemory", "ERROR: While freeing memory for task %i: CheckPattern invalid at block %i\n", uiTaskID, i );
				signal TitanMemory.issueError(TM_MEMORY_CORRUPTION);
				return FAIL;
			} 
			// out of bounds?
			else if ( ((uint8_t*)pCurBlock < m_TaskMemory) || (m_TaskMemory + TITAN_TASK_MEMORY_SIZE < (uint8_t*)pCurBlock ) ) {
				dbg( "TitanMemory", "ERROR: While freeing memory for task %i: pointer out of bounds after block %i\n", uiTaskID, i );
				signal TitanMemory.issueError(TM_MEMORY_CORRUPTION);
				return FAIL;
			}
			
			// check whether this is the block we are looking for
			if ( pData == ((void*)pCurBlock) + sizeof(TitanMemCheckBlock) ) {
			
				// first block can't be deleted - this should be solved
				if ( pLastBlock == NULL ) {
					return SUCCESS;
				} else {
					// remove block out of the list
					pLastBlock->pNext = pCurBlock->pNext;
					return SUCCESS;
				}
			
			}
			
			pLastBlock = pCurBlock;
			pCurBlock = pCurBlock->pNext;

		} // while
		
		dbg( "TitanMemory", "ERROR: While freeing memory for task %i: data pointer not found!\n" );
		signal TitanMemory.issueError(TM_MEMORY_CORRUPTION);
		
		return FAIL;
#endif

	}

	command error_t TitanMemory.freeAll() {
	
		m_pFreeMemory = m_TaskMemory;
		m_pLastBlock  = NULL;

		return SUCCESS;
	}

}
