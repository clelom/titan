package titancommon.node.tasks.test;

import titancommon.node.DataPacket;
import titancommon.node.TitanTask;

/**
 *
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class TestTemplateOnePort extends TestBench {
  DataPacket datapkt;

  TestTemplateOnePort(TitanTask t) {
    super(t);

    short[] data = new short[22];
    for (int i = 0; i < data.length; i++) {
      data[i] = (short) 42;
    }
    datapkt = new DataPacket(data);
  }

  void setupPorts() {
    prod.addPortOut(0, task, 0);
    task.addPortIn(0, prod, 0);
    
    task.addPortOut(0, sink, 0);
    sink.addPortIn(0, task, 0);
  }

  void sendData() {
    prod.send(0, datapkt);
  }

  void recvData(int port, DataPacket data) {
    // set done on first incoming packet
    synchronized(this) {
      done = true;
      this.notify();
    }
  }

  public static short[] getPara() {
    short[] para = { 11, 11 };  // winSize, winShift
    return para;
  }
}
