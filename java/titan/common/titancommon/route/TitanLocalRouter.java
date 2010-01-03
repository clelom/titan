package titancommon.route;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import titan.TitanCommunicate;
import titan.messages.SerialMsg;
import titancommon.node.TitanLocalNode;
import titancommon.node.TitanTCP;

/**
 *
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class TitanLocalRouter extends TitanRouter {
  private TitanTCP tcpip;
  private boolean bStopped;

  public TitanLocalRouter(int net_id, int mode) {
    super(net_id, mode);
    tcom.registerListener(new SerialMsg(), this);
  }

  public boolean run() {
    bStopped = false;
    while (!bStopped) {
      ServerSocket server;

      try {
        server = new ServerSocket(LISTEN_PORT);
      }
      catch (IOException ioe) {
        System.err.println("TitanLocalRouter: could not start local router on port " + LISTEN_PORT);
        return false;
      }

      try {
        System.out.println("TitanLocalRouter: waiting for connection from MasterRouter... ");
        Socket s = server.accept();
        server.close();
        tcpip = new TitanTCP(s);
        if (!tcpip.isConnected()) {
          throw new IOException("TitanTCP not connected");
        }
      }
      catch (IOException ioe) {
        System.err.println("TitanLocalRouter: could not accept() tcp socket properly");
        return false;
      }

      System.out.println("TitanLocalRouter: connection to MasterRouter established");

      SerialMsg msg;
      while (((msg = tcpip.recvMsg()) != null) && !bStopped) {
        if (!tcom.sendIntern(/*msg.get_address()*/0, msg))  // first parameter? destination mote?
          break;
      }

      System.out.println("TitanLocalRouter: closing connection to MasterRouter");
      tcpip.disconnect();
    }

    return true;
  }

  public void stop() {
    bStopped = true;
  }

  protected void sendExtern(int dest_net, SerialMsg msg) {
    if (tcpip.isConnected()) {
      try {
        tcpip.sendMsg(msg);
      }
      catch (IOException ioe) {
        System.err.println("TitanLocalRouter: external send failed");
      }
    }
  }
  
  public static TitanLocalRouter create(String[] args) {
    if (args == null || args.length == 0) {
      System.err.println("TitanLocalRouter: missing net_id");
      return null;
    }

    int net_id = Integer.parseInt(args[0], 10);

    if (net_id < 1) {
      System.err.println("TitanLocalRouter net_id smaller than 1");
      return null;
    }

    int mode = TitanCommunicate.TCP_ONLY;
    if (args.length > 1) {
      if (args[1].compareToIgnoreCase("bt") == 0) {
        System.out.println("Creating TitanLocalRouter in BLUETOOTH MODE");
        mode = TitanCommunicate.BLUETOOTH;
      }
      else if (args[1].compareToIgnoreCase("tmote") == 0) {
        System.out.println("Creating TitanLocalRouter in TMOTE MODE");
        mode = TitanCommunicate.TMOTE;
      }
      else if (args[1].compareToIgnoreCase("tcp") == 0) {
        System.out.println("Creating TitanLocalRouter in TCP_ONLY MODE");
        mode = TitanCommunicate.TCP_ONLY;
      }
      else {
        System.err.println("TitanLocalRouter unknown mode: " + args[1]);
        return null;
      }
    }
    else {
      System.out.println("Creating TitanLocalRouter in default mode: TCP_ONLY");
    }

    return new TitanLocalRouter(net_id, mode);
  }

  private TitanLocalNode startNewLocalNode(int node_id, int tcp_port) {
    TitanLocalNode tln = new TitanLocalNode(node_id, tcp_port);
    if (tln.start()) {
      Thread.yield();  // let server thread go into accept(), if it is not already
      System.out.println("started local node (" + node_id + ") on tcp port " + tcp_port);
      tcom.TCPConnect(node_id, "localhost", tcp_port);
    }
    else {
      System.err.println("start of local node failed!");
      return null;
    }
    return tln;
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      args = new String[1];
      args[0] = "1";
    }
    
    TitanLocalRouter tlr = create(args);
    if (tlr == null) {
      System.exit(-1);
    }

    // start a new local node
    int node_id = (tlr.getNetID() << CLIENT_BITS) + TitanLocalNode.LOCAL_NODE_ID_BASE;
    int port = TitanLocalNode.LOCAL_NODE_TCP_PORT_BASE + TitanLocalNode.LOCAL_NODE_ID_BASE;
    tlr.startNewLocalNode(node_id, port);

    //node_id++;
    //port++;
    //tlr.startNewLocalNode(node_id, port);

    // run the router
    tlr.run();
  }
}
