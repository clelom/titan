package titancommon.node.tasks;

import java.util.ArrayList;
import titancommon.node.TitanTask;
import titancommon.node.DataPacket;
import titancommon.tasks.Magnitude;

/**
 * Magnitude = ( SUM ( (X - offset)/scaleFactor )^2 ) >> resultShift
 *
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class EMagnitude extends Magnitude implements ExecutableTitanTask {
  private TitanTask tTask;
  public void setTitanTask(TitanTask tsk) { tTask = tsk; }

  private int coordNum;
  private int offset;
  private int scaleFactor;
  private int resultShift;

  private ArrayList/*<Short>*/ dataBuffer[];

  public boolean setExecParameters(short[] param) {
    switch (param.length) {
      case 6:
        coordNum    = param[0];
        offset      = (param[1] << 8) + param[2];
        scaleFactor = (param[3] << 8) + param[4];
        resultShift = param[5];
        break;

      default:
        tTask.errSource = tTask.getRunID();
        tTask.errType = 4;  // ERROR_CONFIG
        return false;
    }

    return true;
  }

  public void init() {
    dataBuffer = new ArrayList/*<Short>*/[coordNum];
    for (int i = 0; i < coordNum; i++) {
      dataBuffer[i] = new ArrayList/*<Short>*/();
    }
  }

  public void inDataHandler(int port, DataPacket data) {
    if (data.sdata.length % 2 != 0) {
      System.err.println("uneven number of bytes in 16bit array");
      return;
    }

    for (int i = 0; i < data.sdata.length; i+= 2) {
      // little endian 16 bit value (signed)
      addValue((short) (data.sdata[i] + (data.sdata[i+1] << 8)), port);
    }
  }

  private void addValue(short val, int port) {
    dataBuffer[port].add(new Short(val));

    boolean doCalc = true;
    for (int i = 0; i < coordNum; i++) {
      if (dataBuffer[i].isEmpty()) {
        doCalc = false;
        break;
      }
    }

    if (doCalc) {
      short[] coval = new short[coordNum];
      for (int i = 0; i < coordNum; i++) {
        coval[i] = ((Short) dataBuffer[i].remove(0)).shortValue();
      }

      int magnitude = getMagnitude(coval);
      magnitude >>= resultShift;

      magnitude = ( (magnitude >= Short.MAX_VALUE) ? Short.MAX_VALUE : magnitude );


      // send data packet
      short[] data = new short[2];
      data[0] = (short) ( magnitude       & 0xFF);
      data[1] = (short) ((magnitude >> 8) & 0xFF);
      //System.out.println("Magnitude is " + (short)(data[0] + (data[1]<<8))); // ATTENTION: + kommt vor << !
      tTask.send(0, new DataPacket(data));

    }
  }

  private int getMagnitude(short[] data) {
    int sum = 0;
    for (int i = 0; i < data.length; i++) {
      int v = (int) data[i];
      v = (v - offset) / scaleFactor;
      v *= v;
      sum += v;
    }
    return sum;
  }
}
