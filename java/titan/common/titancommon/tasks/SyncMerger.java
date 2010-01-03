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
 * @author Benedikt Köppel <bkoeppel@ee.ethz.ch>
 * @author Jonas Huber <huberjo@ee.ethz.ch>
 *
 * Merges the data coming in from a number of ports and sends the merged
 * data over the output port.
 *
 */
public class SyncMerger extends Task {


    public final static String NAME   = "syncmerger";
    public final static int    TASKID = 36;

    short numInputs;      // number of inputs
    short samplingPeriod;    // sampling period
    short bufferSize;       // buffersize

    public SyncMerger() {}

    public SyncMerger( SyncMerger m ) {
    	super(m);
        numInputs = m.numInputs;
        samplingPeriod = m.samplingPeriod;
        bufferSize = m.bufferSize;
    }

    /* (non-Javadoc)
     * @see eth.ife.wearable.Titan.Task.Task#getName()
     */
    public String getName() {
        return NAME.toLowerCase();
    }

    /* (non-Javadoc)
     * @see eth.ife.wearable.Titan.Task.Task#getID()
     */
    public int getID() {
        return TASKID;
    }

    /* (non-Javadoc)
     * @see eth.ife.wearable.Titan.Task.Task#copy()
     */
    public Object clone() {
        return new SyncMerger(this);
    }

    /* (non-Javadoc)
     * @see eth.ife.wearable.Titan.Task.Task#getInPorts()
     */
    public int getInPorts() {
        return numInputs;
    }

    /* (non-Javadoc)
     * @see eth.ife.wearable.Titan.Task.Task#getOutPorts()
     */
    public int getOutPorts() {
        // this task returns the merged data
        return 1;
    }

    /**
     * the configuration:
     *  * first argument: number of inputs. if this is bigger than the actual inputs, the syncmerger will wait
     *      on the missing inputs and then just write zeroes there
     *  * second argument: the sampling period, in which the syncmerger should generate an output packet
     *  * third argument: the fixed output delay in milliseconds. the syncmerger generates at time X the packet for time X-outputdelay,
     *      because it has to wait for late incoming packets.
     *      TODO: the third argument should be renamed to output_delay or something. and double check, if the ESyncMerger correclty gets that parameter
     */
    public boolean setConfiguration(String[] strConfig) {
        /**
         * the configuration consist of:
         * - number of inputs to synchronize
         * - sampling rate
         */

        if (strConfig == null || strConfig.length == 0) {
            System.out.println("No configuration for SyncMerger.");
            return false;
        } else if (strConfig.length > 3) {
            System.out.println("Wrong configuration for SyncMerger");
            return false;
        }

        numInputs = Short.parseShort(strConfig[0]);
        samplingPeriod = Short.parseShort(strConfig[1]);
        bufferSize = Short.parseShort(strConfig[2]);

        if ( numInputs <= 0) {
            System.out.println("SyncMerger: Less than 0 inputs makes no sense.");
            return false;
        }
        if ( samplingPeriod <= 0 ) {
            System.out.println("SyncMerger: Sampling Period < 0 makes no sense.");
            return false;
        }
        if ( bufferSize <= 0 ) {
            System.out.println("SyncMerger: BufferSize < 0 makes no sense.");
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see eth.ife.wearable.Titan.Task.Task#getConfigBytes()
     * TODO: samplingPeriod can't be bigger than 255, because only the lowest byte will be transmitted by titan...
     */
    public short[][] getConfigBytes(int maxBytesPerMsg) {
        short[][] config = new short[1][4];

        config[0][0] = numInputs;
        config[0][1] = samplingPeriod;
        config[0][2] = (short) (bufferSize & 0xff); // LSB
        config[0][3] = (short) (bufferSize >> 8);   // MSB
        return config;
    }


}
