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

import titancommon.*;
import titancommon.compiler.NodeMetrics;
import titancommon.compiler.TaskMetrics;

/**
 * LightSensor.java
 * 
 * @author Andreas Breitenmoser
 *
 * LightSensor samples the light sensor on the node with a given sample rate.
 * 
 * Created: 23.01.2008
 * Last modified: 23.01.2008
 */

public class LightSensor extends Task {
    
    public final static String NAME   = "lightsensor";
    public final static int    TASKID = 23;
    
    public int m_iSamplerate;
    public int m_iSamplesPerPacket;
    
    
    public LightSensor() {}
    
    public LightSensor(LightSensor ls) {
    	super(ls);
        m_iSamplerate       = ls.m_iSamplerate;
        m_iSamplesPerPacket = ls.m_iSamplesPerPacket;
    }
    

    public String getName() {
        return NAME.toLowerCase();
    }
    

    public int getID() {
        return TASKID;
    }
    
    
    public Object clone() {
        return new LightSensor(this);
    }
    

    public int getInPorts() {
        return 0;
    }
    

    public int getOutPorts() {
        return 1;
    }
    
    
    public boolean setConfiguration(String[] strConfig) {
        
        if ( strConfig == null ) return false;
        
        // parse data
        if ( strConfig.length == 1 ) {
            m_iSamplerate       = Integer.parseInt(strConfig[0]);
            m_iSamplesPerPacket = 1;
        } else if ( strConfig.length == 2 ) {
            m_iSamplerate       = Integer.parseInt(strConfig[0]);
            m_iSamplesPerPacket = Integer.parseInt(strConfig[1]);
        } else return false;
        
        boolean bValid = true;
        
        bValid &= ((0 < m_iSamplerate) && (m_iSamplerate < 65000));
        bValid &= ((0 < m_iSamplesPerPacket) && (m_iSamplesPerPacket*2 < TitanCommand.TITAN_PACKET_SIZE));
        
        return bValid;
    }
    
    
    public short[][] getConfigBytes(int maxBytesPerMsg) {
        
        if ( m_iSamplesPerPacket != 1 ) {
            short[][] config = {{ (short)((m_iSamplerate>>8)&0xFF), (short)(m_iSamplerate&0xFF), (short)m_iSamplesPerPacket }};
            return config;
        } else if (m_iSamplerate > 255 ){
            short[][] config = {{ (short)((m_iSamplerate>>8)&0xFF), (short)(m_iSamplerate&0xFF) }};
            return config;
        } else {
            short[][] config = {{ (short)(m_iSamplerate&0xFF) }};
            return config;
        }
        
    }

    //////////////////////////////////////////////////////////////////////////
    // Evaluation functions
    
    // TODO: The metrics have not been evaluated for this task yet

    /** 
     * @see eth.ife.wearable.Titan.Task.Task#getMetrics(NodeMetrics)
     */
    public TaskMetrics getMetrics(NodeMetrics nm){
         
        TaskMetrics tm = new TaskMetrics();
        tm.procCycles = m_iSamplerate*5300;
        tm.datapackets = new float [] {(float)m_iSamplerate / (float)m_iSamplesPerPacket };
        tm.packetsizes = new int [] { m_iSamplesPerPacket*2 }; // 3 axes
        tm.dynMemory = 9 + m_iSamplesPerPacket*2;
        tm.instantiations = (float)m_iSamplesPerPacket/(float)m_iSamplerate;
        tm.period = m_iSamplerate;
        // this is correct for low samplerates, for high ones, it might be better to keep the sensor running all the time 
        tm.sensorEnergy = nm.powerAccelerometer*nm.timeAccelerometer*m_iSamplerate; 
        return tm;
    }


    public boolean setInputPortDatarate( int port, float pktPerSecond, int pktSize ) {
        System.err.println("Error: Task LightSensor: received input port datarate! No input port exists here!");
        return false; 
    }

}
