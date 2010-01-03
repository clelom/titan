package titancommon.node.tasks;

//import java.util.Date;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import titancommon.node.TitanTask;
import titancommon.node.DataPacket;
import titancommon.tasks.GSense;

/**
 * GSense reads the sensor data from the GSense application over TCP
 * with an adjustable data rate.
 * 
 * The 11 output ports are defined as:
 *  0: gv.X
 *  1: gv.Y
 *  2: gv.Z
 *  3: light [candela/m^2]
 *  4: g = sqrt(gv.X^2 + gv.Y^2 + gv.Z^2)
 *  5: walking (0 / 1)
 *  6: latitude (North: +)
 *  7: longitude (East: +)
 *  8: speed (1 knot = 1.852 km/h)
 *  9: heading (in degrees, 0 = north)
 * 10: altitude (sea level altitude in m)
 *
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class EGSense extends GSense implements ExecutableTitanTask {
  private TitanTask tTask;
  public void setTitanTask(TitanTask tsk) { tTask = tsk; }

  private static final int    GSENSE_VAL_BITS = 12;
  private static final double GSENSE_G_GRAV  = 9.80665;
  private static final double GSENSE_G_MAX = 32.0;
  private static final double GSENSE_LIGHT_MAX = 1000.0;
  private static final double GSENSE_SPEED_MAX = 150.0;
  private static final double GSENSE_ALTITUDE_MAX = 4000.0;

  private static final String GSENSE_TCP_HOST = "localhost";
  private static final int GSENSE_TCP_PORT = 27020;

  private static final int DEFAULT_INTERVAL_MS = 1000;
  private int interval_ms;

  private Socket socket;
  private OutputStream outStream;
  private InputStream inStream;

  private TimerTask watchdog;

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

    // try to establish connection with GSense server
    try {
      socket = new Socket(GSENSE_TCP_HOST, GSENSE_TCP_PORT);
      outStream = socket.getOutputStream();
      inStream = socket.getInputStream();
    }
    catch (Exception e) {
      System.err.println("Error: connection to GSense server failed!");
      tTask.errSource = tTask.getRunID();
      tTask.errType = 4;  // ERROR_CONFIG (is there a constant table for the IDs?)
      return false;
    }

    // set up gsense resource watchdog
    // needed if task gets canceled before it was started
    watchdog = new WatchDog();
    (new Timer()).schedule(watchdog, 0, 500);

    return true;
  }

  public void init() {
    // we got started, don't need the resource watchdog anymore
    watchdog.cancel();

    boolean failed = false;
    // check welcome message (11x "0")
    String[] gsdata = getData();
    if (gsdata != null) {
      for (int i = 0; i < gsdata.length; i++) {
        if (!gsdata[i].equals("0")) {
          failed = true;
          break;
        }
      }
    }
    else { failed = true; }
    
    if (!failed) {
      (new Timer()).schedule(new GSTimerTask(), 0, interval_ms);
    }
    else {
      System.err.println("handshake with GSense server failed");
      closeSocket();
    }
  }

  public void inDataHandler(int port, DataPacket data) {
    // no incoming ports
  }

  private void closeSocket() {
    try {
      // stop/restart GSense server
      socket.close();
    } catch (Exception e) { }
  }

  private String[] getData() {
    try {
      outStream.write('R');  // request new data
      outStream.flush();

      String str = "";
      int c = 0, i = 0;
      byte[] b = new byte[1];
      String[] data = new String[11];
      while ((c = inStream.read()) != '\n') {
        if (c == '|') {
          data[i++] = str;
          str = "";
        }
        else {
          b[0] = (byte) c;
          str += new String(b);
        }
      }
      data[i++] = str;

      if (i != 11) {
        System.err.println("unkown string format from GSense server");
        return null;
      }

      return data;
    }
    catch (IOException ioe) {
      System.err.println("I/O Exception in Task GSense");
    }

    return null;
  }

  private int encodeValue(double val, double low, double high, int bits) {
    val = (val < low) ? low : val;
    val = (val > high) ? high : val;
    double max = (high >= -low) ? high : -low;
    double v = val / max;
    v *= ((1 << (bits - 1)) - 1);
    return (int) v;
  }

  private int[] encodeData(String[] gsdata) {
    // gsdata contains 11 data values from GSense server
    int[] data = new int[11];

    // g sensor data
    for (int i = 0; i < 5; i++) {
      if (i == 3) continue;
      data[i] = encodeValue(Double.parseDouble(gsdata[i]) / GSENSE_G_GRAV, -GSENSE_G_MAX, GSENSE_G_MAX, GSENSE_VAL_BITS);
    }

    // light sensor
    data[3] = encodeValue(Double.parseDouble(gsdata[3]), 0, GSENSE_LIGHT_MAX, GSENSE_VAL_BITS);

    // walking detection
    data[5] = gsdata[5].equals("1") ? 1 : 0;

    // GPS data
    data[6] = encodeValue(Double.parseDouble(gsdata[6]), -90.0, 90.0, GSENSE_VAL_BITS);
    data[7] = encodeValue(Double.parseDouble(gsdata[7]), -180.0, 180.0, GSENSE_VAL_BITS);
    data[8] = encodeValue(Double.parseDouble(gsdata[8]), 0, GSENSE_SPEED_MAX, GSENSE_VAL_BITS);
    data[9] = encodeValue(Double.parseDouble(gsdata[9]), 0, 360.0, GSENSE_VAL_BITS);
    data[10] = encodeValue(Double.parseDouble(gsdata[10]), -GSENSE_ALTITUDE_MAX, GSENSE_ALTITUDE_MAX, GSENSE_VAL_BITS);

    return data;
  }

  private void sendData(int[] encdata) {
    String dbg = "";

    // outgoing data, 16-bit values (little endian)
    for (int i = 0; i < encdata.length; i++) {
      dbg += Integer.toString(encdata[i]) + " ";
      short[] data = new short[2];
      data[0] = (short) ( encdata[i]       & 0xFF);
      data[1] = (short) ((encdata[i] >> 8) & 0xFF);
      tTask.send(i, new DataPacket(data));
    }

    if (TitanTask.DEBUG_MSGS) {
      System.out.println("GS: " + dbg);
    }
  }

  private class WatchDog extends TimerTask {
    public void run() {
      if (tTask.isCanceled()) {
        closeSocket();
        cancel();
      }
    }
  }

  private class GSTimerTask extends TimerTask {
    public void run() {
      if (tTask.isRunning()) {
        sendData(encodeData(getData()));
      }
      else {
        closeSocket();
        cancel();
      }
    }
  }
}
