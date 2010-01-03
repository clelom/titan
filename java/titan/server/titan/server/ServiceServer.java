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
 * The ServiceServer keeps a database of TaskGraphs available to Titan. It 
 * listens for incoming client connections, which can request applications 
 * that are possible to execute on the WSAN indicated by them.
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 * @date 12 July 2008
 * 
 */

package titan.server;

import java.net.ServerSocket;
import java.net.Socket;

public class ServiceServer extends Thread {
    
	/** TCP Port at whicht the server listens */
    private final static int LISTEN_BASE_PORT = 9000;
    /** server socket accepting connections */
    private ServerSocket m_ServerSocket;
    /** Path to the server's application resources */
    private static String SERVER_PATH = "titan/applications/";
    /** Service composer object */
    private ServiceComposer m_ServiceComposer;
    
    public ServiceServer()
    {
    	m_ServiceComposer = new ServiceComposer(SERVER_PATH);
    }
    
    /** Runs the server. This function listens for incoming connections and starts ConnectionHandlers 
     * on them.
     */
    public void run()
    {
    	// open up the server socket
        try {
            m_ServerSocket = new ServerSocket(LISTEN_BASE_PORT);
        } catch ( java.io.IOException e ) {
            System.err.println("ERROR: Could not open server socket on port " + (LISTEN_BASE_PORT) );
            return;
        }

        // wait for incoming connections
        System.out.println("\nListening on port " + LISTEN_BASE_PORT);
        while (true)
        {
            try {
                Socket socket = m_ServerSocket.accept();
                
                ConnectionHandler con = new ConnectionHandler(socket,SERVER_PATH,m_ServiceComposer);
                con.start();
                
            } catch ( java.io.IOException e ) {
                System.err.println("ERROR while processing new connection");
            }
        } // loop forever 
    }
    
    //////////////////////////////////////////////////////////////////////////
    // Test code
    /**
     * Opens up a server and lets it run.
     * 
     * @param args No command line parameters taken
     */
    public static void main(String [] args) {
    	ServiceServer ssrv = new ServiceServer();
    	ssrv.start();
    }
}
