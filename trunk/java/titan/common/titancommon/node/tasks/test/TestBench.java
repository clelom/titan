package titancommon.node.tasks.test;

import titancommon.node.DataPacket;
import titancommon.node.TitanTask;
import titancommon.node.tasks.ESink;
import titancommon.tasks.Sink;

/**
 *
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
abstract public class TestBench {
  public static final int NO_OF_RUNS_DEFAULT = 1000000;

  TitanTask task;
  TitanTask prod;
  TitanTask sink;
  
  boolean done;
  int runs;
  long totalTime;

  public TestBench(TitanTask t) {
    task = t;

    prod = new TitanTask(null, Sink.TASKID, 1, new short[0]);
    sink = new TitanTask(null, Sink.TASKID, 2, new short[0]);
    ESinkTest est = new ESinkTest();
    est.setTitanTask(sink);
    sink.setExecTask(est);
    
    setupPorts();

    prod.start();
    sink.start();
  }

  private class ESinkTest extends ESink {
    // provides inDataHandler hook
    public void inDataHandler(int port, DataPacket data) {
      recvData(port, data);
      super.inDataHandler(port, data);
    }
  }

  abstract void setupPorts();
  abstract void sendData();
  abstract void recvData(int port, DataPacket data);

  public int getNoOfRuns(String[] args) {
    if (args.length > 0) {
      return Integer.parseInt(args[0], 10);
    }
    return NO_OF_RUNS_DEFAULT;
  }

  public long getTimeTotal() {
    return totalTime;
  }

  public double getTimePerRun() {
    return ((double) totalTime) / ((double) runs);
  }

  public void run(int n) {
    runs = n;
    totalTime = 0;

    task.start();

    long startTime = System.currentTimeMillis();
    for (int i = 0; i < n; i++) {
      //synchronized (this) {  // not needed, if one packet results
        done = false;
      //}
      sendData();
      waitForResult();
    }
    totalTime = System.currentTimeMillis() - startTime;

    task.stop();
  }

  void waitForResult() {
    synchronized(this) {
      while (!done) {
        try { this.wait(); }
        catch (InterruptedException ie) { }
      }
    }
  }

  public void close() {
    prod.stop();
    sink.stop();
  }

  public void stdTest(String[] args) {
    int n = getNoOfRuns(args);
    System.out.println("no. of runs: " + n);
    run(n);
    close();
    System.out.println("per run: " + getTimePerRun());
    System.out.println("total: " + getTimeTotal());
  }
}
