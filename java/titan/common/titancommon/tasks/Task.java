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
 * Titan task superclass. This class defines the interface needed for all tasks.
 * It implements the handling of attributes that define where tasks should be 
 * placed.
 * 
 * All tasks MUST define a default constructor (Task()), or they cannot be loaded
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */


package titancommon.tasks;

import java.util.*;

import titancommon.compiler.NodeMetrics;
import titancommon.compiler.TaskMetrics;


public abstract class Task {
    
    /** Gets the name of the tasks as it appears in the config file.
     *  *CAUTION*: must be lowercase!
     */
    public abstract String getName();
    
    /** Gets the unique identifier of the task */
    public abstract int getID();
    
    /** Returns a copy of itself - this function should call a copy constructor, which first calls the super(copy) class! */
    public abstract Object clone();
    
    /** Returns the number of input ports with the current configuration */
    public abstract int getInPorts();
    
    /** Returns the number of output ports with the current configuration 
     * @return
     */
    public abstract int getOutPorts();
    
    /** Enters a configuration string as it appears in the config file. 
     * *CAUTION*: whitespace is removed during the parsing
     * @param strConfig The configuration contents from the configuration file 
     * @return Whether successful
     */
    public abstract boolean setConfiguration(String[] strConfig);
    
    /** Gets the contribution for a configuration message for Titan 
     * @param maxBytesPerMsg Maximum message size
     */
    public abstract short[][] getConfigBytes(int maxBytesPerMsg);
    
    //////////////////////////////////////////////////////////////////////////
    // Performance estimation
    
    /** 
     * Indicates the complexity of the algorithm contained in the task
     * @param nm Indicates on what node the task is supposed to run
     * @return The metrics of this task 
     */
    public TaskMetrics getMetrics(NodeMetrics nm){ return null; }

    /** 
     * Sets the datarate at the specified input port. This value is set to compute 
     * the datarates appearing at the output ports. 
     * 
     * @param port port number
     * @param pktPerSecond Number of packets coming in per second
     * @param pktSize Average size of packets going out
     * @return Whether this information changed the configuration
     */
    public boolean setInputPortDatarate( int port, float pktPerSecond, int pktSize ) { return false; }


    //////////////////////////////////////////////////////////////////////////
    // Task attributes (these functions should not be overriden)

    private Map/*<String,String>*/ m_Attributes = new HashMap();
    
    /** Sets an attribute to the task */
    public boolean addAttribute(String strName, String strValue) {
        m_Attributes.put(strName, strValue);
        return true; 
    }
    
    /** Retrieves an attribute value */
    public String getAttribute( String strName ) {
        return (String)m_Attributes.get(strName);
    }
    
    public Task() {
    }
    
    public Task(Task t) {
    	m_Attributes.putAll(t.m_Attributes);
    	
/*    	Iterator<Map.Entry<String,String>> iter = m_Attributes.entrySet().iterator();
    	for (int i = 0; i < m_Attributes.size(); i++)
    	{
    	  Map.Entry<String,String> entry = (Map.Entry<String,String>) iter.next();
    	  String key = entry.getKey();
    	  String value = entry.getValue();
    	  m_Attributes.put(key,value);
    	}
*/
    	
    }
}
