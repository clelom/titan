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

/**
 * SimChannel opens and handles a debug channel to the TOSSIM simulator.
 * It is running as a thread that keeps reading from the channel until 
 * stopped. Two variants have been programmed:
 * 
 * 1. Using pipes
 *    SimChannel opens a pipe in the TOSSIM simulation directory and 
 *    routes data from TOSSIM to the Java implementation.
 *    
 * 2. Using a TCP stream - THIS IS NOT WORKING
 *    Opens a TCP stream in TOSSIM and in Java. Unluckily _TOSSIM.dll 
 *    does not accept a socket as the destination for debugging output.
 *    
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * 
 */

package titan.simulation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Observer;

public class SimChannel extends Thread {

    /** Channel name */
    private String m_strChannel;

    /** TOSSIM connection */
    private Tossim m_Tossim;

    /** Used to stream the data */
    private InputStreamReader m_istr;

    private BufferedReader m_br;

    // these are needed for TCP channels
    private Socket m_Socket;

    private static int m_iChannelCounter = 57000;

    private int m_iChannel;

    /** The receiver of the channel data */
    ChannelNotices m_Observers = new ChannelNotices();

    /** Last received line */
    String m_strLine;

    /** Stops the execution of the reader */
    boolean m_bRunning = true;
    
    /** Prints every received line to stdout if true */
    boolean m_bPrint = false;

    /**
     * Constructs a SimChannel object
     * 
     * @param strChannel
     *                Channel name as used in the nesC dbg() function
     * @param tossim
     *                Reference to the TOSSIM simulator used
     * @param print
     *                Prints all lines also to stdout
     */
    public SimChannel(String strChannel, Tossim tossim, boolean print) {

        // store necessary data
        m_strChannel = strChannel;
        m_Tossim = tossim;
        m_iChannel = m_iChannelCounter++;
        m_bPrint = print;

        // actually open the channel
        OpenPipeChannel();
    }

    /**
     * This version opens a channel using a pipe
     */
    private void OpenPipeChannel() {

        // open a pipe in the filesystem to read data
        try {
            // workaround - somehow mkfifo does not always work right,
            // so just touch a file which will be replaced by a pipe
            Runtime.getRuntime().exec(
                    "cmd.exe /C \"touch channel" + m_strChannel + "\"", null,
                    m_Tossim.getDir());
            // create pipe
//            Runtime.getRuntime().exec(
//                    "cmd.exe /C \"mkfifo channel" + m_strChannel + "\"", null,
//                    m_Tossim.getDir());
        } catch (IOException e) {
            System.err.println("Could not create pipe! " + e.getMessage());
            return;
        }

        // open the pipe from TOSSIM
        // IMPORTANT: do this BEFORE opening it from this side
        m_Tossim.runFunc("channel" + m_strChannel + "=open('channel"
                + m_strChannel + "','w')");
        m_Tossim.runFunc("channel" + m_strChannel + ".write('Channel "
                + m_strChannel + "')"); // put in some initial data
        m_Tossim.runFunc("channel" + m_strChannel + ".flush()");
        m_Tossim.runFunc(m_Tossim.getID() + ".addChannel(\"" + m_strChannel
                + "\", channel" + m_strChannel + ")");

        // now open the pipe from the Java side (like a normal file)
        // IMPORTANT: this code will not work if nothing has been written to the
        // pipe yet!
        try {
            String m_strPath = m_Tossim.getDir().getAbsolutePath() + "\\channel" + m_strChannel;
            m_istr = new FileReader(m_strPath);
            m_br = new BufferedReader(m_istr);
        } catch (IOException e) {
            System.err.println("SimChannel("+m_strChannel+"): Could not open pipe! -> Has mkfifo created a *.lnk? Update mkfifo.");
        }
    }
    
    /**
     * Closes the pipe in TOSSIM and Java and cleanes up.
     *
     */
    private void ClosePipe() {

        // Close the pipe in TOSSIM
        m_Tossim.runFunc(m_Tossim.getID() + ".removeChannel(\"" + m_strChannel
                + "\", channel" + m_strChannel + ")");
        m_Tossim.runFunc("channel" + m_strChannel + ".close()");
        
        // close the pipe in Java and in the file system
        try {
            if (m_istr != null)
                m_istr.close();

            // delete the pipe file
//            String strDelete = "cmd.exe /C \"del channel" + m_strChannel + "\"";
//            Runtime.getRuntime().exec(strDelete, null, m_Tossim.getDir());
        } catch (IOException e) {
            System.err.println("Could not delete channel pipe");
        }
    }

    /**
     * This version opens a channel using a TCP stream. Sadly TOSSIM's
     * addChannel method only accepts files.
     */
    @SuppressWarnings("unused")
    private void OpenTCPChannel() {
        // instantiate TCPFile object
        m_Tossim.runFunc("from TCPFile import *");
        m_Tossim.runFunc("channel" + m_strChannel + "=TCPFile(" + m_iChannel
                + ")");

        // open server socket
        m_Tossim
        .runFuncAsync("channel" + m_strChannel + ".waitForConnection()");

        // connect to server socket
        try {
            m_Socket = new Socket(InetAddress.getByName("localhost"),
                    m_iChannel);
            m_istr = new InputStreamReader(m_Socket.getInputStream());
            m_br = new BufferedReader(m_istr);
        } catch (UnknownHostException e) {
            System.err
            .println("Could not open TCP stream: Unknown host (localhost???)");
        } catch (IOException e) {
            System.err.println("Could not open TCP stream");
        }

        // now add the channel
        m_Tossim.runFunc(m_Tossim.getID() + ".addChannel(\"" + m_strChannel
                + "\", channel" + m_strChannel + ")");

    }

    /**
     * This is the actual thread function. It keeps reading from the
     * BufferedReader and will notify the listener on new input
     */
    public void run() {

        // if there have been any problems to open the stream, stop immediately
        if (m_br == null)
            return;

        while (m_bRunning) {
            try {
                m_strLine = m_br.readLine();

                // check for actual data
                if (m_strLine == null) {
                    sleep(100);
                    continue;
                }
                
                // print to stdout
                if(m_bPrint){
                    System.out.println("[" + m_strChannel + "] " + m_strLine);
                    System.out.flush();
                }

                // notify the listener
                if (m_Observers != null) {
                    m_Observers.notifyObservers(m_strChannel, m_strLine);
                }

            } catch (IOException e) {
                System.err.println("Could not handle channel input");
            } catch (InterruptedException e) {
                // don't care about interruptions
            }
        } // while(m_bRunning)
    }

    /**
     * Stops the channel and close the streams/pipes
     *
     */
    public void stopChannel() {
        m_bRunning = false;
        
        ClosePipe();
    }

    /**
     * Registers a listener to receive notifications when data has 
     * been received on the channel
     * @param obs Reference to the object receiving the notifications
     */
    public void registerListener(Observer obs) {
        m_Observers.addObserver(obs);
    }

    /**
     * Deregisters a listener previously registered
     * @param obs the observer to be removed.
     */
    public void deregisterObserver(Observer obs) {
        m_Observers.deleteObserver(obs);
    }
}
