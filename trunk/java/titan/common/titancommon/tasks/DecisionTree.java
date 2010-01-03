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

import java.util.Vector;

import titancommon.compiler.NodeMetrics;
import titancommon.compiler.TaskMetrics;

/**
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 *
 * decision tree classifier.
 * 
 * Configuration:
 * 
 * DecisionTree( num_features, (operator,[feature,]value)+ )
 * 
 * where:
 *  num_features  received as input to the classifier
 *  operator      operator to be performed on feature (<,>,=,class)
 *  feature       input feature number (not needed for class), zero based, must be <num_features
 *  value         value used for operator
 *  
 *
 * Note: the given tree node tupels are interpreted as breath-first. Each 
 *       node with operator not 'class' has two child nodes, of which the 
 *       child for the TRUE operator outcome comes first. Example:
 *       
 *                           +---------+
 *                           | f0 > 24 |
 *                           +----+----+
 *                   true         |         false
 *                    +-----------+-----------+
 *                    |                       |
 *               +----+----+             +----+----+
 *               | class 1 |             | f1 = 4  |
 *               +---------+             +----+----+
 *                                            |
 *                                +-----------+-----------+
 *                                |                       |
 *                           +----+----+             +----+----+
 *                           | class 4 |             | class 2 |
 *                           +---------+             +---------+
 *               
 *        is encoded as:  
 *        
 *                         +----+  +-----+  +---+  +-----+  +-----+
 *        DecisionTree( 2, >,0,24, class,1, =,1,4, class,4, class,2 )
 *        
 *        Where f* stands for feature number *.
 *  
 */
public class DecisionTree extends Task {
    
    public final static String NAME   = "decisiontree";
    public final static int    TASKID = 29;

    /////////////////////////////////////////
    // Internal data representation
    protected static final short TDO_SMALLER_THAN = 0x00;
    protected static final short TDO_LARGER_THAN  = 0x10;
    protected static final short TDO_EQUAL        = 0x20;
    protected static final short TDO_SETCLASS     = 0x30;
    
    private class TreeNode {
        short operator; // one of the TDO_* values
        short value; // value to be operated on
    }
    /////////////////////////////////////////

    // data fields
    private Vector m_tree;
    float m_minFrequency = Float.MAX_VALUE;
    private int m_num_features;
    
    
    public DecisionTree() {}
    
    public DecisionTree( DecisionTree m ) {
    	super(m);
        m_tree = (m.m_tree == null)? null : (Vector) m.m_tree.clone();
        m_minFrequency = m.m_minFrequency;
        m_num_features = m.m_num_features;
    }
    
    public String getName() {
        return NAME.toLowerCase();
    }
    
    public Object clone() {
        return new DecisionTree(this);
    }
    
    public int getInPorts() {
        return m_num_features;
    }
    
    public int getOutPorts() {
        return 1;
    }
    
    public int getID() {
        return TASKID;
    }
    
    //TODO: last two bytes seem not to get through!
    public boolean setConfiguration(String[] strConfig) {
        
        // argument must be an integer and >3
        if ( strConfig == null || strConfig.length < 3 ) return false;

        // read in data
        m_num_features = Integer.parseInt(strConfig[0]);
        
        if (m_num_features > 8) {
            System.err.println("DecisionTree: No more than 8 features supported");
            return false;
        }

        m_tree = new Vector();
        
        // extract tree nodes
        int i=1;
        while (i < strConfig.length) {
            // determine operator
            short operator = (strConfig[i].compareTo(">")==0)?     TDO_LARGER_THAN :
                             (strConfig[i].compareTo("<")==0)?     TDO_SMALLER_THAN :
                             (strConfig[i].compareTo("=")==0)?     TDO_EQUAL :
                             (strConfig[i].compareTo("class")==0)? TDO_SETCLASS :
                                                                  -1;
            // check operator ok?
            if (operator==-1) {
                System.err.println("DecisionTree: Error: undefined operator: " + strConfig[i] );
                return false;
            }
            
            // make sure we have enough operands
            if ( ((operator != TDO_SETCLASS) &&  (i+2 >= strConfig.length)) ||
               /*((operator == TDO_SETCLASS) &&*/(i+1 >= strConfig.length) ) {
               System.err.println("DecisionTree: Insufficient arguments at node" + (m_tree.size()+1) );
               return false;
            }
            
            TreeNode tn = new TreeNode();
            if ( operator == TDO_SETCLASS ) { // parse a setnode tree node
                tn.operator = TDO_SETCLASS;
                tn.value = Short.parseShort(strConfig[i+1]);
                i+=2;
            } else { // parse all other operators
                short feature = Short.parseShort(strConfig[i+1]);
                
                if ( feature >= m_num_features ) {
                    System.err.println("DecisionTree: Invalid feature number: " + feature );
                    return false;
                }
                
                tn.operator = (short)(operator | (feature&0x0F)); // operator and features are encoded together
                tn.value = Short.parseShort(strConfig[i+2]);
                i+=3;
            }
            m_tree.add(tn);
            
        }
        
        return true;
    }
    
    public short[][] getConfigBytes(int maxBytesPerMsg) {

        // determine number and size of config messages
        int iClassesMsgOne = (int)Math.min(  (int)Math.floor((maxBytesPerMsg-3)/3), m_tree.size()  );
        int iClassesPerMsg = (int)Math.floor((maxBytesPerMsg-1)/3);
        int iMsgs = (int)Math.ceil((double)(m_tree.size()-iClassesMsgOne)/(double)iClassesPerMsg)+1;
        
        // allocate buffer
        short [][] config = new short[iMsgs][];

        // write first message
        config[0] = new short[iClassesMsgOne*3+3];
        config[0][0] = (short)(0x80 | Math.min(iClassesMsgOne,m_tree.size())); // number of nodes in this message
        config[0][1] = (short) m_tree.size(); // total number of nodes
        config[0][2] = (short)m_num_features; // number of feature inputs
        for ( int j=0; j<iClassesMsgOne; j++ ) {
            TreeNode curNode = (TreeNode) m_tree.get(j); 
            config[0][j*3+3] = curNode.operator;
            config[0][j*3+4] = (short)((curNode.value >> 8) & 0xFF);
            config[0][j*3+5] = (short)((curNode.value     ) & 0xFF);
        }
        
            // write messages
        for (int i=1; i<iMsgs; i++) {
            int iClasses = Math.min(iClassesPerMsg, m_tree.size()-iClassesMsgOne-(i-1)*iClassesPerMsg);
            config[i] = new short[iClasses*3+1];
            config[i][0] = (short)iClasses; // number of nodes in this message
            for (int j=0; j<iClasses; j++) {
                TreeNode curNode = (TreeNode) m_tree.get(iClassesMsgOne + (i-1)*iClassesPerMsg+j); 
                config[i][j*3+1] = curNode.operator;
                config[i][j*3+2] = (short)((curNode.value >> 8) & 0xFF);
                config[i][j*3+3] = (short)((curNode.value     ) & 0xFF);
            }
        }
        
        return config;
    }

    //////////////////////////////////////////////////////////////////////////
    // Evaluation functions

    /** 
     * @see eth.ife.wearable.Titan.Task.Task#getMetrics(NodeMetrics)
     */
    public TaskMetrics getMetrics(NodeMetrics nm){

        //TODO: Measure DecTree characteristics
        TaskMetrics tm = new TaskMetrics();
        tm.procCycles = 1;
        tm.datapackets = new float[] {m_minFrequency};
        tm.packetsizes = new int [] { 2 };
        tm.dynMemory = 0;
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
