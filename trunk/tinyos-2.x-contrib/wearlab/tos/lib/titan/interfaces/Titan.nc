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
 * Titan.nc
 *
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *
 * Interface for a task to the Titan network. Any task for the Titan framework 
 * needs to implement these functions. Please copy TitanTaskTemplate.nc to 
 * create a task, as it implements the basic functionality.
 *
 *
 */

#include "Titan.h"
#include "TitanTaskUIDs.h"

interface Titan {

    /* ********************************************************************** */
    /* *                        Configuration operations                    * */
    /* ********************************************************************** */

    /** 
    * Is called one time during startup. Basic initialization, but nothing 
    * should be started.
    */
    event error_t init();

	/**
	* Returns the universal task identifier of the task. These identifiers are 
	* stored in TitanTaskUIDs.h and should be statically programmed.
	* @return Universal Task Identifier
	*/
	event TitanTaskUID getTaskUID();

	/**
	* Initializes a component and passes a configuration structure. In the pInOut 
	* structure, the task returns its configuration, ie. how many input and output 
	* ports it reserves according to the configuration received.
	* @param pConfig Pointer to the configuration structure for the task
	* @param pInOut  Pointer to the resulting input and output port configuration
	* @return Whether the task has successfully been initialized
	*/
	event error_t configure( TitanTaskConfig* pConfig );

	/**
	* Indicates that the task will be terminated. After this event has been 
	* received, the task should process no more data and free all resources 
	* allocated.
	* @return Whether the task has successfully been terminated.
	*/
	event error_t terminate( TitanTaskConfig* pConfig );

    /* ********************************************************************** */
    /* *                            Control operations                      * */
    /* ********************************************************************** */

	/**
	* Starts the operation of a task. This could also wake it up again.
	*/
	event error_t start( TitanTaskConfig* pConfig );
	
	/**
	* Stops the operation of a task. It should then prepare a sleep mode.
	*/
	event error_t stop( TitanTaskConfig* pConfig );


    /* ********************************************************************** */
    /* *                           Task Scheduling                          * */
    /* ********************************************************************** */

	/**
	 * Schedules a task for execution
	 * @param uiPortID Parameter that will be passed to the execution
	 * @return whether successful
	 */
	async command error_t postExecution( TitanTaskConfig* pConfig, uint8_t uiParameter );
	
	/**
	 * Starts the execution of a task.
	 * @param uiInterfaceID task to be executed
	 * @param uiPortID execution dependent parameter
	 */
	event void execute( TitanTaskConfig* pConfig, uint8_t uiParameter );

#ifdef lasdfnln

		if (call Titan.postExecution(pConfig,uiPort) == FAIL ) {
		  call Titan.issueError(ERROR_PACKET);
		  return FAIL;
		}

#endif
  
  
    /* ********************************************************************** */
    /* *                             Packet operations                      * */
    /* ********************************************************************** */

	/**
	* Is issued when a new packet arrives at the input port.
	* @param  iPort Port where the packet arrives
	* @return SUCCESS if the packet will be processed
	*/
	async event error_t packetAvailable( TitanTaskConfig* pConfig, uint8_t uiPort );

	/**
	* This command checks whether there is a packet available on the port, without 
	* actually retrieving it.
	* @param iPort Input port on which to check for packets.
	* @return number of available packets in the input FIFO
	*/
	command uint8_t hasPacket( uint8_t uiTaskID, uint8_t uiPort );

	/**
	* This retrieves a packet from the specified input port queue. The queue 
	* gives the slot free after calling this function.
	* 
	* NOTE: If the data contained in the packet should be accessible after the 
	*       task has ended, it _must_ be copied out of the message, as the 
	*       message queue frees the slot for a new message of another task.
	* 
	* @param iInPort Input port from which the packet should be consumed.
	* @return Pointer to a TitanPacket, NULL, if no new packet is available.
	*/
	async command TitanPacket* getNextPacket( uint8_t uiTaskID, uint8_t uiInPort );
	
	/**
	* Reserves a packet slot in the output buffer to be filled with data. 
	* use sendPacket() to actually transmit it.
	* 
	* <b>CAUTION</b>: Due to memory restrictions and as tasks may produce 
	*                 packets at a faster rate as they are consumed, a 
	*                 NULL return MUST be handled. If not, an error can 
	*                 be issued using the issueError() function.
	* 
	* @param iOutPort Output port where a packet will be sent.
	* @return Pointer to a free packet slot. NULL if none is free
	*/
	command TitanPacket* allocPacket( uint8_t uiTaskID, uint8_t uiOutPort );
	
	/**
	* Actually sends a packet allocated by allocNewPacket() to the next 
	* task to receive it. This data buffer should not be accessed any 
	* more.
	* @param iOutPort Port on which to send the packet. This MUST be the 
	*        same as used with allocNewPacket.
	* @return SUCCESS if the packet has been queued.
	*/
	command error_t sendPacket( uint8_t uiTaskID, uint8_t uiOutPort, TitanPacket* pPacket );


    /* ********************************************************************** */
    /* *                           Memory Management                        * */
    /* ********************************************************************** */

	/**
	* Allocates data of a specified size for a task. This should only be 
	* called during Titan.configure(). During Titan.terminate() all this 
	* data should be deallocated.
	* 
	* NOTE: It is recommened that structures are used to store data and 
	*       a sizeof() result is passed to this function.
	* 
	* @param  uiSize Size in bytes to allocate
	* @return A pointer to the data range if successfull.
	*/
	command void* allocMemory( uint8_t uiTaskID, uint16_t uiSize );


	/**
	* Frees data previously allocated by Titan.allocMemory(). This function 
	* should only be called during Titan.terminate().
  * CAUTION: do not set the pTaskData to NULL after having freed it! There 
  *          the run() task might still be running.
  *
	* @param  pData Pointer to the data to be deallocated.
	* @return Whether successful
	*/
	command error_t freeMemory( uint8_t uiTaskID, void* pData );
	

    /* ********************************************************************** */
    /* *                             Error handling                         * */
    /* ********************************************************************** */

	/**
	* Allows to issue an error indicator. This will be passed to the control 
	* of the Titan network to handle it, or display to the user.
	*/
	async command error_t issueError( uint16_t iErrorID );

}
