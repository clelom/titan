package titancommon.route;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import net.tinyos.message.MessageListener;
import titan.TitanCommunicate;
import titan.messages.SerialMsg;
import titancommon.node.TitanTCP;

/**
 *
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class TitanCommunicateTCP extends TitanCommunicate {
  // TCPIP connection interfaces
  private List/*<TitanTCPCon>*/ tcpConns = new ArrayList/*<TitanTCPCon>*/();
  private MessageListener tcpListener = null;

  private class TitanTCPCon {
    private int      nodeID;
    private TitanTCP tcpIp;
    private Thread   tcpRecvThread;
  }

  public TitanCommunicateTCP(int mode) {
    super(mode);
  }

  private TitanTCPCon getTCPCon(int node_id) {
    for (ListIterator li = tcpConns.listIterator(); li.hasNext();) {
      TitanTCPCon tcon = (TitanTCPCon) li.next();
      if (tcon.nodeID == node_id)
        return tcon;
    }
    return null;
  }

  public void TCPConnect(int node_id, String host, int port) {
    if (tcpListener == null) {
      System.err.println("TCP Listener not registered!");
      return;
    }

    if (getTCPCon(node_id) != null) {
      System.err.println("TCP Connection for node_id = " + node_id + " is already open!");
      return;
    }

    final TitanTCPCon tcon = new TitanTCPCon();
    tcon.nodeID = node_id;
    tcon.tcpIp = new TitanTCP(host, port);
    
    tcon.tcpIp.connect();
    if (!tcon.tcpIp.isConnected()) {
      return;
    }

    // start tcp receive thread, redirects messages to messageReceived()
    tcon.tcpRecvThread = new Thread( new Runnable() {
      public void run() {
        try {
          SerialMsg msg;
          while (!tcon.tcpRecvThread.isInterrupted()
                  && (msg = tcon.tcpIp.recvMsg()) != null) {
            tcpListener.messageReceived(tcon.nodeID, msg);
          }
        }
        catch (Exception e) { }
      } });
    tcon.tcpRecvThread.start();

    tcpConns.add(tcon);
  }

  public void TCPDisconnect(int node_id) {
    final TitanTCPCon tcon = getTCPCon(node_id);
    if (tcon == null) {
      System.err.println("TCP Connection Object for node_id = " + node_id + " not found!");
      return;
    }

    if (tcon.tcpRecvThread != null) {
      tcon.tcpRecvThread.interrupt();
    }
    tcon.tcpIp.disconnect();

    tcpConns.remove(tcon);
  }

  public void sendToListener(int moteId, SerialMsg m) {
    if (tcpListener != null) {
      tcpListener.messageReceived(moteId, m);
    }
  }
  
  // =================================================
  // | extended TitanCommunicate methods for TCP
  // =================================================
  
  public boolean send(int moteId, SerialMsg m) {
    try {
      short addr = (short) m.get_address();
      for (ListIterator li = tcpConns.listIterator(); li.hasNext(); ) {
        TitanTCPCon tcon = (TitanTCPCon) li.next();
        if (addr == (short) 0xFFFF) {  // check for broadcast
          tcon.tcpIp.sendMsg(m);
        }
        else if (tcon.nodeID == addr) {
          tcon.tcpIp.sendMsg(m);
          return true;
        }
      }
    }
    catch(IOException e) {
      System.err.println("TCP ERROR: Could not send packet: ");
      e.printStackTrace();
      return false;
    }
    
    return super.send(moteId, m);
  }
  
  synchronized public void registerListener(SerialMsg template, MessageListener listener) {
    tcpListener = listener;
    super.registerListener(template, listener);
  }
  
  synchronized public void deregisterListener(SerialMsg template, MessageListener listener) {
    if (tcpListener == listener) {
      tcpListener = null;
    }
    super.deregisterListener(template, listener);
  }
}
