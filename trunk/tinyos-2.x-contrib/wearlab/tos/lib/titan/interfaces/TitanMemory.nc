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
  * TitanMemory.nc
  *
  * Interface for the Titan memory management. allocMemory() and freeMemory() 
  * are forwarded to the tasks by TitanMemoryM.
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  */

interface TitanMemory {

	/**
	 * Initializes the memory handling
	 * @return whether successful
	 */
	command error_t init();

	/**
	 * Allocates uiSize Bytes of memory for the task with ID uiOwner
	 * @param uiOwner TaskID of the allocating task
	 * @param uiSize  Size in bytes requested
	 * @return Pointer to the data area, or NULL if failed
	 */
	command void*    allocMemory( uint8_t uiOwner, uint16_t uiSize );

	/**
	 * Frees the memory previously allocated by allocMemory()
	 * @param uiOwner TaskID of the freeing task
	 * @return whether successful
	 */
	command error_t freeMemory( uint8_t uiOwner, void* pData );
	
	/**
	 * Frees all memory, all allocations are removed
	 * @return whether successful
	 */
	command error_t freeAll( );
	
	/**
	 * Called by the memory management when an error occurs. Refer to 
	 * TitanMemoryM.nc for a decoding of uiErrorID.
	 */
	event error_t issueError( uint16_t uiErrorID );

}
