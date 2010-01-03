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

package titancommon.tasks;

/**
 * This tasks splits an incoming packet up into several outgoing packets
 * each incoming short-element (of the sdata-array) is splitted into a spearate packet
 * please note, that the short-value in each outgoing packet is then again splitted into lower and higher bytes.
 *
 * the first element of the array will generate an output packet on port 0
 * the second ... on port 1
 * and so on
 *
 * @author Benedikt Köppel <bkoeppel@ee.ethz.ch>
 * @author Jonas Huber <huberjo@ee.ethz.ch>
 * 
 *
 */
public class Splitter extends Task {
    
    public final static String NAME   ="splitter";
    public final static int    TASKID = 41;
    
    protected short m_iOutports=0;
    
    public Splitter() {};
    
    public Splitter(Splitter d ) {
    	super(d);
        m_iOutports = d.m_iOutports;
    }
    
    public String getName() {
        return NAME;
    }
    
    public int getID() {
        return TASKID;
    }
    
    public Object clone() {
        return new Splitter(this);
    }
    
    public int getInPorts() {
        return 1;
    }
    
    public int getOutPorts() {
        return m_iOutports;
    }
    
    /**
     * the first parameter must be a number, and define up to how many output 
     * ports should be used (X).
     * if the incoming array has more elements than that, only the first X elements
     * will generate the output
     * if the incoming array has less elements, all elements are sent but no packages 
     * are sent on the X+1 and following ports.
     * 
     */
    public boolean setConfiguration(String[] strConfig) {
       if ( strConfig.length == 1 ) {
           m_iOutports = Short.parseShort(strConfig[0]);
           //System.out.println("m_iOutports is " + m_iOutports);
           return true;
       } else {
           return false;
       }
    }

    public short[][] getConfigBytes(int maxBytesPerMsg) {
        short[][] config = new short[1][1];
        config[0][0] = m_iOutports;
        //System.out.println("config[0][0] is " + config[0][0]);
        return config;
    }


}