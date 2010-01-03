package titancommon.node.tasks;

import titancommon.node.TitanTask;
import titancommon.node.DataPacket;
import titancommon.tasks.Splitter;

/**
 * splits an incoming array up, into several outgoing packets
 * 
 * @author Benedikt Köppel <bkoeppel@ee.ethz.ch>
 * @author Jonas Huber <huberjo@ee.ethz.ch>
 */
public class ESplitter extends Splitter implements ExecutableTitanTask {
  private TitanTask tTask;
  private int outports;
  public void setTitanTask(TitanTask tsk) { tTask = tsk; }

  public boolean setExecParameters(short[] param) {
      outports = 8; // TODO: should be set via config
      return true;
  }

  public void init() {
    // nothing to init
  }

  public void inDataHandler(int port, DataPacket data) {
    if (port != 0) {
      System.err.println("incoming data on port > 0");
      return;
    }
    for (int i = 0; i < outports && i < data.sdata.length ; i++ ) {
        //System.out.println("data.sdata.length is " + data.sdata.length + ", tTask.getPortsOutNum() is " + tTask.getPortsOutNum() );
        short[] senddata = new short[2];

        // take the i-th byte of the input data...
        senddata[0] = (short) (data.sdata[i] & 0xFF);    // low byte
        senddata[1] = (short) ((data.sdata[i] >> 8) & 0xFF);    // high byte

        DataPacket dp = new DataPacket(senddata);
        // and send it out on port i alone
        tTask.send(i, dp);
        //System.out.println("Splitter " + tTask.getRunID() + " sending " + senddata[1] + " " + senddata[0] + " on port " + i);
    }
  }
}
