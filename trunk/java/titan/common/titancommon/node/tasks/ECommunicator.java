package titancommon.node.tasks;

import titancommon.node.TitanTask;
import titancommon.node.DataPacket;
import titancommon.tasks.Communicator;

/**
 *
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class ECommunicator extends Communicator implements ExecutableTitanTask {
  private TitanTask tTask;
  public void setTitanTask(TitanTask tsk) { tTask = tsk; }

  private int destAddress[];
  private int destPort[];

  public boolean setExecParameters(short[] param) {
    if (param.length % 3 != 0) {
      tTask.errSource = tTask.getRunID();
      tTask.errType = 4;  // ERROR_CONFIG (is there a constant table for the ERR IDs?)
      return false;
    }

    int n = param.length / 3;
    destAddress = new int[n];
    destPort = new int[n];
    for (int i = 0; i < n; i++) {
      destAddress[i] = (param[3*i+0] << 8) + param[3*i+1];
      destPort[i] = param[3*i+2];
    }

    return true;
  }

  public void init() {
  }

  public void inDataHandler(int port, DataPacket data) {
    tTask.globalSend(destAddress[port], destPort[port], data);
  }
}
