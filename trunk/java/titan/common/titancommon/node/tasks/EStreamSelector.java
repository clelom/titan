package titancommon.node.tasks;

import titancommon.node.DataPacket;
import titancommon.node.TitanTask;
import titancommon.tasks.StreamSelector;

/**
 * This service enables selection of columns of incoming data packets. The outgoing
 * packages contain only the these columns, but stored as an array of *bytes* in the
 * sdata-array. This is because many feature tasks such as Mean expect the incoming
 * data packets to have this format.
 *
 * Example: Config string '2,4'
 *
 * Incoming packet                  Outgoing packet
 *  -----------                      -----------
 *  | short 1 |                      |b2.l|b2.h|
 *  -----------                      -----------
 *  | short 2 |                      |b4.l|b4.h|
 *  -----------                      -----------
 *  | short 3 |
 *  -----------
 *  | short 4 |
 *  -----------
 *
 * @author Benedikt Koeppel <bkoeppel@ee.ethz.ch>
 * @author Jonas Huber <huberjo@ee.ethz.ch>
 */
public class EStreamSelector extends StreamSelector implements ExecutableTitanTask {

    private TitanTask tTask;
    private short [] streams;
    private int maxStreamNo;

    public void setTitanTask(TitanTask tsk) {
        tTask = tsk;
    }

    public boolean setExecParameters(short[] param) {
        streams = param;
        return true;
    }

    public void init() {
        // Find the maximum stream id that has to be available in incoming
        // data packets.
        maxStreamNo = 0;
        for ( int i=0; i<streams.length; i++) {
            if ( streams[i] > maxStreamNo ) {
                maxStreamNo = streams[i];
            }
        }
        maxStreamNo = maxStreamNo + 1;
    }

    public void inDataHandler(int port, DataPacket data) {

        if ( port != 0 ) {
            System.err.println("[EStreamSelector] Received data on invalid port!");
            return;
        }

        if ( data.sdata.length < maxStreamNo ) {
            System.err.println("[EStreamSelector] Requested index overflow!");
            return;
        }

        // Get the requested streams and convert them to an array of *bytes*!
        short [] outData = new short[streams.length*2];

        for (int i=0,j=0; i<streams.length; i++,j+=2) {
            outData[j]   = (short)(data.sdata[streams[i]]  & 0xFF);     // Low Byte
            outData[j+1] = (short)(data.sdata[streams[i]]>>8);          // High Byte
        }

        // Create data packet and send it
        DataPacket res = new DataPacket(outData);
        tTask.send(0, res);
        //System.out.println("Task " + tTask.getRunID() + " sending");
    }
}
