package titancommon.route;

import java.io.IOException;
import titan.messages.SerialMsg;
import titancommon.node.TitanTCP;

/**
 *
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class TitanMasterRouter extends TitanRouter {
  private static final TitanTCP[] localRouters = {
    null,                                       // netID: 0 - MasterRouter itself
    new TitanTCP("cphones.test", LISTEN_PORT),  // netID: 1 - ?
    new TitanTCP("cphones.test2", LISTEN_PORT)  // netID: 2 - ?
  };

  private Thread[] conRecv;

  private class conReceiver implements Runnable {
    private int net_id;
    private TitanTCP tcpip;

    public conReceiver(int nid) {
      net_id = nid;
      tcpip = localRouters[nid];
    }

    public int getNetID() { return net_id; }

    public void run() {
      Thread thread = conRecv[net_id];
      SerialMsg msg;

      while (!thread.isInterrupted()
             && ((msg = tcpip.recvMsg()) != null)) {
        messageReceived(net_id << CLIENT_BITS, msg);
      }

      tcpip.disconnect();
      System.out.println("Connection to LocalRouter " + net_id + " closed!");
    }
  }

  public TitanMasterRouter(int mode) {
    super(0, mode);
    conRecv = new Thread[localRouters.length];
    initConnections();
  }
  
  private boolean initConnections() {
    boolean bSuccess = true;
    for (int i = 0; i < localRouters.length; i++) {
      if (localRouters[i] == null)
        continue;
      
      localRouters[i].connect();
      
      if (localRouters[i].isConnected()) {
        conRecv[i] = new Thread(new conReceiver(i));
        conRecv[i].start();
        System.out.println("Connection to LocalRouter " + i + " established!");
      }
      else {
        bSuccess = false;
      }
    }
    return bSuccess;
  }

  protected void sendExtern(int dest_net, SerialMsg msg) {
    if (dest_net != -1) {
      if ((localRouters.length <= dest_net) || (localRouters[dest_net] == null)) {
        System.err.println("TitanMasterRouter Error: unknown destination net " + dest_net);
        return;
      }

      if (localRouters[dest_net].isConnected()) {
        try {
          localRouters[dest_net].sendMsg(msg);
        }
        catch (IOException ioe) {
          System.err.println("TitanMasterRouter: external send failed (" + dest_net + ")");
        }
      }
    }
    else {  // broadcast
      for (int i = 0; i < localRouters.length; i++) {
        if ((localRouters[i] == null) || (!localRouters[i].isConnected()))
          continue;

        try {
          localRouters[i].sendMsg(msg);
        }
        catch (IOException ioe) {
          System.err.println("TitanMasterRouter: external send failed (" + dest_net + ")");
        }
      }
    }
  }
}
