package titancommon.node.tasks;

import titancommon.node.TitanTask;
import titancommon.node.DataPacket;
import titancommon.tasks.StdDev;

/**
 * This is exactly the same as the Variance Task, but because it takes the
 * square root before sending results, these remain smaller and fit probably
 * into the 16bit shorts used for inter task communication.
 *
 * better use the variance task with a result shift!
 *
 * @author Benedikt Köppel <bkoeppel@ee.ethz.ch>
 * @author Jonas Huber <huberjo@ee.ethz.ch>
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class EStdDev extends StdDev implements ExecutableTitanTask {
  private TitanTask tTask;
  public void setTitanTask(TitanTask tsk) { tTask = tsk; }

  private int winSize;
  private int winShift;
  private int resultShift = 0;
  private short dataWindow[];
  private int idx;

  public boolean setExecParameters(short[] param) {
    switch (param.length) {
      case 3:
        resultShift = param[2];

      case 2:
        winSize   = param[0];
        winShift  = param[1];

        if ((winShift <= winSize) && (winSize > 0) && (winShift > 0)
             && (resultShift >= 0))
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
      int sum = getSum();
      int sumSqr = getSumSqr();

      // This line contains the only difference to the variance tasks: sqrt()
      int var = (int)Math.sqrt(((sum * sum) / winSize) - (sumSqr / winSize));
      var >>= resultShift;

      // send data packet
      short[] data = new short[2];
      data[0] = (short) ( var       & 0xFF);
      data[1] = (short) ((var >> 8) & 0xFF);
      tTask.send(0, new DataPacket(data));
      //System.out.println("Var is " + data[1] + " " + data [0] );

      // copy data, shifted by winShift, set new write index
      idx = winSize - winShift;
      for (int i = 0; i < idx; i++) {
        dataWindow[i] = dataWindow[i + winShift];
      }
    }
  }

  private int getSum() {
    // calculate sum
    int sum = 0;
    for (int i = 0; i < winSize; i++) {
      sum += dataWindow[i];
    }
    return sum;
  }

  private int getSumSqr() {
    // calculate squared sum
    int sum = 0;
    for (int i = 0; i < winSize; i++) {
      sum += ((int) dataWindow[i]) * ((int) dataWindow[i]);
    }
    return sum;
  }
 }
