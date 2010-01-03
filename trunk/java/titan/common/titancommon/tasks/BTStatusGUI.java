/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package titancommon.tasks;

/**
 * This is a simple GUI which can be used to display the status of the connections
 * to bluetooth sensors.
 * 
 * @author Benedikt Köppel <bkoeppel@ee.ethz.ch>
 * @author Jonas Huber <huberjo@ee.ethz.ch>
 */
public class BTStatusGUI extends Task {

    public final static String NAME   = "btstatusgui";
    public final static int    TASKID = 37;

    protected short numOfConnections;

    /** Constructor */
    public BTStatusGUI() {
    }

    /** Copy constructor */
    public BTStatusGUI(BTStatusGUI s) {
        super(s);
        numOfConnections = s.numOfConnections;
    }

    /** Clone object */
    public Object clone() {
        return new BTStatusGUI(this);
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
        return 7;   // because a maximum of 7 bluetooth nodes can be connected.
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
