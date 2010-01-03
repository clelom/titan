package titancommon.route;

import net.tinyos.message.Message;
import net.tinyos.message.MessageListener;
import titan.messages.SerialMsg;
import titancommon.node.TitanLocalNode;

/**
 *
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public abstract class TitanRouter implements MessageListener {
  public static final int LISTEN_PORT = 24000;
  public static final int CLIENT_BITS = 10;
  public static final int CLIENT_MASK = ((1 << CLIENT_BITS) - 1);

  public static final boolean DEBUG_MSGS = false;

  protected int net_id;
  protected TitanCommunicateRouter tcom;

  public TitanRouter(int nid, int mode) {
    net_id = nid;
    tcom = new TitanCommunicateRouter(mode, this);
  }

  public int                    getNetID() { return net_id; }
  public TitanCommunicateRouter getTCom()  { return tcom; }

  synchronized public void messageReceived(int addr, Message msg) {
    SerialMsg smsg = (SerialMsg) msg;
    
    int dest_addr = smsg.get_address();
    if (dest_addr == 0xFFFF) {  // Java VM inconsistency
      dest_addr = -1;           // force -1
    }
    int dest_net = dest_addr >> CLIENT_BITS;
    int dest_client = dest_addr & CLIENT_MASK;

    int src_addr = addr;
    int src_net = net_id;
    int src_client = -1;
    if (src_addr >= 0) {
      src_net = src_addr >> CLIENT_BITS;
      src_client = src_addr & CLIENT_MASK;
    }

    if (DEBUG_MSGS) {
      System.out.println("R: " + dest_addr + " " + dest_net + " " + dest_client + " - "
                         + src_addr + " " + src_net + " " + src_client);
    }

    if ((dest_addr == 0) && (net_id == 0)) {  // direct hook for Master
      if (DEBUG_MSGS) {
        System.out.println("routed to TitanCommand");
      }
      tcom.sendToListener(src_addr, smsg);
    }
    else if (dest_addr != -1) {  // normal message, needs to be forwarded
      if (dest_net != net_id) {  // send over external connection
        if (DEBUG_MSGS) {
          System.out.println("routed to Extern");
        }
        sendExtern(dest_net, smsg);
      }
      else {  // internal routing
        if (    (src_net != dest_net)                               // message is from outside, have to send it
             || (dest_client >= TitanLocalNode.LOCAL_NODE_ID_BASE)  // cover this internal direction: tmote/bt/tcp -> tcp
             || (src_client >= TitanLocalNode.LOCAL_NODE_ID_BASE)   // and tcp -> tmote/bt/tcp
           ) {                                                      // but never tmote/bt -> tmote/bt (message would be duplicated)

          // extend here for local moteID mapping (maybe with stripped netID)
          if (DEBUG_MSGS) {
            System.out.println("routed to Intern");
          }
          tcom.sendIntern(/*dest_addr*/0, smsg);
        }
      }
    }
    else {  // broadcast
      if (DEBUG_MSGS) {
        System.out.println("broadcast msg");
      }
      if (net_id == 0) { // broadcast to connected routers (must only be used by MasterRouter 0)
        if (DEBUG_MSGS) {
          System.out.println("routed to Extern");
        }
        sendExtern(-1, smsg);
      }

      if (DEBUG_MSGS) {
        System.out.println("routed to Intern");
      }
      tcom.sendIntern(/*dest_addr*/0, smsg);
    }
  }

  abstract protected void sendExtern(int dest_net, SerialMsg msg);
}
