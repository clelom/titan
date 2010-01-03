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
  * TitanSchedule.nc
  *
  * Interface for the Titan scheduler postExecution() and execute() are 
	* forwarded to this module
  *
  * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
  */

#include "Titan.h"
	
interface TitanSchedule {

	/**
	 * Schedules a task for execution
	 * @param uiTaskID task to be scheduled.
	 * @return whether successful
	 */
	async command error_t postExecution( TitanTaskConfig* pConfig, uint8_t uiPortID );
	
	/**
	 * Starts the execution of a task.
	 * @param uiInterfaceID task to be executed
	 * @param uiPortID execution dependent parameter
	 */
	event void execute( TitanTaskConfig* pConfig, uint8_t uiPortID );

	/**
	 * Cancels the execution of all tasks
	 */
	command void clearAll();
}

