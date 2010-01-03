package titancommon.node.tasks;

import titancommon.node.TitanTask;
import titancommon.node.DataPacket;
import titancommon.tasks.Duplicator;

/**
 * duplicates an incoming packet
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class EDuplicator extends Duplicator implements ExecutableTitanTask {
  private TitanTask tTask;
  public void setTitanTask(TitanTask tsk) { tTask = tsk; }

  public boolean setExecParameters(short[] param) {
    /*
    switch (param.length) {
      case 0:
      case 1:
        // config is valid if 0 or 1 byte parameters
        // default for 0 bytes is 2 out ports
        // the exec task does not need the out port count info,
        // since it is directly provided from the task config
        break;

      default:
        tTask.errSource = tTask.getRunID();
        tTask.errType = 4;  // ERROR_CONFIG
        return false;
    }
    */

    return true;
  }

  public void init() {
    // nothing to init
  }

  public void inDataHandler(int port, DataPacket data) {
    // incoming packets are not restricted to port 0 atm
    /*
    if (port != 0) {
      System.err.println("incoming data on port > 0");
      return;
    }
    */
    for (int i = 0; i < tTask.getPortsOutNum(); i++) {
      tTask.send(i, data);
      //System.out.println("Task " + tTask.getRunID() + " sending on port " + i);
      
    }
  }
}
