package titancommon.route;

import titan.messages.SerialMsg;

/**
 *
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class TitanCommunicateRouter extends TitanCommunicateTCP {
  private TitanRouter router;

  public TitanCommunicateRouter(int mode, TitanRouter tr) {
    super(mode);
    router = tr;
  }

  // =================================================
  // | extended TitanCommunicateTCP methods for Routing
  // =================================================
  
  public boolean send(int moteId, SerialMsg m) {
    int dest_net = m.get_address() >> TitanRouter.CLIENT_BITS;

    if (router == null) { // this must be simulation
       return super.send(moteId, m);
    } if (dest_net == router.getNetID()) {
      return super.send(moteId, m);
    }

    // use messageReceived to send extern, since dest_net != net_id
    router.messageReceived(router.getNetID() << TitanRouter.CLIENT_BITS, m);
    return true;
  }

  public boolean sendIntern(int moteId, SerialMsg m) {
    return super.send(moteId, m);
  }
}
