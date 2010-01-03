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
 * Voice.java
 * 
 * @author Andreas Breitenmoser
 *
 * Voice controls the text-to-speech voice synthesiser module V-Stamp.
 * It accepts up to 4 configuration bytes (uint8_t), the first byte defines
 * the application to run and must always be set.
 * 
 * Created: 22.01.2008
 * Last modified: 22.01.2008
 * 
 */
 
public class Voice extends Task {
    
    public final static String NAME   = "voice";
    public final static int    TASKID = 22;
    
    protected int m_uiSelApp;
    protected int m_uiVolume;
    protected int m_uiVoice;
    protected int m_uiSpeed;
    
    public Voice() {}
    
    public Voice(Voice m) {
    	super(m);
        m_uiSelApp = m.m_uiSelApp;
        m_uiVolume = m.m_uiVolume;
        m_uiVoice = m.m_uiVoice;
        m_uiSpeed = m.m_uiSpeed;
    }
    
    public String getName() {
        return NAME.toLowerCase();
    }
    
    public int getID() {
        return TASKID;
    }
    
    public Object clone() {
        return new Voice(this);
    }
    
    public int getInPorts() {
        return 1;
    }
    
    public int getOutPorts() {
        return 0;
    }
    
    public boolean setConfiguration(String[] strConfig) {
      
      if ( strConfig == null ) return false;
      
      // parse data
      switch (strConfig.length) {
        case 4:  m_uiSpeed = Integer.parseInt(strConfig[3]);
        case 3:  m_uiVoice = Integer.parseInt(strConfig[2]);
        case 2:  m_uiVolume = Integer.parseInt(strConfig[1]);
        case 1:  m_uiSelApp = Integer.parseInt(strConfig[0]);
                 break;
        default: return false;
      }
      
      boolean bValid = true;
      
      bValid &= ((0 <= m_uiSpeed) && (m_uiSpeed < 14));
      bValid &= ((0 <= m_uiVoice) && (m_uiVoice < 11));
      bValid &= ((0 <= m_uiVolume) && (m_uiVolume < 10));
      bValid &= ((0 <= m_uiSelApp) && (m_uiSelApp < 256));
      
      return bValid;
    }
    
    public short[][] getConfigBytes(int maxBytesPerMsg) {
      short [][] config =  {{(short)m_uiSelApp }}; //, (short)m_uiVolume, (short)m_uiVoice, (short)m_uiSpeed}};
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
        tm.procCycles = 6;
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
