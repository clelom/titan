package titancommon.node.tasks;

import titancommon.node.TitanTask;
import titancommon.node.DataPacket;
import titancommon.tasks.BTSensor;
import titancommon.util.StringUtil;

import titancommon.bluetooth.*;

import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

/**
 * EBTSensor connects to a bluetooth sensor, monitors this connection and tries
 * to reconnect in case of LOSS. It provides two out ports, on the first it provides
 * the data read from the sensor and on the second status information are sent.
 * 
 * @author Jonas Huber <huberjo@ee.ethz.ch>
 * @author Benedikt Köppel <bkoeppel@ee.ethz.ch>
 */
public class EBTSensor extends BTSensor implements ExecutableTitanTask {

    private TitanTask tTask;
    private String address;
    private String dxstring;
    private ConfigFileReader m_config;
    private BTConnectionMonitor m_btmon;
    private BTConnection m_btc;
    private boolean zeroDataSent = false;

    public void setTitanTask(TitanTask tsk) {
        tTask = tsk;
    }

    public boolean setExecParameters(short[] param) {

        int param_nr = 0;   // remember, how many params have been read before

        // param contains several strings which are separated by '\0'.
        Vector temp = new Vector();
        for (int i = 0; i < param.length; i++) {
            if (param[i] != (short) '\0') {
                temp.add(new Short(param[i]));
            } else {
                // Now we have found a delimiter, so we create a string out of
                // the collected shorts and then add this string to our list
                // of labels.
                short[] t2 = new short[temp.size()];
                for (int j = 0; j < temp.size(); j++) {
                    t2[j] = ((Short) temp.elementAt(j)).shortValue();
                }

                if( param_nr == 0) {        // first param is address
                    address = StringUtil.toString(t2);
                    System.out.println("Address reconverted to " + address);
                    param_nr++;
                } else if ( param_nr == 1 ) {       // second param is DX-string
                    dxstring = StringUtil.toString(t2);
                    System.out.println("DX String reconverted to " + dxstring);
                    param_nr++;
                } else {
                    return false;
                }

                temp.clear();
            }
        }

        /* convert the short array back to char array */
        /*char[] address_c = new char[param.length];
        for (int i = 0; i < param.length; i++) {
            address_c[i] = (char) param[i];
        }

        this.address = new String(address_c);
        System.out.println("EBT setExecParameters: address = " + address);*/
        return true;
    }

    public void init() {

        m_config = new ConfigFileReader();
        m_config.addBTDevice(address);
        m_config.addFilter(dxstring);

        //TODO: encoding should be auto-guessed (with FrameDetector) or read from config file
        //second parameter is the name for the sensor, third parameter the encoding...
        // this will start the BT connection to 'address'

        m_btmon = new BTConnectionMonitor(address, address, dxstring);
        m_btmon.addBTConnection();
    }

    public void inDataHandler(int port, DataPacket data) {
        // no incoming ports
    }

    /**
     * This class establishes and monitors the connection to the bluetooth sensor
     */
    private class BTConnectionMonitor implements Observer {

        //private HashMap m_BTConnections = new HashMap();
        private String m_address;
        private String m_sensorname;
        private String m_format;

        /**
         * Constructor which sets the stuff one has to know about a bluetooth sensor.
         * 
         * @param address
         * @param sensorname
         * @param format
         */
        public BTConnectionMonitor(String address, String sensorname, String format) {
            m_address = address;
            m_sensorname = sensorname;
            m_format = format;
        }

        /**
         * Initializes a Bluetooth connection to the address specified in the constructor.
         */
        public void addBTConnection() {

            FrameParser fp = new FrameParser(m_sensorname, m_format, m_config.getDescription(m_format));
            System.out.println("FrameParser created");
            System.out.println("FrameParser has format " + fp.getFormat());

            // create an array, for each field in the FrameParser-Format, and send zeroes as first output
            // this works, because the FrameParser converts '-s' to 't' ==> all fields are exactly one character
            // only do this once
            if ( zeroDataSent == false ) {
                short[] zerodata = new short[fp.getFormat().length()];
                for( int i=0; i<zerodata.length; i++ ) {
                    zerodata[i] = 0;
                }
                DataPacket dp = new DataPacket(zerodata);
                dp.setTimestamp( System.currentTimeMillis() );
                tTask.send(0, dp);

                zeroDataSent = true;
            }

            fp.addObserver(new BTData());
            System.out.println("BTData Observer added to FrameParser");

            m_btc = new BTConnection(m_address, fp);
            System.out.println("BTConnection created");

            // Attach to the bt connection
            m_btc.addObserver(this);

            //new Thread(tcpserver).start();
            new Thread(m_btc).start();
            System.out.println("BTConnection started");

        }

        /**
         * This function get's called because of two different reasons:
         *  - status change of the BTConnection
         *  - repeatedly by the Timer which monitors the read function
         *
         * Definitions for staus:
         * -1           Connecting
         * -2           Connected
         * -3           LOS
         * 0,1,2,...    Number of bytes in the InputStreams buffer
         *
         * @todo Further investigate the delay imposed before a new BT-connection is added.
         */
        public void update(Observable obs, Object arg) {
            if (arg instanceof Short) {

                short[] st = new short[1];
                st[0] = ((Short) arg).shortValue();

                // In all cases, we send the received status on our status out port to
                // for further processing.
                DataPacket status = new DataPacket(st);
                // System.out.println("[EBTSensor] send(1, status/buffersize): " + status + " (" + ((Object) status).hashCode() + ")");
                tTask.send(1, status);

                // If we have connection loss, we try to reconnect.
                if (st[0] == -3) {

                    System.out.println("[EBTSensor] Lost connection. Reconnecting...");
                    // Wait some time to avoid uncoordinated creating of new
                    // BTConnection objects.
                    try {
                        Thread.sleep(2000);
                    } catch (Exception e) {
                        System.err.println("[EBTSensor] Received error while sleeping.");
                    }

                    // Reconnect
                    addBTConnection();
                }

            } else {
                System.out.println("[EBTSensor,BTConnectionManager] Observed thing is not a BTConnetion!");
            }
        }
    }

    /**
     * this class observes the FrameParser and sends all packages out on port 0 of the EBTSensor-Task
     */
    private class BTData implements Observer {

        public void update(Observable obs, Object arg) {
            //System.out.println("BTData.update called");

            if (arg instanceof BTDataPacket) {
                BTDataPacket bdp = (BTDataPacket) arg;

                // convert the DataPacket into a TitanPacket
                // send the bluetooth packet via port 0 (data port)
                //System.out.print("Bluetooth Data Packet: ");
                //System.out.println("Bluetooth Data Packet: " + bdp);
                DataPacket dp = new DataPacket(bdp.getShortArray());

                // set timestamp of dp to the original timestamp
                dp.setTimestamp(bdp.getTimestamp());

                //System.out.print("Titan Data Packet: ");
                //System.out.println("EBTSensor send(0, dp): " + dp + " (" + dp.getTimestamp() + ", " + ((Object)dp).hashCode() + ")");
                //System.out.println();
                tTask.send(0, dp);


            } else {
                System.out.println("EBTSensor: received unknown packet.");
            }

        }
    }
}
