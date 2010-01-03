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
 *
 * Interface for application services within Titan. Applications receive 
 * results from the WSN and can reconfigure the network.
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * @date 12 July 2008
 * 
 */

package titancommon.applications;

import java.util.Observer;

import titan.TitanCommunicate;
import titancommon.messages.MessageDispatcher;
import titancommon.services.ServiceDirectory;

public interface Application extends Observer {
	
	/** Starts the application and delivers the most important objects to 
	 *  connect to the WSN
	 * @param com Communication object to access the network
	 * @param sd Service directory containing information about the network
	 * @param dispatcher Network message dispatcher. Register there to get messages from the WSN.
	 * @return
	 */
	public boolean startApplication(TitanCommunicate com, ServiceDirectory sd, MessageDispatcher dispatcher);
	
	/** Human readable application name for registration. */
	public String getName();
	
	/** Returns the Unique Service ID of the application */
	public int getUSID();
}
