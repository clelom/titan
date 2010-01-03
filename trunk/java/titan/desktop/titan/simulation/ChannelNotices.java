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
 * This notifies listeners to a channel of new text coming in.
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */
package titan.simulation;

import java.util.Observable;

public class ChannelNotices extends Observable {
    
    public class NoticeInfo {
        public String strChannel;
        public String strMessage;
        public NoticeInfo(String channel,String msg) {
            strChannel = channel;
            strMessage = msg;
        }
    }

    public void notifyObservers( String strChannel, String strMessage ) {
        setChanged();
        NoticeInfo ni = new NoticeInfo(strChannel,strMessage);
        this.notifyObservers(ni);
        clearChanged();
    }
}