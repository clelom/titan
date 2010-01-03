package titancommon.node.tasks;

//import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import titancommon.node.TitanTask;
import titancommon.node.DataPacket;
import titancommon.tasks.VirtualBTSensor;

import java.util.Random;

/**
 *
 * this is just a demo task to simulate the BTSensor
 * it randomly goes offline after a random period of time, to see what happens with syncmerger or so
 * does not send a status information (or does it?)
 *
 * @author Benedikt Köppel <bkoeppel@ee.ethz.ch>
 * @author Jonas Huber <huberjo@ee.ethz.ch>
 *
 */
public class EVirtualBTSensor extends VirtualBTSensor implements ExecutableTitanTask {

    private TitanTask tTask;

    public void setTitanTask(TitanTask tsk) {
        tTask = tsk;
    }
    private static final int DEFAULT_INTERVAL_MS = 1000;
    private int interval_ms;
    private Random randGen = new Random();
    // define a random nodeID, which will be constant for all the time
    private short nodeID = (short) randGen.nextInt(Short.MAX_VALUE);
    private boolean online = true;      // online: true if online, false if offline (change this variable with the debugger to simulate a sensor which is unresponsive)
    private long remainingPackets = Long.MAX_VALUE;

    public boolean setExecParameters(short[] param) {
        switch (param.length) {
            case 0:
                interval_ms = DEFAULT_INTERVAL_MS;
                break;

            case 2:
                interval_ms = (param[0] << 8) + param[1];

                break;

            default:
                tTask.errSource = tTask.getRunID();
                tTask.errType = 4;  // ERROR_CONFIG (is there a constant table for the IDs?)
                return false;
        }
        return true;
    }

    public void init() {
        (new Timer()).schedule(new SWTimerTask(), 0, interval_ms);
        if( randGen.nextInt(2) == 1) {
            remainingPackets = randGen.nextInt(1000/interval_ms*360) + 1000/interval_ms*30; // within the first 360 seconds, this sensor will go offline, but not within the first 30 seconds
            System.out.println("This sensor goes offline after " + remainingPackets + " Packets sent.");
        } else {
            System.out.println("This sensor will stay online forever (nearly...)");
        }
    }

    public void sendStatus() {

        short[] status_array = new short[1];
        if (online) {
            status_array[0] = 0;
        } else {
            status_array[0] = 1;
        }
        DataPacket dp = new DataPacket(status_array);
        tTask.send(2, dp);
    }

    public void inDataHandler(int port, DataPacket data) {
        // no incoming ports
    }

    private class SWTimerTask extends TimerTask {

        private int counter = 0;
        //private long startTime = (new Date()).getTime();

        private void sendData() {
            //long t = (new Date()).getTime() - startTime;

            // set this sensor offline after a random period of packets
            if ( remainingPackets <= 0 ) {
                online = false;
                System.out.println("This Sensor is offline now!");
                return;
            }
            remainingPackets--;

            short[] data = new short[4];

            // Bluetooth generates kind of random data
            data[0] = nodeID;
            data[1] = (short) (randGen.nextInt(2 * Short.MAX_VALUE) - Short.MAX_VALUE);
            data[2] = (short) (randGen.nextInt(2 * Short.MAX_VALUE) - Short.MAX_VALUE);
            data[3] = (short) (randGen.nextInt(2 * Short.MAX_VALUE) - Short.MAX_VALUE);
           // System.out.println("VirtualBTNode: data: " + data[0] + " " + data[1] + " " + data[2] + " " + data[3]);

            DataPacket dp = new DataPacket(data);
            dp.setTimestamp(System.currentTimeMillis());
            tTask.send(0, dp);
        }

        public void run() {
            if (tTask.isRunning()) {
                if(online) {
                    sendData();
                } else {
                    sendStatus();
                }
            } else {
                cancel();
            }
        }
    }
}
