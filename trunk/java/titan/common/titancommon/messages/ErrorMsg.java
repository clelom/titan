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
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */


public class ErrorMsg {
    public short nodeID;
    public short configID;
    public short errSource;
    public short errType;
    public ErrorMsg(short nodeID, short configID, short errSource, short errType) {
        this.nodeID = nodeID;
        this.configID = configID;
        this.errSource = errSource;
        this.errType = errType;
    }
}
