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
 * GraphPlot plots multiple data lines into one graph
 * using up to 16 different input ports.
 *
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class GraphPlot extends Task {
    
    public final static String NAME   = "graphplot";
    public final static int    TASKID = 32;

    private int m_Samples = -1;
    private int m_YLow = -1;
    private int m_YHigh = -1;

    public String getName() {
        return NAME.toLowerCase();
    }
    
    public int getID() {
        return TASKID;
    }
    
    public GraphPlot() {}
    
    public GraphPlot(GraphPlot s) {
    	super(s);
    }
    
    public Object clone() {
        return new GraphPlot(this);
    }
    
    public int getInPorts() {
        return 16;
    }
    
    public int getOutPorts() {
        return 0;
    }
    
    public boolean setConfiguration(String[] strConfig) {
      if (strConfig == null) return true;

      int i = 0;
      while (i < strConfig.length) {
        if (strConfig[i].equals("x")) {
          try {
            m_Samples = Integer.parseInt(strConfig[i+1]);
            i += 2;
          }
          catch (NumberFormatException nfe) { return false; }
        }
        else if (strConfig[i].equals("y")) {
          try {
            m_YLow = Integer.parseInt(strConfig[i+1]);
            m_YHigh = Integer.parseInt(strConfig[i+2]);
            i += 3;
            if (m_YLow >= m_YHigh) {
              m_YLow = -1;
              m_YHigh = -1;
              return false;
            }
          }
          catch (NumberFormatException nfe) { return false; }
        }
        else {
          return false;
        }
      }

      return true;
    }
    
    public int getConfigBytesNum() {
        return   ((m_Samples == -1) ? 0 : 2)
               + (((m_YLow == -1) && (m_YHigh == -1)) ? 0 : 4);
    }
    
    public short[][] getConfigBytes(int maxBytesPerMsg) {
        int n = getConfigBytesNum();
        if (n == 0) return null;

        short[][] config = new short[1][n];

        int i = 0;
        if (m_Samples != -1) {
          config[0][i++] = (short) ((m_Samples >> 8) & 0xFF);
          config[0][i++] = (short) (m_Samples & 0xFF);
        }
        if ((m_YLow != -1) || (m_YHigh != -1)) {
          config[0][i++] = (short) ((m_YLow >> 8) & 0xFF);
          config[0][i++] = (short) (m_YLow & 0xFF);
          config[0][i++] = (short) ((m_YHigh >> 8) & 0xFF);
          config[0][i++] = (short) (m_YHigh & 0xFF);
        }

        return config;
    }

    //////////////////////////////////////////////////////////////////////////
    // Evaluation functions

    /** 
     * @see eth.ife.wearable.Titan.Task.Task#getMetrics(NodeMetrics)
     */
    public TaskMetrics getMetrics(NodeMetrics nm){
        TaskMetrics tm = new TaskMetrics();
        tm.procCycles = 1;
        tm.datapackets = null;
        tm.packetsizes = null;
        tm.dynMemory = 0;
        tm.instantiations = 0;
        tm.period = 0;
        return tm;
    }

    /** 
     * @see eth.ife.wearable.Titan.Task.Task#setInputPortDatarate()
     */
    public boolean setInputPortDatarate( int port, float pktPerSecond, int pktSize ) {
        return true;
    }
}
