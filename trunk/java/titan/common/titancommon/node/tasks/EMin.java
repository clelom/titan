package titancommon.node.tasks;

import titancommon.node.TitanTask;
import titancommon.node.DataPacket;
import titancommon.tasks.Min;

/**
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class EMin extends Min implements ExecutableTitanTask {
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
      short min = getMinVal();

      // send data packet
      short[] data = new short[2];
      data[0] = (short) ( min       & 0xFF);
      data[1] = (short) ((min >> 8) & 0xFF);
      tTask.send(0, new DataPacket(data));

      // copy data, shifted by winShift, set new write index
      idx = winSize - winShift;
      for (int i = 0; i < idx; i++) {
        dataWindow[i] = dataWindow[i + winShift];
      }
    }
  }

  private short getMinVal() {
    // calculate min (signed shorts)
    short min = dataWindow[0];
    for (int i = 1; i < winSize; i++) {
      if (min > dataWindow[i])
        min = dataWindow[i];
    }
    return min;
  }
}
