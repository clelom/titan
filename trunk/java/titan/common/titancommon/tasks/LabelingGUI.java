package titancommon.tasks;

import titancommon.util.StringUtil;

/**
 * Provides a GUI which allows for up to 8 differen labels to be assigned to
 * an activity.
 * TODO: really only 8 labels? i thought we could show more, and then with a slider-bar
 * 
 * @author Benedikt Köppel <bkoeppel@ee.ethz.ch>
 * @author Jonas Huber <huberjo@ee.ethz.ch>
 */
public class LabelingGUI extends Task {

    public final static String NAME = "labelinggui";
    public final static int TASKID = 38;
    protected short [] m_configshort;   // Stores labels as short array.

    /** Constructor */
    public LabelingGUI() {
    }

    /** Copy constructor */
    public LabelingGUI(LabelingGUI l) {
        super(l);
        m_configshort = l.m_configshort;
    }

    /** Clone object */
    public Object clone() {
        return new LabelingGUI(this);
    }

    /**
     * This has to return the configuration
     * @param maxBytesPerMsg
     * @return
     */
    public short[][] getConfigBytes(int maxBytesPerMsg) {
        short [][] c ={m_configshort};
        return c;
    }

    public int getID() {
        return TASKID;
    }

    public int getInPorts() {
        return 0;
    }

    public String getName() {
        return NAME.toLowerCase();
    }

    public int getOutPorts() {
        return 1;
    }

    /**
     * The configuration should contain one string, namely the filename of a
     * config file.
     * 
     * @param strConfig
     * @return
     */
    public boolean setConfiguration(String[] strConfig) {

        if ( strConfig == null || strConfig.length > 1 ) {
            System.err.println("[LabelingGUI] Wrong config parameters!");
            return false;
        }

        m_configshort = StringUtil.toShortArray( strConfig[0] );
        return true;
       
    }
}
