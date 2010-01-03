package titancommon.tasks;

/**
 * This is a demo application which shows the ability of Titan to do online
 * activity recognition on a mobile phone.
 *
 * It takes the output of the DecisionTree and informs the user about the detected
 * acitivity using text and images.
 *
 * @author Benedikt Köppel <bkoeppel@ee.ethz.ch>
 * @author Jonas Huber <huberjo@ee.ethz.ch>
 */
public class DemoGUI extends Task {

    public final static String NAME   = "demogui";
    public final static int    TASKID = 40;

    /** Constructor */
    public DemoGUI() {
    }

    /** Copy constructor */
    public DemoGUI(DemoGUI d) {
        super(d);
    }

    /** Clone object */
    public Object clone() {
        return new DemoGUI(this);
    }

    /**
     * This has to return the configuration
     * @param maxBytesPerMsg
     * @return
     */
    public short[][] getConfigBytes(int maxBytesPerMsg) {
        return null;
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
        return 0;
    }

    /**
     * no configuration needed and possible
     */
    public boolean setConfiguration(String[] strConfig) {
        return ( strConfig == null );
    }
}