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
 * The AliasTask can simulate any other task and be configured according to 
 * user needs. It has mainly been added to evaluate different compiler 
 * performance issues without the need to implement a large number of tasks. 
 * The config file can configure the alias task the way it likes 
 * 
 * See setConfiguration() for parameters to set
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *
 */
public class AliasTask extends Task {

    public String m_strName = "alias";
    public int    m_taskID  = 123;
    public double[][]  m_inPorts;
    public double[][]  m_outPorts;

    //TODO: AliasTask: model for input->output relation and task metrics
//    private double[][] m_outMatrixPacketFreq;
//    private double[][] m_outMatrixPacketSize;
    private TaskMetrics m_tm;

    public String getName() {
        if (m_taskID!=123) {
            return m_strName+"("+m_taskID+")"; // this is returned when it has been used
        } else {
            return m_strName;
        }
    }

    public int getID() {
        return m_taskID;
    }
    
    public AliasTask() {};
    
    public AliasTask(AliasTask at) {
    	super(at);
        m_strName = at.m_strName;
        m_taskID  = at.m_taskID;
        if ( at.m_inPorts != null ) {
            m_inPorts = new double[at.m_inPorts.length][2];
            for (int i=0; i<at.m_inPorts.length; i++ ) {
                m_inPorts[i][0] = at.m_inPorts[i][0];
                m_inPorts[i][1] = at.m_inPorts[i][1];
            }
        }
        if ( at.m_outPorts != null ) {
            m_outPorts = new double[at.m_outPorts.length][2];
            for (int i=0; i<at.m_outPorts.length; i++ ) {
                m_outPorts[i][0] = at.m_outPorts[i][0];
                m_outPorts[i][1] = at.m_outPorts[i][1];
            }
        }
        if (at.m_tm!=null) m_tm = new TaskMetrics(at.m_tm);
    }

    public Object clone() {
        return new AliasTask(this);
    }

    public int getInPorts() {
        return (m_inPorts!=null)? m_inPorts.length : 0;
    }

    public int getOutPorts() {
        return (m_outPorts!=null)? m_outPorts.length : 0;
    }

    /**
     * Sets the configuration of the AliasTask task. At least the first  
     * three parameters need to be given.
     * 
     * Parameter sequence and type:
     * 
     * Required:
     * taskID     int   TaskID for this task
     * inPorts    int   number of input ports
     * outPorts   int   number of output ports
     * 
     * Optional (must be given in sequence)
     * procCycles     int   processing cycles to execute the task
     * dynMem         int   dynamic memory
     * period         int   avg delay between tasks
     * instantiations int   instantiations per second
     * datapackets    float[outPorts]
     * packetsizes    int[outPorts]
     * 
     */
    public boolean setConfiguration(String[] strConfig) {
        
        if (strConfig.length < 1 ) return false;
        
        try {
            
            // parse parameters
            m_taskID = Integer.parseInt(strConfig[0]);
            m_inPorts = new double[Integer.parseInt(strConfig[1])][2]; 
            m_outPorts = new double[Integer.parseInt(strConfig[2])][2];
            
            // matrices for transport
//            m_outMatrixPacketFreq = new double[m_inPorts.length][m_outPorts.length];
//            m_outMatrixPacketSize = new double[m_inPorts.length][m_outPorts.length];
            
            // go through optional parameters
            if ( strConfig.length > 3 ) m_tm = new TaskMetrics();
            int iParameter=3;

            if ( strConfig.length > iParameter ) m_tm.procCycles = Integer.parseInt(strConfig[iParameter++]);
            if ( strConfig.length > iParameter ) m_tm.dynMemory = Integer.parseInt(strConfig[iParameter++]);
            if ( strConfig.length > iParameter ) m_tm.period = Integer.parseInt(strConfig[iParameter++]);
            if ( strConfig.length > iParameter ) m_tm.instantiations = Integer.parseInt(strConfig[iParameter++]);
            if ( strConfig.length > iParameter ) {

                if ( strConfig.length - iParameter < 2*m_outPorts.length) return false;
                
                m_tm.datapackets = new float[m_outPorts.length];
                m_tm.packetsizes = new int[m_outPorts.length];
                for (int i=0;i<m_tm.datapackets.length; i++ ) {
                    m_tm.datapackets[i] = Float.parseFloat(strConfig[iParameter++]);
                    m_tm.packetsizes[i] = Integer.parseInt(strConfig[iParameter++]);
                }
            }

        } catch (NumberFormatException e) {
            return false;
        }
        
        return true;
    }

    public short[][] getConfigBytes(int maxBytesPerMsg) {
        return null;
    }

    public TaskMetrics getMetrics(NodeMetrics nm){
        return m_tm; 
    }

    /** 
     * Sets the datarate at the specified input port. This value is set to compute 
     * the datarates appearing at the output ports. 
     * 
     * @param port port number
     * @param pktPerSecond Number of packets coming in per second
     * @param pktSize Average size of packets going out
     * @return Whether this information changed the configuration
     */
    public boolean setInputPortDatarate( int port, float pktPerSecond, int pktSize ) 
    {
        boolean bChanged = false;

        if (m_inPorts.length > port ) {
            
            bChanged = (m_inPorts[port][0] != pktPerSecond) || (m_inPorts[port][1] != pktSize);
                
            m_inPorts[port][0] = pktPerSecond;
            m_inPorts[port][1] = pktSize;
        } else return bChanged;
        
        return bChanged;
    }

}
