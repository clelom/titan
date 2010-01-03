package titancommon.node.tasks;

//import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import titancommon.node.TitanTask;
import titancommon.node.DataPacket;
import titancommon.tasks.SimpleWriter;

/**
 *
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class ESimpleWriter extends SimpleWriter implements ExecutableTitanTask {
  private TitanTask tTask;
  public void setTitanTask(TitanTask tsk) { tTask = tsk; }

  private static final int DEFAULT_INTERVAL_MS = 1000;
  private int interval_ms;

  public boolean setExecParameters(short[] param) {
    switch (param.length) {
      case 0:
        interval_ms = DEFAULT_INTERVAL_MS;
        break;

      case 2:
        interval_ms = (param[0] << 8) + param[1];
        
        break;

      default:
        tTask.errSource = tTask.getRunID();
        tTask.errType = 4;  // ERROR_CONFIG (is there a constant table for the IDs?)
        return false;
    }
    return true;
  }

  public void init() {
    (new Timer()).schedule(new SWTimerTask(), 0, interval_ms);
  }

  public void inDataHandler(int port, DataPacket data) {
    // no incoming ports
  }

  private class SWTimerTask extends TimerTask {
    private int counter = 0;
    //private long startTime = (new Date()).getTime();

    private void sendData() {
      //long t = (new Date()).getTime() - startTime;

      short[] data = new short[2];
      // example node also sends in 16bit little endian format?!
      data[0] = (short) ( counter       & 0xFF);
      data[1] = (short) ((counter >> 8) & 0xFF);
      counter++;

      DataPacket dp = new DataPacket(data);
      dp.setTimestamp(System.currentTimeMillis());
      tTask.send(0, dp);
    }
    
    public void run() {
      if (tTask.isRunning()) {
        sendData();
      }
      else {
        cancel();
      }
    }
  }
}
