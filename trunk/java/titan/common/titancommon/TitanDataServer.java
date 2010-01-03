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
 * TitanDataServer.java
 * 
 * Handles connections to one TCP port, and allows sending of string messages to multiple clients. 
 * The port is defined by LISTEN_BASE_PORT + Port given with the constructor. The connection is 
 * closed as soon as the client writes data.
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */

package titancommon;

import java.io.*;
import java.net.*;
import java.util.*;

public class TitanDataServer extends Thread {
    
    private final static int LISTEN_BASE_PORT = 8000; ///< TCP base port listening for incoming connections
    private int m_iPort;
    private ServerSocket m_ServerSocket; ///< Server socket
    
    private List/*<PrintStream>*/ m_TCPPrints = new ArrayList(); ///< List of print streams associated to the connections on the ports
    
    public TitanDataServer(int iPort)
    {
        m_iPort = iPort;
    }
    
    /**
     * Sends a message to all connected clients
     * @param strMessage message to be sent
     */
    public void send( String strMessage ) {
        for ( Iterator/*<PrintStream>*/ stream = m_TCPPrints.iterator(); stream.hasNext(); ) {
            PrintStream pStream = (PrintStream)stream.next();
            pStream.print(strMessage);
        }
    }
    
    
    public void run()
    {
        try {
            m_ServerSocket = new ServerSocket(LISTEN_BASE_PORT+m_iPort);
        } catch ( java.io.IOException e ) {
            System.err.println("ERROR: Could not open server socket on port " + (LISTEN_BASE_PORT+m_iPort) );
            return;
        }
        
        System.out.println("\nOpening port " + (LISTEN_BASE_PORT+m_iPort));
        
        while (true)
        {
            try {
                Socket socket = m_ServerSocket.accept();
                
                System.out.println("\nNew connection on port " + (LISTEN_BASE_PORT+m_iPort));
                
                OutputStream tcpStream = socket.getOutputStream();
                PrintStream  tcpPrint = new PrintStream(tcpStream);
                socket.shutdownInput(); // connection closes when client writes
                
                m_TCPPrints.add( tcpPrint );
            } catch ( java.io.IOException e ) {
                System.err.println("ERROR while processing new connection");
            }
        } // loop forever 
        
    }
    
}
