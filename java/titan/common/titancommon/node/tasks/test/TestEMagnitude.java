package titancommon.node.tasks.test;

import titancommon.node.DataPacket;
import titancommon.node.TitanTask;
import titancommon.tasks.Magnitude;

/**
 *
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class TestEMagnitude extends TestBench {
  DataPacket datapkt;

  TestEMagnitude(TitanTask t) {
    super(t);

    short[] data = new short[2];
    data[0] = 1;
    data[1] = 2;
    datapkt = new DataPacket(data);
  }

  void setupPorts() {
    prod.addPortOut(0, task, 0);
    prod.addPortOut(1, task, 1);
    prod.addPortOut(2, task, 2);
    task.addPortIn(0, prod, 0);
    task.addPortIn(1, prod, 1);
    task.addPortIn(2, prod, 2);
    
    task.addPortOut(0, sink, 0);
    sink.addPortIn(0, task, 0);
  }

  void sendData() {
    prod.send(0, datapkt);
    prod.send(1, datapkt);
    prod.send(2, datapkt);
  }

  void recvData(int port, DataPacket data) {
    // set done on first incoming packet
    synchronized(this) {
      done = true;
      this.notify();
    }
  }

  public static short[] getPara() {
    short[] para = { 3, 0, 20, 0, 10, 2 };  // coordNum, offset (16), scaleFactor (16), resultShift;
    return para;
  }

  public static void main(String[] args) {
    TestEMagnitude test = new TestEMagnitude(new TitanTask(null, Magnitude.TASKID, 0, getPara()));
    test.stdTest(args);
  }
}
