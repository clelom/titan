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
 * Sets radio settings for the TOSSIM simulation
 * 
 * @author Urs Hunkeler <urs.hunkeler@epfl.ch>
 *
 */

package titan.simulation;

public class Radio {
    private static int ID = 0;
    
    private String id = "r" + (ID++);
    private Tossim tossim = null;
    
    protected Radio(Tossim tossim) {
        this.tossim = tossim;
    }
    
    public String getVarID() {
        return id;
    }
    
    public void add(Mote m1, Mote m2, float gain) {
        String ret = tossim.runFunc(id + ".add(" + m1.getMoteID() + ", " + m2.getMoteID() + ", " + gain + ")");
        if(ret != null) {
            throw new RuntimeException("Unexpected return value: " + ret);
        }
    }
    
    public void setNoise(Mote m, float mean, float variance) {
        String ret = tossim.runFunc(id + ".setNoise(" + m.getMoteID() + ", " + mean + ", " + variance + ")");
        if(ret != null) {
            throw new RuntimeException("Unexpected return value: " + ret);
        }
    }
}
