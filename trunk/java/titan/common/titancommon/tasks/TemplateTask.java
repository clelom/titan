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

/**
 *
 * TODO for a new task:
 *  1) find a free task ID in "int[] testtasks" in titancommon/services/ServiceDirectory.java
 *  2) make an entry of this task in "private static final Class[] execTaskClasses" in titancommon/node/TitanTask.java
 *  3) make an entry of this task in "public ConfigReader( String strFilename ) throws FileNotFoundException" in titan(titanmobile)/ConfigReader.java
 *  4) implement all methods here
 *  5) create a ETemplateTask in titancommon/node/tasks and continue there with the implementation
 *
 * you'll need to change the class name of this task according to your task name
 * don't forget that the filename and the class name must match!
 *
 * @author Benedikt Köppel <bkoeppel@ee.ethz.ch>
 * @author Jonas Huber <huberjo@ee.ethz.ch>
 *
 */
public class TemplateTask extends Task {

    public final static String NAME = "templatetask";     // the name of this task
    public final static int TASKID = 100;                // a new and free task id (see int[] testtasks in titancommon/services/ServiceDirectory.java)

    /**
     * you don't have to change this
     * @return the name of this task
     */
    public String getName() {
        return NAME.toLowerCase();
    }

    /**
     * you don't have to change this
     * @return the task ID of this task
     */
    public int getID() {
        return TASKID;
    }

    /**
     * constructor
     * you'll have to rename this according to your class name
     */
    public TemplateTask() {
    }

    /**
     * copy constructor
     * you'll have to rename this according to your class name
     * and you'll need to copy all your internal variables here!
     * @param s a TemplateTask to copy
     */
    public TemplateTask(TemplateTask s) {
        super(s);
    }

    /**
     * to clone itself
     * you'll have to change the return class of this according to your class name
     * @return an object copy of this class
     */
    public Object clone() {
        return new TemplateTask(this);
    }

    /**
     * you'll need to specify here, how many input ports your task has
     * @return the number of input ports
     */
    public int getInPorts() {
        return 127;
    }

    /**
     * you'll need to specify here, how many output ports your task has
     * @return the number of output ports
     */
    public int getOutPorts() {
        return 0;
    }

    /**
     * this method sets the configuration
     * you need to change this according to your needs
     * normally, you'll set some class variables here (don't forget to copy them in the copy constructor)
     * 
     * the config-string (in the config-file) is splittet at the , (commas), and each element in the strConfig-array is one of those splittet strings
     * @param strConfig the configuration (splitted at the commas)
     * @return return true, if the configuration was parsed successfully
     */
    public boolean setConfiguration(String[] strConfig) {
        return (strConfig == null);
    }

    /**
     * this method returns the configuration, which is then inserted into the ETemplateTask
     * you'll need to return a 2D-array with the configuration. Each element will be an input for the ETemplateTask setExecParameters.
     * for each element in this 2D-array, the setExecParameter will be called separately. But you are not sure, that they're called in
     * the same order as they're in the 2D array.
     * so a good idea would be to have the first byte in each element as a ID, and all following bytes for the data
     *
     * normally, you'll return all data you got with setConfiguration here again, but as short array
     * please note, that it is actually not a short-array, but rather a char-array. you can't have 16 bit values in the array, all values
     * (each element) must only be 8 bits (or they'll get cut off).
     *
     * see also in titancommon.util StringUtil, there are some methods to convert strings to short arrays and vice versa
     *
     * the configuration must be smaller than maxBytesPerMsg
     *
     * @param maxBytesPerMsg maximal size of a config message
     * @return a 2D array with all configuration
     */
    public short[][] getConfigBytes(int maxBytesPerMsg) {
        return null;
    }
    /**
     * ok, now go on and write a ETemplateTask in titancommon/node/tasks and continue there
     */
}
