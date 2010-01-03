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
 * similarity search classifier for varying input vector size.
 * 
 * Configuration:
 * 
 * SimilaritySearch( num_features, num_classes, buffer_depth, FEATURES, CLASSES )
 * 
 * where:
 *  FEATURES  addrule, port, mean, stddev_inv
 *  CLASSES   classID,minlookback,maxlookback,threshold,FeatureFlags
 * 
 * options: addrule: (1,sum) (2,avg) (3,max) (4,min)
 * 
 */
public class SimilaritySearch extends Task {
    
    public final static String NAME   = "similaritysearch";
    public final static int    TASKID = 20;

    float m_minFrequency = Float.MAX_VALUE;
    private int m_num_features;
    private int m_num_classes;
    private int m_buffer_depth;
    
    private class simsearch_feature {
        short [] data;
    }
    private simsearch_feature [] m_features;
    
    private class simsearch_class {
        short [] data;
    }
    private simsearch_class [] m_classes;
    
    
    public SimilaritySearch() {}
    
    public SimilaritySearch( SimilaritySearch m ) {
    	super(m);
        m_features = m.m_features;
        m_minFrequency = m.m_minFrequency;
        m_classes = m.m_classes;
        m_num_features = m.m_num_features;
        m_num_classes = m.m_num_classes;
        m_buffer_depth = m.m_buffer_depth;
    }
    
    public String getName() {
        return NAME.toLowerCase();
    }
    
    public Object clone() {
        return new SimilaritySearch(this);
    }
    
    public int getInPorts() {
        return m_num_features;
    }
    
    public int getOutPorts() {
        return 1;
    }
    
    //TODO: last two bytes seem not to get through!
    public boolean setConfiguration(String[] strConfig) {
        
        // argument must be an integer and 0<ARG0<256
        if ( strConfig == null || strConfig.length < 5 ) return false;
        
        m_num_features = Integer.parseInt(strConfig[0]);
        m_num_classes = Integer.parseInt(strConfig[1]);
        m_buffer_depth = Integer.parseInt(strConfig[2]);
        
        // check whether rest is ok
        if ( m_num_features*4 + m_num_classes*5 + 3 != strConfig.length ) {
            System.err.println("Task SimilaritySearch: Invalid configuration size");
            return false;
        }
        
        // ok now go through the configuration and store data
        int iCurCfg = 3;
        m_features = new simsearch_feature[m_num_features];
        for(int i=0; i < m_num_features; i++) {
            m_features[i] = new simsearch_feature();
            m_features[i].data = new short[6];
            m_features[i].data[0] = Short.parseShort(strConfig[iCurCfg++]); 
            m_features[i].data[1] = Short.parseShort(strConfig[iCurCfg++]); 
            m_features[i].data[2] = (short) ((Integer.parseInt(strConfig[iCurCfg])>>8)&0x00FF);
            m_features[i].data[3] = (short) ((Integer.parseInt(strConfig[iCurCfg++]) )&0x00FF);
            m_features[i].data[4] = (short) ((Integer.parseInt(strConfig[iCurCfg])>>8)&0x00FF);
            m_features[i].data[5] = (short) ((Integer.parseInt(strConfig[iCurCfg++]) )&0x00FF);
        }

        m_classes = new simsearch_class[m_num_classes];
        for(int i=0; i < m_num_classes; i++) {
            m_classes[i] = new simsearch_class();
            m_classes[i].data = new short[7];
            m_classes[i].data[0] = Short.parseShort(strConfig[iCurCfg++]); 
            m_classes[i].data[1] = Short.parseShort(strConfig[iCurCfg++]); 
            m_classes[i].data[2] = Short.parseShort(strConfig[iCurCfg++]); 
            m_classes[i].data[3] = (short) ((Integer.parseInt(strConfig[iCurCfg])>>8)&0x00FF);
            m_classes[i].data[4] = (short) ((Integer.parseInt(strConfig[iCurCfg++]) )&0x00FF);
            m_classes[i].data[5] = (short) ((Integer.parseInt(strConfig[iCurCfg])>>8)&0x00FF);
            m_classes[i].data[6] = (short) ((Integer.parseInt(strConfig[iCurCfg++]) )&0x00FF);
        }
        
        // check whether all data has really been used
        if ( iCurCfg != strConfig.length ) {
            System.err.println("Task SimilaritySearch: Could not use all configuration data");
            return false;
        }

        return true;
    }
    
    public int getID() {
        return TASKID;
    }
    
    public short[][] getConfigBytes(int maxBytesPerMsg) {
        
        // compute number of messages needed
        int i1stMsgFeatures =  Math.min((maxBytesPerMsg-5)/6,m_features.length);
        int iFeatureMsgs    = (int)Math.ceil((double)(m_features.length - i1stMsgFeatures)*6.0/(double)maxBytesPerMsg);
        int iClassMsgs      = (int)Math.ceil((double)m_classes.length*7/(double)maxBytesPerMsg);
        
        short [][] config = new short[1+iFeatureMsgs+iClassMsgs][];
        
        // overflow check
        if ( i1stMsgFeatures > 15 ) System.err.println("Task SimilaritySearch: Did not expect so many features in first message!");
        
        // create first message
        config[0] = new short[5+i1stMsgFeatures*6];
        int iCurMsg=0,iMsgIndex=0;
        config[iCurMsg][iMsgIndex++] = 0x10;
        config[iCurMsg][iMsgIndex++] = (short)m_num_features;
        config[iCurMsg][iMsgIndex++] = (short)m_num_classes;
        config[iCurMsg][iMsgIndex++] = (short)m_buffer_depth;
        config[iCurMsg][iMsgIndex++] = (short)(i1stMsgFeatures<<4); // no classes
        for ( int i=0; i < i1stMsgFeatures; i++ ) {
            for ( int j=0; j < m_features[i].data.length; j++ ) {
                config[iCurMsg][iMsgIndex++] = m_features[i].data[j];
            }
        }
        iCurMsg++;
        
        
        // create the rest of the feature messages
        int iFeaturesPerMessage = (maxBytesPerMsg-2)/6;
        int iFeatures = (iCurMsg<iFeatureMsgs)? iFeaturesPerMessage : m_features.length-i1stMsgFeatures;
        config[iCurMsg] = new short[ iFeatures*6+2 ];
        config[iCurMsg][0] = (short)0x20;
        config[iCurMsg][1] = (short)(iFeatures<<4);
        iMsgIndex = 2;
        for ( int i=i1stMsgFeatures; i < m_features.length; i++ ) {

            // check whether message is full
            if ( iMsgIndex + 6 > maxBytesPerMsg ) {
                iCurMsg++;
                iFeatures = (iCurMsg<iFeatureMsgs)? iFeaturesPerMessage : (m_features.length-i);
                config[iCurMsg] = new short[ iFeatures*6+1 ];
                config[iCurMsg][0] = (short)0x20;
                config[iCurMsg][1] = (short)(iFeatures<<4);
                iMsgIndex = 2;
            }
            
            for( int k=0; k < m_features[i].data.length; k++ ) {
                config[iCurMsg][iMsgIndex++] = m_features[i].data[k];
            }
        }
        if ( iFeatureMsgs != 0 ) iCurMsg++;
        
        // create the rest of the feature messages
        int iClassesPerMessage = (maxBytesPerMsg-2)/7;
        int iClasses = (iCurMsg<iClassMsgs-iFeatureMsgs)? iClassesPerMessage : m_classes.length;
        config[iCurMsg] = new short[ iClasses*7+2 ];
        config[iCurMsg][0] = (short)0x20;
        config[iCurMsg][1] = (short)(iClasses&0x00FF);
        iMsgIndex = 2;
        for ( int i=0; i < m_classes.length; i++ ) {

            // check whether message is full
            if ( iMsgIndex + 6 > maxBytesPerMsg ) {
                iCurMsg++;
                iClasses = (iCurMsg<iClassMsgs-iFeatureMsgs)? iClassesPerMessage : (m_classes.length-i);
                config[iCurMsg] = new short[ iClasses*6+1 ];
                config[iCurMsg][0] = (short)0x20;
                config[iCurMsg][1] = (short)(iClasses<<4);
                iMsgIndex = 2;
            }
            
            for( int k=0; k < m_classes[i].data.length; k++ ) {
                config[iCurMsg][iMsgIndex++] = m_classes[i].data[k];
            }
        }

        if ( iCurMsg+1 != config.length ) {
            System.err.println("Task SimilaritySearch: Could not fill up configuration messages!");
        }

        return config;
    }

    //////////////////////////////////////////////////////////////////////////
    // Evaluation functions

    /** 
     * @see eth.ife.wearable.Titan.Task.Task#getMetrics(NodeMetrics)
     */
    public TaskMetrics getMetrics(NodeMetrics nm){

        //TODO: Measure SimSearch characteristics

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
