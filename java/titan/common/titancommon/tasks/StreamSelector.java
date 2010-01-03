package titancommon.tasks;

/**
 * Allows to select some streams (columns) of a datapackage.
 *
 * @author Benedikt Köppel <bkoeppel@ee.ethz.ch>
 * @author Jonas Huber <huberjo@ee.ethz.ch>
 */
public class StreamSelector extends Task {

    public final static String NAME = "streamselector";
    public final static int TASKID = 42;

    protected short [] m_streams;

    /** Constructor */
    public StreamSelector() {
    }

    /** Copy constructor */
    public StreamSelector(StreamSelector s) {
        super(s);
        m_streams = s.m_streams;
    }

    /** Clone object */
    public Object clone() {
        return new StreamSelector(this);
    }


    /**
     * defines all column-IDs which you want to select
     * 
     */
    public boolean setConfiguration(String[] strConfig) {

        if ( strConfig.length < 1 ) {
            System.err.println("[StreamSelector] Need at least one config parameter!");
            return false;
        }

        m_streams = new short[strConfig.length];

        for (int i = 0; i < strConfig.length; i++) {
            m_streams[i] = Short.parseShort(strConfig[i]);
        }

        return true;
    }
    
    /**
     * This has to return the configuration
     * @param maxBytesPerMsg
     * @return
     */
    public short[][] getConfigBytes(int maxBytesPerMsg) {
        short [][] c = {m_streams};
        return c;
    }

    public int getID() {
        return TASKID;
    }

    public int getInPorts() {
        return 1;
    }

    public String getName() {
        return NAME.toLowerCase();
    }

    public int getOutPorts() {
        return 1;
    }
}
