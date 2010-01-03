package titancommon.node.tasks;

import titancommon.node.TitanTask;
import titancommon.node.DataPacket;
import titancommon.tasks.Max;

/**
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class EMax extends Max implements ExecutableTitanTask {
  private TitanTask tTask;
  public void setTitanTask(TitanTask tsk) { tTask = tsk; }

  private int winSize;
  private int winShift;
  private short dataWindow[];
  private int idx;

  public boolean setExecParameters(short[] param) {
    switch (param.length) {
      case 2:
        winSize = param[0];
        winShift = param[1];

        if ((winShift <= winSize) && (winSize > 0) && (winShift > 0))
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
      short max = getMaxVal();

      // send data packet
      short[] data = new short[2];
      data[0] = (short) ( max       & 0xFF);
      data[1] = (short) ((max >> 8) & 0xFF);
      tTask.send(0, new DataPacket(data));

      // copy data, shifted by winShift, set new write index
      idx = winSize - winShift;
      for (int i = 0; i < idx; i++) {
        dataWindow[i] = dataWindow[i + winShift];
      }
    }
  }

  private short getMaxVal() {
    // calculate max (signed shorts)
    short max = dataWindow[0];
    for (int i = 1; i < winSize; i++) {
      if (max < dataWindow[i])
        max = dataWindow[i];
    }
    return max;
  }
}
