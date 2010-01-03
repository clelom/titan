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
 * Handles a mote during simulation time
 * 
 * @author Urs Hunkeler <urs.hunkeler@epfl.ch>
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *
 */

package titan.simulation;

public class Mote {
    private static int ID = 0;
    
    private String id = "m" + (ID++);
    private int moteID = 0;
    private Tossim tossim = null;
    
    protected Mote(Tossim tossim, int moteID) {
        this.tossim = tossim;
        this.moteID = moteID;
    }
    
    public int getMoteID() {
        return moteID;
    }
    
    public String getVarID() {
        return id;
    }
    
    public void bootAtTime(long time) {
        String ret = tossim.runFunc(id + ".bootAtTime(" + time + ")");
        if(ret != null) {
            throw new RuntimeException("Unexpected return value: " + ret);
        }
    }
    
    public boolean isOn() {
        boolean b = false;
        String ret = tossim.runFunc(id + ".isOn()");
        if(ret == null) {
            throw new RuntimeException("Unexpected return value: null");
        }
        ret = ret.trim();
        if(ret.equalsIgnoreCase("true") || ret.equals("1")) {
            b = true;
        } else if(ret.equalsIgnoreCase("false") || ret.equals("0")) {
            b = false;
        } else {
            throw new RuntimeException("Unexpected return value: " + ret);
        }
        
        return b;
    }
    
    public void turnOff() {
        String ret = tossim.runFunc(id + ".turnOff()");
        if(ret != null) {
            throw new RuntimeException("Unexpected return value: " + ret);
        }
    }
}
