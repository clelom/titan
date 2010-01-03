/*
 * TCPServer.java
 *
 */

package bluetoothgateway;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Observable;
import java.util.Observer;

/**
 * This class opens a server to stream out the data. Multiple client connections 
 * are supported and individually deliver data.
 * 
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */
public class TCPServer extends Observable implements Runnable, Observer {

   private static int m_nextport = 8000;
   
   private int m_port;
   private ServerSocket m_ServerSocket;
   
   /**
    * This class handles the input
    */
   private class Connection implements Observer {
      private Socket m_socket;
      private PrintStream m_print;
      private TCPServer m_server;
      
      /**
       * Registers as observer at TCPServer and associates printing functions 
       * with the socket. DataPackets can then be sent over the Observer 
       * interface.
       * @param sock Socket accepted by TCPServer
       * @param server TCPServer itself - used for registering as observer
       * @throws java.io.IOException in case the socket connections could not be handled properly
       */
      protected Connection(Socket sock, TCPServer server) throws IOException {
         m_socket = sock;
         m_server = server;

         m_server.addObserver(this);

         m_socket.setTcpNoDelay(true);
         m_print = new PrintStream(m_socket.getOutputStream());
         m_socket.shutdownInput(); // connection closes when client writes
      }

      /**
       * Receives incoming data packets
       * @param obs Observer having send the data packet - usually the TCPServer
       * @param arg the DataPacket to print to the connection
       */
      public void update(Observable obs, Object arg) {
         
         // print incoming data into the stream
         if ( arg instanceof DataPacket ) {
            DataPacket dp = (DataPacket)arg;
            m_print.print(dp);
            // write newline manually to see whether socket is open
            try {
               byte [] newline = {'\r','\n'};
               m_print.write(newline);
            } catch(IOException e) {
               close();
            }

            // check whether any errors occurred
            if(m_print.checkError()) {
               close();
            }

         } else {
            System.out.println("TCPServer.Connection: Unknown update received");
         }
      }
      
      private void close() {
         System.out.println("TCPServer.Connection: closing connection on port "+m_socket.getLocalPort());
         try {
            m_print.close();
            m_socket.close();
            m_server.deleteObserver(this);
         } catch (IOException ex) {
            // something failed when closing the socket
            System.err.println("TCPServer.Connection: could not close socket");
            ex.printStackTrace();
         }
      }
      
   }
   
   /**
    * Initializes a server and chooses a port - start with a thread to actually 
    * open the port
    */
   public TCPServer() {
      m_port = m_nextport++;
   }
   
   /**
    * Initializes a server at the given port number. Start thread to actually 
    * open the port
    * @param port Port number to run server on
    */
   public TCPServer(int port) {
      m_port = port;
   }
   

   /**
    * Listens on the given port for incoming connections. Upon a connection 
    * request, a connection object is associated with the connection.
    */
   public void run() {
        try {
            m_ServerSocket = new ServerSocket(m_port);
        } catch ( java.io.IOException e ) {
            System.err.println("ERROR: TCPServer: Could not open server socket on port " + m_port );
            return;
        }
        
        System.out.println("TCPServer: Opening port " + m_port);
        
        while (true)
        {
            try {
                Socket socket = m_ServerSocket.accept();
                new Connection(socket,this); // registers itself as observer
                System.out.println("TCPServer: Accepted connection on port " + m_port + " to port " + socket.getLocalPort());
                
            } catch ( java.io.IOException e ) {
                System.err.println("ERROR: TCPServer: could not associate streams to socket on port " + m_port);
            }
        } // loop forever 
   }

   public void update(Observable obs, Object arg) {
      if ( arg instanceof DataPacket ) {
         setChanged();
         notifyObservers(arg);
      } else {
         System.out.println("TCPServer: unknown update received");
      }
   }

}
