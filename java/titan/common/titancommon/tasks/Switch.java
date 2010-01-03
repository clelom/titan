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
 * Switch.java
 * 
 * @author Andreas Breitenmoser
 *
 * Switch controls an analog switch that can be used for various switching actions,
 * such as turning a lamp on.
 * 
 * It accepts 1 configuration byte (uint8_t).
 * 
 * Created: 29.01.2008
 * Last modified: 29.01.2008
 * 
 */
 
public class Switch extends Task {
    
    public final static String NAME   = "switch";
    public final static int    TASKID = 24;
    
    protected int m_uiNotInverted;
    
    public Switch() {}
    
    public Switch(Switch m) {
    	super(m);
        m_uiNotInverted = m.m_uiNotInverted;
    }
    
    public String getName() {
        return NAME.toLowerCase();
    }
    
    public int getID() {
        return TASKID;
    }
    
    public Object clone() {
        return new Switch(this);
    }
    
    public int getInPorts() {
        return 1;
    }
    
    public int getOutPorts() {
        return 0;
    }
    
    public boolean setConfiguration(String[] strConfig) {
      
      // argument must be an integer and 0<ARG0<256
      if ( strConfig == null || strConfig.length != 1 ) return false;
      
      m_uiNotInverted = Integer.parseInt(strConfig[0]);
      
      boolean bValid = true;
      bValid &= ((0 <= m_uiNotInverted) && (m_uiNotInverted < 256));

      return bValid;
    }
    
    public short[][] getConfigBytes(int maxBytesPerMsg) {
      short [][] config =  {{(short)m_uiNotInverted}};
      return config;
      }
    
    //////////////////////////////////////////////////////////////////////////
    // Evaluation functions

    //TODO: The metrics have not been evaluated for this task yet
    
    float m_frequency;
    
    /** 
     * @see eth.ife.wearable.Titan.Task.Task#getMetrics(NodeMetrics)
     */
    public TaskMetrics getMetrics(NodeMetrics nm){
        
        TaskMetrics tm = new TaskMetrics();
        tm.procCycles = (int) (m_uiNotInverted*360.1287);
        tm.datapackets = null;
        tm.packetsizes = null;
        tm.dynMemory = 5;
        tm.instantiations = m_frequency;
        tm.period = (int)(1/m_frequency);
        return tm;
    }

    /** 
     * @see eth.ife.wearable.Titan.Task.Task#setInputPortDatarate()
     */
    public boolean setInputPortDatarate( int port, float pktPerSecond, int pktSize ) {
        
        if ( m_frequency != pktPerSecond ) {
            m_frequency = pktPerSecond;
            return true;
        } else return false;
        
    }
}
