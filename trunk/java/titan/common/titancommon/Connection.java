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

package titancommon;

/**
 * This class stores the data associated with interconnections in the Titan 
 * task network.
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *
 */
public class Connection {
    
    public int StartTask;
    public int StartPort;
    public int EndTask;
    public int EndPort;
    
    public Connection(int iStartTask,int iStartPort,int iEndTask,int iEndPort){
        StartTask = iStartTask;
        StartPort = iStartPort;
        EndTask = iEndTask;
        EndPort = iEndPort;
    }
    
    public Object clone() {
    	return new Connection(StartTask, StartPort,EndTask,EndPort);
    }
    
}
