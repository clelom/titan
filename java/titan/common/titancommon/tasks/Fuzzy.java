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

import titancommon.compiler.NodeMetrics;
import titancommon.compiler.TaskMetrics;

/**
 * Fuzzy.java
 * 
 * @author Andreas Breitenmoser
 *
 * Makes decisions in function of the input values. The precise decision rule
 * must be adjusted for the specific application.
 * As input 8 bit data is expected, 16 bit value is output. 
 * 
 * Created: 21.02.2008
 * Last modified: 21.02.2008
 * 
 */

public class Fuzzy extends Task {
    
    public final static String NAME   = "fuzzy";
    public final static int    TASKID = 28;
    
    protected int uiSelApp;
    
    public Fuzzy() {}
    
    public Fuzzy( Fuzzy m ) {
    	super(m);
        uiSelApp = m.uiSelApp;
    }
    
    public String getName() {
        return NAME.toLowerCase();
    }
    
    public Object clone() {
        return new Fuzzy(this);
    }
    
    public int getInPorts() {
        return 1;
    }
    
    public int getOutPorts() {
        return 1;
    }
    
    public boolean setConfiguration(String[] strConfig) {
        
      // argument must be an integer and 0<ARG0<256
      if ( strConfig == null || strConfig.length != 1 ) return false;
      
      uiSelApp = Integer.parseInt(strConfig[0]);
      
      return ((0 < uiSelApp) && (uiSelApp < 256));
    }
      
    public int getID() {
        return TASKID;
    }
    
    public short[][] getConfigBytes(int maxBytesPerMsg) {
      
      short [][] config =  {{ (short)uiSelApp }};
      return config;
    }
    
    
    //////////////////////////////////////////////////////////////////////////
    // Evaluation functions
    
    //TODO: The metrics have not been evaluated for this task yet

    float m_pktPerFuzzy;
    
    /** 
     * @see eth.ife.wearable.Titan.Task.Task#getMetrics(NodeMetrics)
     */
    public TaskMetrics getMetrics(NodeMetrics nm){
        
        TaskMetrics tm = new TaskMetrics();
        tm.procCycles = (int)(uiSelApp*360.1287);
        tm.datapackets = new float [] { m_pktPerFuzzy };
        tm.packetsizes = new int [] {2};
        tm.dynMemory = uiSelApp + 1;
        tm.instantiations = m_pktPerFuzzy;
        tm.period = (int)(1/m_pktPerFuzzy);
        return tm;
    }

    /** 
     * @see eth.ife.wearable.Titan.Task.Task#setInputPortDatarate()
     */
    public boolean setInputPortDatarate( int port, float pktPerSecond, int pktSize ) {
        
        float packetsNeededForFuzzy = pktPerSecond*pktSize;
        
        if ( m_pktPerFuzzy != packetsNeededForFuzzy ) {
            m_pktPerFuzzy = packetsNeededForFuzzy;
            return true;
        } else return false;
        
    }
    
}
