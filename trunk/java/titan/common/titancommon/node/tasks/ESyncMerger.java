package titancommon.node.tasks;

import titancommon.node.DataPacket;
import titancommon.node.TitanTask;
import titancommon.tasks.SyncMerger;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

/**
 * Merges the data coming in from a number of ports and sends the merged
 * data over the output port.
 *
 * @author Jonas Huber <huberjo@ee.ethz.ch>
 * @author Benedikt Köppel <bkoeppel@ee.ethz.ch>
 */
public class ESyncMerger extends SyncMerger implements ExecutableTitanTask {

    private TitanTask tTask;
    short numInputs;      // number of inputs
    short samplingPeriod;    // sampling period
    long nextSampleTime = System.currentTimeMillis();    // time of last sample
    long outputDelay = 1000;                 // fixed delay. that delay (after the current system time), the next package should be sent. TODO: set this via configuration
    DataPacketFIFO buffers[];           // contains all FIFO buffers
    private static final int DEFAULT_VALUES = 0;  // default values to return if not enough data is here
    SyncMergerProcessor packetProcessor;

    /**
     * handles incoming data and stores it into the corresponding buffer
     * @param port
     * @param data
     */
    public void inDataHandler(int port, DataPacket data) {

        // get the buffer corresponding to port, and add data into it
        //buffers.get(port).add(data);
        //if (buffers.length >= port ) {
            buffers[port].add(data);
        //}

        // TODO: check, if buffers are huge, and if so, delete them
    }

    public void init() {
        // initialize buffers
        // TODO: what happens, if we have more buffers than actual inputs?
        buffers = new DataPacketFIFO[numInputs];
        for (int i = 0; i < numInputs; i++) {
            buffers[i] = new DataPacketFIFO();
        }

        // get a packetProcessor, who processes all the packets
        SyncMergerProcessor packetProc = new SyncMergerProcessor();

        // Starts the timer, which will check the buffer and synchronize the output
        (new Timer()).schedule(new SyncMergerTimerTask(packetProc), 0, samplingPeriod);

    }

    public boolean setExecParameters(short[] param) {
        numInputs = param[0];
        samplingPeriod = param[1];
        outputDelay = param[2] + 256 * param[3];
        return true;
    }

    public void setTitanTask(TitanTask tsk) {
        tTask = tsk;
    }

    private class DataPacketFIFO /*extends Observable*/ {

        Vector elements = new Vector();
        // number of elements per each packet
        int numValues = 0;

        public void add(DataPacket e) {
            elements.add(e);

            if (numValues == 0) {          // haven't set the length before
                numValues = e.getDataArray().length;
            }

            // if the buffer is huge, we'll just delete all the values and start from scratch
            // this should prevent a outOfMemoryError
            if (isHugeBuffer(samplingPeriod,outputDelay)) {
                System.out.println("This buffer is huge! Cleaning up, to prevent an outOfMemoryError.");
                elements.clear();
            }

        }

        /**
         * returns that amount of zeros, as a normal packet would have
         * @return
         */
        public short[] getDefaultValues() {
            short[] zero = new short[numValues];
            for (int i = 0; i < numValues; i++) {
                zero[i] = DEFAULT_VALUES;
            }
            return zero;
        }

        /**
         * interpolates between the last and the following packet of timestamp
         */
        public short[] interpolate(long timestamp) /*throws NoPacketException*/ {

            cleanUp(timestamp);


            boolean hasOldPacket = false;
            DataPacket oldPacket = null;
            try {
                oldPacket = peekNewestOlderPacket(timestamp);
                hasOldPacket = true;
            } catch (NoPacketException e) {
                hasOldPacket = false;
            }

            boolean hasNewPacket = false;
            DataPacket newPacket = null;
            try {
                newPacket = peekOldestNewerPacket(timestamp);
                hasNewPacket = true;
            } catch (NoPacketException e) {
                hasNewPacket = false;
            }



            if (hasNewPacket && hasOldPacket) {
                // found both needed packets, so we can do the interpolation

                // to interpolate, we need the data and the timestamp
                short[] newData = newPacket.getDataArray();
                long newTime = newPacket.getTimestamp();

                short[] oldData = oldPacket.getDataArray();
                long oldTime = oldPacket.getTimestamp();

                // to calculate the slope, we need deltaValue and deltaTime
                int[] deltaData = new int[numValues];
                
                long deltaTime = newTime - oldTime;

                double slope[] = new double[numValues];

                double value;
                short[] values = new short[numValues];

                
                for (int i = 0; i < numValues; i++) {
                    // calculate deltas
                    deltaData[i] = newData[i] - oldData[i];


                    if (deltaTime == 0) {
                        // we're interpolating between the same packet and itself, because the timestamp matches exactly
                        // so the slope does not matter, because the deltaTime will be zero...
                        slope[i] = 0;
                    } else {
                        slope[i] = ((double) deltaData[i]) / ((double) deltaTime);
                    }

                    // interpolate linearly
                    value = oldData[i] + slope[i] * (timestamp - oldTime);

                    // check short boundaries
                    if (value > Short.MAX_VALUE) {
                        values[i] = Short.MAX_VALUE;
                    } else if (value < Short.MIN_VALUE) {
                        values[i] = Short.MIN_VALUE;
                    } else {
                        values[i] = (short) value;
                    }
                }
                return values;

            } else {
                // not all data found, so we return the default value. we can't wait longer, because this was specified with outputDelay
                return getDefaultValues();
            }




        }

        /**
         * get the newest package, which is older than timestamp, from the selected buffer. without removing it
         * @param timestamp
         * @reutrn the newest, older package
         * TODO: after a cleanUp, we could simply use the newest and the oldest packet... or not?
         */
        public DataPacket peekNewestOlderPacket(long timestamp) throws NoPacketException {
            cleanUp(timestamp);
            if ( elements.size() > 0 ) {
                return ((DataPacket)elements.get(0));
            } else {
                throw new NoPacketException();
            }
        }
        /*public DataPacket peekNewestOlderPacket(long timestamp) throws NoPacketException {

            // if the oldest element is newer than the parameter timestamp, return null, because there is no packet older
            if (elements.size() > 0 && ((DataPacket) elements.get(0)).getTimestamp() > timestamp) {
                throw new NoPacketException();
            }

            // if the newest element is older than the parameter timestam, return the newest
            if (elements.size() > 0 && ((DataPacket) elements.lastElement()).getTimestamp() <= timestamp) {
                return ((DataPacket) elements.lastElement());
            }

            // cycle through all elements
            for (int i = 0; i < elements.size() - 1; i++) {
                // if this element has an older timestamp, but the next one has a newer timestamp (compared to parameter timestamp), this element is the newest one, which is older than the paraemter timestamp
                if (((DataPacket) elements.get(i)).getTimestamp() <= timestamp && ((DataPacket) elements.get(i + 1)).getTimestamp() > timestamp) {
                    return ((DataPacket) elements.get(i));
                }
            }

            throw new NoPacketException();
        }*/

        public DataPacket peekOldestNewerPacket(long timestamp) throws NoPacketException {
            cleanUp(timestamp);
            if ( elements.size() > 1 ) {
                return ((DataPacket)elements.get(1));
            } else {
                throw new NoPacketException();
            }
        }
        /*public DataPacket peekOldestNewerPacket(long timestamp) throws NoPacketException {

            // if the newest element is older than the parameter timestamp, return null, because there is no packet newer
            if (elements.size() > 0 && ((DataPacket) elements.lastElement()).getTimestamp() < timestamp) {
                throw new NoPacketException();
            }

            // if the oldest element is newer than the parameter timestamp, return the newest
            if (elements.size() > 0 && ((DataPacket) elements.get(0)).getTimestamp() >= timestamp) {
                return ((DataPacket) elements.get(0));
            }

            // cycle through all elements
            for (int i = 1; i < elements.size(); i++) {
                // if this element has a newer timestamp, but the previous one has an older timestamp (compared to parameter timestamp), this element is the oldest one, which is newer than the parameter timestamp
                if (((DataPacket) elements.get(i)).getTimestamp() >= timestamp && ((DataPacket) elements.get(i - 1)).getTimestamp() < timestamp) {
                    return ((DataPacket) elements.get(i));
                }
            }

            throw new NoPacketException();
        }*/

        /**
         * will clean up the buffer. it keeps only one packet older than timestamp, and all packets newer than timestmap
         * so the interpolation should be done with element 0 and element 1
         * @param timestamp
         */
        public void cleanUp(long timestamp) {

            int i = 0;

            // if the second oldest element is still older than timestamp, we can delete the oldest
            // get(1), because we're looking at the second-oldest packet
            while (elements.size() > 1 && ((DataPacket) elements.get(1)).getTimestamp() <= timestamp) {
                elements.remove(0);
                i = i + 1;
            }
            //System.out.println("Cleaned up buffer. Removed " + i + " elements. Oldest Timestamp: " + ((DataPacket)elements.firstElement()).getTimestamp() + " Newest Timestamp: " + ((DataPacket)elements.lastElement()).getTimestamp() + " Current Timestamp: " + System.currentTimeMillis() );
        }

        /**
         * checks, if there is 10 times more data in the buffers than normally expected
         * @return
         */
        private boolean isHugeBuffer(short samplingPeriod, long outputDelay) {
            if ( elements.size() >  outputDelay/samplingPeriod*10) {
                return true;
            } else {
                return false;
            }
        }


    }

    private class NoPacketException extends Exception {
    }

    private class SyncMergerProcessor {

        private Vector interpolatedData = new Vector();

        public SyncMergerProcessor() { }

        /**
         *
         * @return is true, if packet is sent. else it is false
         */
        public boolean sendSynchronizedData() {

            // check, if we should generate a packet, that is newer as the (current time)-(fixed delay)
            // although there might be all needed values already in the FIFO, we don't want to do that, because we want to have (an approximately) constant output rate of the SyncMerger
            if (nextSampleTime > System.currentTimeMillis() - outputDelay) {
                //System.out.println("Can't send data for nextSampleTime=" + nextSampleTime + " because nextSampleTime > " + (System.currentTimeMillis() - outputDelay) );
                return false;
            }

            // go through all buffers
            for (int i = 0; i < buffers.length; i++) {

                short[] interpolatedDataShort = buffers[i].interpolate(nextSampleTime);

                // add all values to the interpolatedData vector
                for (int j = 0; j < interpolatedDataShort.length; j++) {
                    interpolatedData.add(new Short(interpolatedDataShort[j]));
                }

                // TODO: remove this!
                // add the buffer length to the output
                int bl = buffers[i].elements.size();
                if ( bl >= Short.MAX_VALUE ) {
                    interpolatedData.add( new Short(Short.MAX_VALUE) );
                } else if ( bl <= Short.MIN_VALUE ) {
                    interpolatedData.add( new Short(Short.MIN_VALUE) );
                } else {
                    interpolatedData.add( new Short((short)bl) );
                }

            }


            // convert the vector to a short array
            // size + 1 because we want to store the time difference as first element
            short[] sendData = new short[interpolatedData.size() +1];
            //System.out.print("Sending Packet ");
            for (int i = 0; i < interpolatedData.size(); i++) {
                // +1 here for sendData, because the first element is reserved for the time difference
                // no +1 for the get(i), because the vector interpolatedData is not shiftet
                // first element of interpolatedData goes to second element of sendData.
                sendData[i+1] = ((Short) interpolatedData.get(i)).shortValue();
                //System.out.print(sendData[i] + ", ");
            }
            //System.out.println();

            // time difference as first element
            long diff = nextSampleTime - System.currentTimeMillis();
            if ( diff >= Short.MAX_VALUE ) {
                sendData[0] = Short.MAX_VALUE;
            } else if ( diff <= Short.MIN_VALUE ) {
                sendData[0] = Short.MIN_VALUE;
            } else {
                sendData[0] = (short)diff;
            }

            // send the data
            DataPacket dp = new DataPacket(sendData);
            dp.setTimestamp(nextSampleTime);

            //System.out.println("Sending Packet with Timestamp " + nextSampleTime + ". Actual Time is " + System.currentTimeMillis() + ". Diff = " + (nextSampleTime - System.currentTimeMillis()));

            tTask.send(0, dp);

            // reset the vector
            interpolatedData.clear();

            // increase the nextSampleTime
            nextSampleTime = nextSampleTime + samplingPeriod;

            // clean up all buffers
            for (int i = 0; i < buffers.length; i++) {
                buffers[i].cleanUp(nextSampleTime);
                //System.out.println("Cleaned up buffer " + i + ", new size: " + ((DataPacketFIFO)buffers.get(i)).getNumOfElements());
                }

            return true;
        }
    }

    private class SyncMergerTimerTask extends TimerTask {

        SyncMergerProcessor processor;

        public SyncMergerTimerTask(SyncMergerProcessor p) {
            processor = p;
        }

        public void run() {
            if (tTask.isRunning()) {
                boolean dataSent;

                // try to calculate new data and sent them.
                // if that was okay (sendSynchronizedData returns true then), try again
                // sendSynchronizedData will find out, if it is too early to send the next packet and then return with false (and not send a packet of course)
                do {
                    //System.out.println("Trying to send Data for timestamp " + nextSampleTime);
                    dataSent = processor.sendSynchronizedData();
                } while (dataSent);
                //System.out.println("Going to sleep.\n");
            } else {
                cancel();
            }
        }
    }
}
