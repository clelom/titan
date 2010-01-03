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

package titancommon.messages;

/**
 * This class is used by TitanCommand to disseminate messages received from the 
 * network. TitanCommand filters unexpected messages and formats messages as 
 * available in titan.messages.* to be sent over this interface.
 * 
 * Please use addObserver and deleteObserver from the superclass to register and 
 * deregister message recipicents.
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */
public class MessageDispatcher extends java.util.Observable {
    /**
     * Submit message to be delivered to all registered recipicents.
     */
    public void sendMessage(Object obs) {
        setChanged();
        this.notifyObservers( obs );
        clearChanged();
    }
}
