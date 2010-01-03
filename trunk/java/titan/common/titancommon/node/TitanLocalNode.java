package titancommon.node;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.ListIterator;
import titan.messages.SerialMsg;
import titancommon.TitanCommand;

/**
 *
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class TitanLocalNode implements Runnable{
  public static final int LOCAL_NODE_ID_BASE = 100;
  public static final int LOCAL_NODE_TCP_PORT_BASE = 23000;
  public static final boolean DEBUG_MSGS = false;

  private int node_id;
  private int tcp_port;

  private ServerSocket server;
  private Thread       thread;
  private TitanTCP     tcpip;

  private NodeConfig   node_cfg;

  public TitanLocalNode(int nid, int port) {
    node_id = nid;
    tcp_port = port;
    server = null;
    thread = null;
    tcpip = null;
    node_cfg = null;
  }

  public int getNodeId()  { return node_id; }
  public int getTcpPort() { return tcp_port; }

  public boolean start() {
    try {
      server = new ServerSocket(tcp_port);
      thread = new Thread(this);
      thread.start();
    }
    catch (IOException ioe) {
      System.err.println("LN(" + node_id + "): tcp server: could not start local node");
      return false;
    }

    return true;
  }

  public void stop() {
    if (thread != null) {
      thread.interrupt();
    }
  }

  public void run() {
    try {
      Socket s = server.accept();
      server.close();
      tcpip = new TitanTCP(s);
      SerialMsg msg;

      while (!thread.isInterrupted()
              && (msg = tcpip.recvMsg()) != null) {
        processMsg(msg);
      }
    }
    catch (IOException ioe) {
      System.err.println("LN(" + node_id + "): could not accept() tcp socket");
    }

    // main thread is ending, stop graph and close tcp connection
    if (node_cfg != null) {
      node_cfg.stop();
    }
    if (tcpip != null) {
      tcpip.disconnect();
    }

    if (DEBUG_MSGS) {
      System.out.println("LN(" + node_id + "): local node stopped");
    }
  }

  private void processMsg(SerialMsg msg) {
    /*
    System.out.println();
    System.out.print("LN(" + node_id + ") new message (" + msg.get_length() + " bytes):");
    for (int i = 0; i < msg.get_length(); i++) {
      System.out.print(" " + msg.getElement_data(i));
    }
    System.out.println();
    */

    if (TitanCommand.TC_VERSION != (msg.getElement_data(0) >> 4)) {
      if (DEBUG_MSGS) {
        System.err.println("LN(" + node_id + "): incompatible TITANCOMM version");
      }
      //return;
    }

    switch ((msg.getElement_data(0) & 0xF)) {
      case TitanCommand.TITANCOMM_CONFIG:
        if (DEBUG_MSGS) {
          System.out.println("LN(" + node_id + "): Configuration message received!");
        }
        {
          boolean bDelayedReconfig = (msg.getElement_data(1) & 0x80) != 0;
          int num_tasks = msg.getElement_data(1) & 0x7F; //& 0x0F; (hack, to support more than 15 tasks on one node)
          int num_conns = msg.getElement_data(2);
          int master_addr = (msg.getElement_data(3) << 8) + msg.getElement_data(4);
          int cfg_id = msg.getElement_data(5) >> 4;

          if (num_tasks == 0) {
            if (node_cfg != null) {
              node_cfg.stop();
              node_cfg = null;
            }
            if (DEBUG_MSGS) {
              System.out.println("LN(" + node_id + "): node config reset");
            }
            break;
          }

          node_cfg = new NodeConfig(this, cfg_id, !bDelayedReconfig, num_tasks, num_conns, master_addr);

          int ret = node_cfg.addTasks(msg, 5);
          if (ret == -1) {
            System.err.println("LN(" + node_id + "): error in configuration message (tasks)");
            break;
          }

          if (msg.getElement_data(ret) != 0) {
            ret = node_cfg.addConnections(msg, ret);
            if (ret == -1) {
              System.err.println("LN(" + node_id + "): error in configuration message (connections)");
              break;
            }
          }

          checkConfigSuccess();
        }
        break;
      
      case TitanCommand.TITANCOMM_CFGTASK:
        if (DEBUG_MSGS) {
          System.out.println("LN(" + node_id + "): Task message received!");
        }
        {
          int ret = node_cfg.addTasks(msg, 1);
          if (ret == -1) {
            System.err.println("LN(" + node_id + "): error in configuration message (tasks)");
            break;
          }

          checkConfigSuccess();
        }
        break;

      case TitanCommand.TITANCOMM_CFGCONN:
        if (DEBUG_MSGS) {
          System.out.println("LN(" + node_id + "): Connection message received!");
        }
        {
          int ret = node_cfg.addConnections(msg, 1);
          if (ret == -1) {
            System.err.println("LN(" + node_id + "): error in configuration message (connections)");
            break;
          }

          checkConfigSuccess();
        }
        break;

      case TitanCommand.TITANCOMM_CFGSUCC:
        // should not be received
        break;

      case TitanCommand.TITANCOMM_FORWARD:
        // ?!
        break;

      case TitanCommand.TITANCOMM_DISCOVER:
        if (DEBUG_MSGS) {
          System.out.println("LN(" + node_id + "): Discovery message received!");
        }
        break;

      case TitanCommand.TITANCOMM_DICS_REP:
        // should not be received
        break;
      
      case TitanCommand.TITANCOMM_DATAMSG:
        if (DEBUG_MSGS) {
          System.out.println("LN(" + node_id + "): Data message received!");
        }
        {
          int port = msg.getElement_data(1);
          int data_len = msg.getElement_data(2);
          short[] data = new short[data_len];
          for (int i = 0; i < data_len; i++) {
            data[i] = msg.getElement_data(i + 3);
          }
          node_cfg.getCommTask().send(port, new DataPacket(data));
        }
        break;

      case TitanCommand.TITANCOMM_ERROR:
        // should not be received
        break;

      case TitanCommand.TITANCOMM_CFGSTART:
        if (DEBUG_MSGS) {
          System.out.println("LN(" + node_id + "): ConfigStart message received!");
        }
        {
          if (node_cfg != null) {
            if (node_cfg.isRunnable()) {
              node_cfg.start();
            }
            else {
              System.err.println("LN(" + node_id + "): config is not executable!");
            }
          }
          else {
            if (DEBUG_MSGS) {
              System.out.println("LN(" + node_id + "): no config to start");
            }
          }
        }
        break;

      default:
        System.err.println("LN(" + node_id + "): Unknown message of type: " + msg.amType() + "received!");
    }
  }

  private void checkConfigSuccess() {
     if (node_cfg.isComplete()) {
       if (node_cfg.isRunnable()) {
         sendSuccessMsg();
         if (node_cfg.isAutoStart()) {
           if (DEBUG_MSGS) {
             System.out.println("LN(" + node_id + "): auto-starting config");
           }
           node_cfg.start();
         }
       }
       else {
         sendErrorMsg(node_cfg.getErrSource(), node_cfg.getErrType());
       }
     }
  }

  private void sendSuccessMsg() {
    final int BODY_SIZE = 4;
    SerialMsg msg = new SerialMsg(BODY_SIZE + SerialMsg.DEFAULT_MESSAGE_SIZE);
    msg.set_length((short) BODY_SIZE);
    msg.set_address(node_cfg.getMasterAddr());
    short[] data = new short[BODY_SIZE];
    data[0] = (short) ((TitanCommand.TC_VERSION << 4) | TitanCommand.TITANCOMM_CFGSUCC);
    data[1] = (short) node_cfg.getConfigID();
    data[2] = (short) ((node_id >> 8) & 0xFF);
    data[3] = (short) (node_id & 0xFF);
    msg.set_data(data);

    if (DEBUG_MSGS) {
      System.out.println("LN(" + node_id + "): configuration successful, sending SUCCMSG to host...");
    }

    try {
      tcpip.sendMsg(msg);
    }
    catch (IOException ioe) {
      System.err.println("LN(" + node_id + "): could not send SuccessMsg");
    }
  }

  private void sendErrorMsg(int errSource, int errType) {
    final int BODY_SIZE = 6;
    SerialMsg msg = new SerialMsg(BODY_SIZE + SerialMsg.DEFAULT_MESSAGE_SIZE);
    msg.set_length((short) BODY_SIZE);
    msg.set_address(node_cfg.getMasterAddr());
    short[] data = new short[BODY_SIZE];
    data[0] = (short) ((TitanCommand.TC_VERSION << 4) | TitanCommand.TITANCOMM_ERROR);
    data[1] = (short) ((node_id >> 8) & 0xFF);
    data[2] = (short) (node_id & 0xFF);
    data[3] = (short) node_cfg.getConfigID();
    data[4] = (short) errSource;
    data[5] = (short) errType;
    msg.set_data(data);

    System.err.println("LN(" + node_id + "): configuration failed, sending ERRMSG to host...");

    try {
      tcpip.sendMsg(msg);
    }
    catch (IOException ioe) {
      System.err.println("LN(" + node_id + "): could not send ErrorMsg");
    }
  }

  public void sendDataMsg(int address, int port, short[] dataIn) {
    final int BODY_SIZE = dataIn.length + 3;
    SerialMsg msg = new SerialMsg(BODY_SIZE + SerialMsg.DEFAULT_MESSAGE_SIZE);
    msg.set_length((short) BODY_SIZE);
    msg.set_address(address);  // should always equal getMasterAddr()
    
    short[] data = new short[BODY_SIZE];
    data[0] = (short) ((TitanCommand.TC_VERSION << 4) | TitanCommand.TITANCOMM_DATAMSG);
    data[1] = (short) port;
    data[2] = (short) dataIn.length;
    for (int i = 0; i < dataIn.length; i++) {
      data[i + 3] = dataIn[i];
    }
    msg.set_data(data);

    //if (DEBUG_MSGS) {
    //  System.out.println("LN(" + node_id + "): sending DATAMSG to host...");
    //}
    
    try {
      tcpip.sendMsg(msg);
    }
    catch (IOException ioe) {
      System.err.println("LN(" + node_id + "): could not send DataMsg");
    }
  }

  public static TitanLocalNode getNodeById(List list, int id) {
    for (ListIterator li = list.listIterator(); li.hasNext(); ) {
      TitanLocalNode tln = (TitanLocalNode) li.next();
      if (tln.node_id == id)
        return tln;
    }
    return null;
  }
}
