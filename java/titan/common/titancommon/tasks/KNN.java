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
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *
 * k-NN classifier for varying input vector size
 * 
 */
public class KNN extends Task {
    
    public final static String NAME   = "knn";
    public final static int    TASKID = 19;

    protected int m_k; // number of neighbors kept for the majority vote
    protected int m_n; // dimensionality: number of features taken into consideration
    protected int m_options; // 2 LSB:  00: 8 bit unsigned, 01: 16 bit unsigned, 10 8 bit signed, 11 16 bit signed
    protected short [] m_data;
    
    public KNN() {}
    
    public KNN( KNN m ) {
    	super(m);
        m_k = m.m_k;
        m_n = m.m_n;
        m_options = m.m_options;
        
        if ( m.m_data != null ) {
            m_data = new short[m.m_data.length];
            for(int i=0; i<m.m_data.length; i++) {
                m_data[i] = m.m_data[i];
            }
        }
    }
    
    public String getName() {
        return NAME.toLowerCase();
    }
    
    public Object clone() {
        return new KNN(this);
    }
    
    public int getInPorts() {
        return m_n;
    }
    
    public int getOutPorts() {
        return 1;
    }
    
    //TODO: last two bytes seem not to get through!
    public boolean setConfiguration(String[] strConfig) {
        
        // argument must be an integer and 0<ARG0<256
        if ( strConfig == null || strConfig.length < 5 ) return false;
        
        // read settings
        m_k = Integer.parseInt(strConfig[0]);
        m_n = Integer.parseInt(strConfig[1]);
        m_options = Integer.parseInt(strConfig[2]);
        
        // check whether the rest is ok
        if (((strConfig.length-3) % (m_n+1)) != 0 ) {
            System.err.println("Task k-NN: Invalid configuration size");
            return false;
        }
        
        // check whether we are working with 8 or 16 bit values
        if ((m_options&0x1) == 1 ) {
            int index = 0;
            
            // allocate data array
            m_data = new short[(strConfig.length-3)/(m_n+1)*(2*m_n+1)];
            for (int i=3; i<strConfig.length;i++) {
                short curValue = Short.parseShort(strConfig[i]);
                
                // class is always sent as 8 bit value
                if ( (i-3) % (m_n+1) != m_n ) {
                    m_data[index++] = (short)(curValue&0x0FF);
                    m_data[index++] = (short)(((curValue&0x0FF00)>>8));
                } else {
                    m_data[index++] = curValue;
                }
            }
        } else {
            
            // for 8 bit, everything is just a simple copy
            m_data = new short[strConfig.length-3];
            for (int i=3; i<strConfig.length;i++) {
                m_data[i-3] = (short)Short.parseShort(strConfig[i]);
            }
        }
        
        return true;
    }
    
    public int getID() {
        return TASKID;
    }
    
    public short[][] getConfigBytes(int maxBytesPerMsg) {
        
        // determine the number of points that can be included in the first message
        int i1stMsgSmpl = (maxBytesPerMsg - 5)/(m_n*((m_options&1)+1)+1);
        int iPerMsgSmpl = maxBytesPerMsg/(m_n*((m_options&1)+1)+1);
        int iTotSampl = m_data.length /(m_n*((m_options&1)+1)+1);  
        int iMsgs = (i1stMsgSmpl >= iTotSampl)? 1 : 1+(int)Math.ceil(((double)(iTotSampl-i1stMsgSmpl))/((double)iPerMsgSmpl));
        int iLastMsgSmpl = (iTotSampl-i1stMsgSmpl)%iPerMsgSmpl;
        
        // allocate all messages
        short [][] config = new short[iMsgs][];
        
        int iBufferPos=0;
        // insert data - create config message and fill it with the data
        for ( int i=0; i < config.length; i++ ) {
            
            int j=0; // j keeps the position in the msg buffer
            int samples = 0;
            int iSamples = (i1stMsgSmpl >= iTotSampl)? iTotSampl : i1stMsgSmpl;
            
            // allocate message buffer
            if ( i==0 ) {
                config[0] = new short[5+iSamples*(m_n*((m_options&1)+1)+1)];

                config[0][j++] = (short)m_k;
                config[0][j++] = (short)m_n;
                
                int fs_size = m_data.length / (m_n*((m_options&1)+1)+1);
                config[0][j++] = (short)((fs_size>>8)&0x0FF);
                config[0][j++] = (short)(fs_size&0xFF);
                config[0][j++] = (short)m_options;
                samples = iSamples;
            } else if ( i == config.length-1) {
                config[i] = new short[iLastMsgSmpl*(m_n*((m_options&1)+1)+1)];
                samples = iLastMsgSmpl;
            } else {
                config[i] = new short[iPerMsgSmpl*(m_n*((m_options&1)+1)+1)];
                samples = iPerMsgSmpl;
            }
            
            // fill up data
            for ( int k=0; k<samples; k++ ) {
                for ( int l=0; l< m_n*((m_options&1)+1)+1; l++ ) {
                    config[i][j++] = m_data[iBufferPos++];
                }
            }
        }
        
        
        return config;
    }

    //////////////////////////////////////////////////////////////////////////
    // Evaluation functions

    float m_minFrequency = Float.MAX_VALUE;
    
    /** 
     * @see eth.ife.wearable.Titan.Task.Task#getMetrics(NodeMetrics)
     */
    public TaskMetrics getMetrics(NodeMetrics nm){

        //TODO: Measure KNN characteristics

        int tupelsize = (m_n*((m_options&1)+1)+1);
        int samples = (m_data.length/tupelsize);
        
        TaskMetrics tm = new TaskMetrics();
        tm.procCycles = (4*samples + m_k + 1)*50; 
        tm.datapackets = new float[] {m_minFrequency};
        tm.packetsizes = new int [] { 1 };
        tm.dynMemory = (samples+1)*tupelsize + 10;
        tm.instantiations = m_minFrequency;
        tm.period = (int)(1/m_minFrequency);
        return tm;
    }

    /** 
     * @see eth.ife.wearable.Titan.Task.Task#setInputPortDatarate()
     */
    public boolean setInputPortDatarate( int port, float pktPerSecond, int pktSize ) {
        
        if ( pktPerSecond  < m_minFrequency ) {
            m_minFrequency = pktPerSecond;
            return true;
        } else return false;
        
    }

}
