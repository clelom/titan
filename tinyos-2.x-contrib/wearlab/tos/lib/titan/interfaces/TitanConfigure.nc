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
 * TitanConfigure.nc
 *
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *
 * Interface between TitanCommM and TitanM. Configures the Titan Task Network 
 * handled by TitanM.
 *
 */

#include "TitanTaskUIDs.h"

interface TitanConfigure {

  /** 
  * Stops all running tasks and prepares for a reconfiguration of the network.
  * @return whether successful
  */
  command error_t resetConfiguration();
  
  /** 
  * Reports the completion of resetConfiguration()
  * @return whether successful
  */
  event error_t resetConfigurationDone();
  
  
  /**
  * Checks whether this task has previously received a configuration structure
  * @return SUCCESS if it has been configured, FAIL if not
  */
  command error_t isTaskConfigured( uint16_t uiTaskID, uint8_t uiRunID );
  
  /**
  * Stores the configuration data for a task configuration. This does not yet 
  * initialize a task.
  * @return whether successful
  */
  command error_t processTaskConfig( uint16_t uiTaskID, uint8_t uiRunID, uint8_t length, uint8_t *data );
  
  /**
  * This configures a connection
  * @param sourceTask source task runtime ID
  * @param sourcePort source task output port
  * @param destTask   destination task runtime ID
  * @param destPort   destination task runtime ID
  * @return whether successful
  */
  command error_t processConnection( uint8_t sourceTask, uint8_t sourcePort, 
                                      uint8_t destTask, uint8_t destPort );
  
  /**
  * Executes the configuration and starts up the system.
  * @param  uiTasks Number of tasks that have been configured
  * @return zero if successful, negative on error, positive on warnings
  */
  command int8_t startupConfiguration( uint8_t uiTasks );
  
  /**
  * Retrieves a list of universal identifiers of the tasks registered at this 
  * node.
  * @param pTaskUIDs A pointer to be set to the beginning of the TaskUID list
  * @return The number of TaskUIDs behind pTaskUID
  */
  command uint8_t getTaskTypes( TitanTaskUID** pTaskUIDs );
  
  /**
  * Reports an error from a task or the running configuration.
  * @param  uiSource Source of the error
  * @param  uiError  error type 
  * @return SUCCESS
  */
  async event error_t issueError( uint8_t uiSource, uint8_t uiError );
  
  #define TC_WARNING_FREEPORTS 1

}
