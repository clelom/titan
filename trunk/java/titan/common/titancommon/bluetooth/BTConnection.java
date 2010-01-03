/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package titancommon.bluetooth;

// Main is used for notification of shutdown
//import bluetoothgateway.Main
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

//import titancommon.node.tasks.EBTSensor;

import java.util.Observable;
import java.util.Timer;
import java.util.TimerTask;
//import java.util.Observer;

/**
 * Handles communication directly with a Bluetooth device.
 * 
 * @author Tonio Gsell <tgsell@ee.ethz.ch>
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */
public class BTConnection extends Observable implements Runnable {

    private String m_btConnectString;
    private String m_address;
    private StreamConnection m_StrmConn;
    private OutputStream m_Output;
    private InputStream m_Input;
    private Boolean m_IsConnected;
    private boolean m_bStop;
    private StreamDecoder m_sd;
    private int BUFFER_SIZE = 1;     // NOTE: This has to be 1!


    private long counter = 0;
    private int TIMEOUT = 3000;      // Timeout for the read() operation in ms
                                     // At *least* 2s because of new firmware

    /**
     * Initializes the connection. Use start() to start receiving data
     * @param address MAC address of the Bluetooth device to connect to
     */
    public BTConnection(String address, StreamDecoder sd) {
        m_address = address;
        m_btConnectString = "btspp://" + address + ":1;authenticate=true;encrypt=true;master=false";
        m_sd = sd;

        // Set bluetooth timeout to 1000ms (does not work as desired.)
        // BlueCoveImpl.setConfigProperty(BlueCoveConfigProperties.PROPERTY_CONNECT_TIMEOUT, String.valueOf(10000));
    }

    /**
     * Replaces the StreamDecoder used
     * @param sd The new stream decoder. It receives the next bytes read
     */
    public void changeDecoder(StreamDecoder sd) {
        m_sd = sd;
    }

    /**
     * Returns the remote bluetooth address of the BTConnection
     * @return
     */
    public String getAddress() {
        return m_address;
    }

    /**
     * Stops receiving data and shuts down the connection gracefully
     */
    public void stop() {
        m_bStop = true;
    }

    /**
     * Returns whether the connection is still alive
     * @return true if connection is still alive
     */
    public Boolean IsConnected() {
        return m_IsConnected;
    }

    /**
     * Worker thread - reads bytes from the connection and forwards them to the
     * FrameDecoder defined. This function will automatically shut down the
     * connection if errors are detected.
     */
    public void run() {
        m_IsConnected = Boolean.FALSE;
        m_bStop = false;

        // set up the streams
        System.out.println("Trying to connect to " + m_btConnectString);

        setChanged();
        notifyObservers(new Short((short) -1)); // Status 2 means: "connecting"
        clearChanged();
        try {
            m_StrmConn = (StreamConnection) Connector.open(m_btConnectString, Connector.READ, true);     // TODO: reconnect in Bluecove possible? Timeout settings?
            System.out.println("Connector.open done");
            m_Output = m_StrmConn.openOutputStream();
            System.out.println("openOutputStream done");
            m_Input = m_StrmConn.openInputStream();
            System.out.print("openInputStream done");
        } catch (IOException ex) {
            System.out.println("BTConnection: could not connect to: '" + m_btConnectString + "': " + ex.getLocalizedMessage());
            m_IsConnected = Boolean.FALSE;

            // TODO: Is this really good? This yields an infinite loop because the
            // observer immediately tries to reconnect!
            setChanged();
            notifyObservers(new Short((short) -3));
            clearChanged();
            return;
        }

        m_IsConnected = Boolean.TRUE;
        System.out.println("BTConnection: successfully connected to '" + m_btConnectString + "'");

        // We have a connection established: inform our observers
        setChanged();
        notifyObservers(new Short((short) -2)); // Status 1 means: "connected"
        clearChanged();


        // This timer checks periodically (e.g. 500ms) if the while loop below
        // blocks.
        // TODO: Improve reaction.
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {

            private long last = 0;

            public void run() {
                if (counter > last || counter == Long.MIN_VALUE ) {
                    last = counter;
                } else {
                    System.out.println("Read hangs!");

                    // Try to close the socket which is blocking the thread.
                    // But Javadoc say: close()-method does nothing...
                    // But it seems to work, at least basically.
                    try {
                        m_Output.close();
                        m_Input.close();
                        m_StrmConn.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    cancel();
                }
                short bufferSize = 0;
                setChanged();
                    try {
                        bufferSize = (short) m_Input.available();
                    } catch ( IOException e ) {
                        // do nothing...
                    }
                    // Send the number of available bytes...
                    notifyObservers( new Short(bufferSize ) );
                    clearChanged();
            }
        };
        timer.schedule(task, TIMEOUT, TIMEOUT);

        try {
            while (m_bStop == false) {

                // TODO: On connection loss, it hangs here for a while until read()
                // returns -1 and thud the loop breaks.
                byte[] bytes = new byte[BUFFER_SIZE];
                if (m_Input.read(bytes) == BUFFER_SIZE) {
                    //System.out.printf("%2X\n", bytes[0]); // print each received byte
                    m_sd.read(bytes);
                } else {
                    System.out.println("BTConnection to " + m_address + ": could not read full buffer from input stream, closing.");
                    break;
                }

                // TODO: What if counter reaches boundary +inf?
                // It seems as if it just gets cycled to -inf which is ok.
                // BUT ONLY IF APPROPRIATELY HANDLED ABOVE!!!
                counter = counter + 1;

            }
        } catch (IOException ex) {
            System.out.println("BTConnection: could not read from input stream, closing: " + ex.getLocalizedMessage());
        }


        m_IsConnected = Boolean.FALSE;

        // We have lost the connection: let's inform our observers
        setChanged();
        notifyObservers(new Short((short) -3)); // Status 0 means: "connection lost"
        clearChanged();

        // Closing connections
        try {
            m_Output.close();
            m_Input.close();
            m_StrmConn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
