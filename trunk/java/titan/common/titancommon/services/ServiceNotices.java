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

package titancommon.services;

import java.util.Observable;

/**
 * Notifies interested components about a node popping up or perishing.
 *
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */

public class ServiceNotices extends Observable {
    
    /** Reasons for notification */
    //public enum NoticeReason { NODE_ADDED,  };
    
    public class NoticeReason {
		public final static int NODE_ADDED =1;
		public final static int NODE_REMOVED=2;
	}
    
    /**
     * This class identifies the notification reason and which node 
     * issued this information.
     *
     * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
     * @author Tonio Gsell <tgsell@ee.ethz.ch>
     *
     */
    public class NoticeInfo {
        public int reason;  ///< reason for the notification
        public int address; ///< address of the node the notification is about
        
        public NoticeInfo( int r, int addr ) { reason = r; address = addr; }
    }
    
    /**
     * Notifies all observers with the given reason
     * @param address  Which node is it?
     * @param reason   What happened to the node
     */
    public void notifyObservers(int address, int reason) {
        
    	setChanged();
        // must use an object
        NoticeInfo ni = new NoticeInfo( reason, address );
        this.notifyObservers( ni );
        clearChanged();
    }
    
}
