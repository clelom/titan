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

import java.util.ArrayList;

import titancommon.compiler.NodeMetrics;
import titancommon.compiler.TaskMetrics;

/**
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *
 */
public class Communicator extends Task {
    
    public final static String NAME   = "COM";
    public final static int    TASKID = 0;
    private int m_iOutPorts;
    private int m_iInPorts;
    
    public class CommConfig {
        public int destAddr;
        public int destPort;
        public CommConfig( int iDestAddr, int iDestPort )
        {destAddr=iDestAddr;destPort=iDestPort;}
    }
    public ArrayList m_Connections = new ArrayList();
    
    
    public Communicator() {m_iOutPorts=0;}
    
    public Communicator( int nodeAddress, int outPort  ){
        m_iOutPorts=0;
    }
    
    public Communicator( Communicator com ) {
    	super(com);
        m_iOutPorts=0;
    }
    
    public String getName() {
        return NAME;
    }
    
    public int getID() {
        return TASKID;
    }
    
    public Object clone() {
        return new Communicator(this);
    }
    
    public int getInPorts() {
        return 0;
    }
    
    public int getOutPorts() {
        return m_iOutPorts;
    }
    
    public boolean setConfiguration(String[] strConfig) {
        return false;
    }
    
    public short[][] getConfigBytes(int maxBytesPerMsg) {
        short[][] cfgdata = {new short[m_Connections.size()*3]};
        
        for (int i=0;i<m_Connections.size();i++) {
            CommConfig cc = (CommConfig)m_Connections.get(i);
            //System.out.println("ComTask: Configuring connection: " + cc.destAddr + ":" + cc.destPort);
            cfgdata[0][i*3  ] = (short)((cc.destAddr>>8)&0xFF);
            cfgdata[0][i*3+1] = (short)((cc.destAddr)&0xFF);
            cfgdata[0][i*3+2] = (short)cc.destPort;
        }
        
        return cfgdata;
    }
    
    public int addConnection( int iDestAddr, int iDestPort ) {
        m_iInPorts++;
        m_Connections.add( new CommConfig(iDestAddr, iDestPort));
        return m_Connections.size()-1;
    }
    
    /**
     * Checks whether a connection to the specified destination node and port exists
     * @param iDestAddr Node address of the destination
     * @param iDestPort Port address of the destination
     * @return
     */
    public boolean hasConnection(int iDestAddr, int iDestPort) {
        for(int i=0; i < m_Connections.size(); i++ ) {
            if ( (((CommConfig)m_Connections.get(i)).destAddr == iDestAddr) && 
                 (((CommConfig)m_Connections.get(i)).destPort == iDestPort) ) 
                    return true;
        }
        return false;
    }
    
    public int addOutport() {
        return m_iOutPorts++;
    }
    
    //////////////////////////////////////////////////////////////////////////
    // Metrics
    
    private float [] m_pktPerSecond;
    private int []   m_pktSize;
    
    /** 
     * This function returns the execution times on the Communicator task.
     * 
     * NOTE: TaskMetrics contains the communication for packets sent over 
     *       the wireless link, and *NOT* the internal ports!!!
     * @return TaskMetrics for the execution. Packet information is *ONLY FOR 
     *         THE WIRELESS LINK!*
     */
    public TaskMetrics getMetrics(NodeMetrics nm){

        //TODO: Measure Communicator characteristics

        float totalCalls = 0;
        if ( m_pktPerSecond != null ) {
            for(int i=0;i<m_pktPerSecond.length; i++ ) {
                totalCalls+=m_pktPerSecond[i];
            }
        }
        
        TaskMetrics tm = new TaskMetrics();
        tm.procCycles = 5500; 
        tm.datapackets = m_pktPerSecond;
        tm.packetsizes = m_pktSize;
        tm.dynMemory = m_iInPorts*7;
        tm.instantiations = totalCalls;
        tm.period = (int)(1/totalCalls);
        return tm;
    }

    /** 
     * @see eth.ife.wearable.Titan.Task.Task#setInputPortDatarate()
     */
    public boolean setInputPortDatarate( int port, float pktPerSecond, int pktSize ) {
        
        boolean bChanged = false;
        
        if (m_pktPerSecond == null ) {
            m_pktPerSecond = new float[m_iInPorts];
            m_pktSize      = new int[m_iInPorts];
            bChanged = true;
        }
        
        if ( m_pktPerSecond[port] != pktPerSecond ) {
            m_pktPerSecond[port] = pktPerSecond;
            bChanged = true;
        }
        
        if ( m_pktSize[port] != pktSize ) {
            m_pktSize[port] = pktSize;
            bChanged = true;
        }
        
        return bChanged;
        
    }

}
