package titancommon.node.tasks;

import titancommon.node.TitanTask;
import titancommon.node.DataPacket;
import titancommon.tasks.ZeroCross;

/**
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class EZeroCross extends ZeroCross implements ExecutableTitanTask {
  private TitanTask tTask;
  public void setTitanTask(TitanTask tsk) { tTask = tsk; }

  private int winSize;
  private int winShift;
  private short threshLow;
  private short threshUp;
  private short dataWindow[];
  private int idx;

  public boolean setExecParameters(short[] param) {
    switch (param.length) {
      case 6:
        winSize   = param[0];
        winShift  = param[1];
        threshLow = (short) ((param[2] << 8) + param[3]);
        threshUp  = (short) ((param[4] << 8) + param[5]);

        if ((winShift <= winSize) && (winSize > 0) && (winShift > 0)
            && (threshLow > 0) && (threshUp > 0))
          break;

      default:
        tTask.errSource = tTask.getRunID();
        tTask.errType = 4;  // ERROR_CONFIG
        return false;
    }

    return true;
  }

  public void init() {
    idx = 0;
    dataWindow = new short[winSize];
  }

  public void inDataHandler(int port, DataPacket data) {
    if (port != 0) {
      System.err.println("incoming data on port > 0");
      return;
    }

    if (data.sdata.length % 2 != 0) {
      System.err.println("uneven number of bytes in 16bit array");
      return;
    }

    for (int i = 0; i < data.sdata.length; i+= 2) {
      // little endian 16 bit value (signed)
      addValue((short) (data.sdata[i] + (data.sdata[i+1] << 8)));
    }
  }

  private void addValue(short val) {
    dataWindow[idx++] = val;

    if (idx == winSize) {
      int cnt = getCrossings(getMean());

      // send data packet
      short[] data = new short[2];
      data[0] = (short) ( cnt       & 0xFF);
      data[1] = (short) ((cnt >> 8) & 0xFF);
      tTask.send(0, new DataPacket(data));

      // copy data, shifted by winShift, set new write index
      idx = winSize - winShift;
      for (int i = 0; i < idx; i++) {
        dataWindow[i] = dataWindow[i + winShift];
      }
    }
  }

  private int getMean() {
    // calculate mean
    int sum = 0;
    for (int i = 0; i < winSize; i++) {
      sum += dataWindow[i];
    }
    return sum / winSize;
  }

  private int getCrossings(int mean) {
    int cnt = 0;
    int pos = getPosition(mean, dataWindow[0]);
    int newpos;
    for (int i = 1; i < winSize; i++) {
      newpos = getPosition(mean, dataWindow[i]);
      if ((pos * newpos) == -1) {
        cnt++;
        pos = newpos;
      }
      else if (pos == 0) {
        pos = newpos;
      }
    }

    return cnt;
  }

  private int getPosition(int mean, int val) {
    if (val > (mean + threshUp))
      return 1;

    if (val < (mean - threshLow))
      return -1;

    return 0;
  }
}
