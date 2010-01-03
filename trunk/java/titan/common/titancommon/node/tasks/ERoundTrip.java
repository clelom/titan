package titancommon.node.tasks;

import titancommon.node.TitanTask;
import titancommon.node.DataPacket;
import titancommon.tasks.RoundTrip;

/**
 *
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class ERoundTrip extends RoundTrip implements ExecutableTitanTask {
  private TitanTask tTask;
  public void setTitanTask(TitanTask tsk) { tTask = tsk; }

  private DataPacket testPacket;
  private long sendTime;
  private int count;
  private long sum;

  private final static int COUNT_MOD = 100;

  public boolean setExecParameters(short[] param) {
    return true;
  }

  public void init() {
    count = 0;
    sum = 0;
    short[] data = new short[2];
    testPacket = new DataPacket(data);
    sendTestPacket();
  }

  public void inDataHandler(int port, DataPacket data) {
    long rtTime = System.currentTimeMillis() - sendTime;
    sum += rtTime;
    count++;
    double avg = ((double) sum) / ((double) count);

    if (TitanTask.DEBUG_MSGS || (count % COUNT_MOD == 0)) {
      System.out.println("roundtrip #" + count + ": " + rtTime + " ms (avg: " + avg + " ms)");
    }
    
    sendTestPacket();
  }

  private void sendTestPacket() {
    sendTime = System.currentTimeMillis();
    tTask.send(0, testPacket);
  }
}
