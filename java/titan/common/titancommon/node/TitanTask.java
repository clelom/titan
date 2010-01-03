package titancommon.node;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;
import titancommon.node.tasks.*;
import titancommon.tasks.Task;

/**
 *
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class TitanTask implements Runnable {
  // specifiy available titan tasks, in no specific order
  private static final Class[] execTaskClasses = {
          ECommunicator.class,
          ESimpleWriter.class,
          EGSense.class,
          EGraphPlot.class,
          EMagnitude.class,
          EMax.class,
          EMean.class,
          EMin.class,
          EVariance.class,
          EZeroCross.class,
          ESink.class,
          EDuplicator.class,
          ERoundTrip.class,
          ETitanFileWriter.class,
          EBTSensor.class,
          ESyncMerger.class,
          EBTStatusGUI.class,
          ELabelingGUI.class,
          EVirtualBTSensor.class,
          EDecisionTree.class,
          EDemoGUI.class,
          ESplitter.class,
          EStreamSelector.class,
          EStdDev.class
  };

  public static final boolean DEBUG_MSGS = false;
  
  private TitanLocalNode localNode;
  private int taskID;
  private int runID;
  private short[] param;
  private int inBufCount;
  private final ArrayList/*<TPortBuf>*/ portsIn;
  private final ArrayList/*<TPort>*/ portsOut;
  private Thread execThread;
  private ExecutableTitanTask execTask;
  private boolean bRunnable;
  private boolean bRunning;
  private boolean bCanceled;
  
  public int errSource;
  public int errType;

  private class TPort {
    public int port;
    public TitanTask r_task;
    public int r_port;

    TPort(int p, TitanTask rt, int rp) {
      port = p;
      r_task = rt;
      r_port = rp;
    }
  }

  private class TPortBuf extends TPort {
    public final LinkedList/*<DataPacket>*/ buffer;

    TPortBuf(int p, TitanTask rt, int rp) {
      super(p, rt, rp);
      buffer = new LinkedList/*<DataPacket>*/();
    }
  }

  public TitanTask(TitanLocalNode lnode, int tID, int rID, short[] pa) {
    localNode = lnode;
    taskID = tID;
    runID = rID;
    param = pa;
    inBufCount = 0;
    portsIn = new ArrayList/*<TPortBuf>*/();
    portsOut = new ArrayList/*<TPort>*/();
    execThread = null;
    bRunnable = false;
    bRunning = false;
    bCanceled = false;
    errSource = -1;
    errType = -1;

    execTask = createExecTask(tID);
    if (execTask != null) {
      execTask.setTitanTask(this);
      bRunnable = execTask.setExecParameters(param);
    }
  }

  public int     getTaskID()      { return taskID; }
  public int     getRunID()       { return runID; }
  public short[] getParam()       { return param; }
  public boolean isRunnable()     { return bRunnable; }
  public boolean isRunning()      { return bRunning; }
  public boolean isCanceled()     { return bCanceled; }
  public int     getErrSource()   { return errSource; }
  public int     getErrType()     { return errType; }
  public int     getPortsInNum()  { return portsIn.size(); }
  public int     getPortsOutNum() { return portsOut.size(); }

  public void setExecTask(ExecutableTitanTask tsk) {
    execTask = tsk;
  }

  private ExecutableTitanTask createExecTask(int tID) {
    try {
      for (int i = 0; i < execTaskClasses.length; i++) {
        Class cls = execTaskClasses[i];
        //Task tsk = (Task) cls.getConstructor().newInstance();  // 1.4++
        Task tsk = (Task) cls.getConstructor(new Class[0]).newInstance(new Class[0]);  // 1.3
        if (tsk.getID() == tID) {
          return (ExecutableTitanTask) tsk;
        }
      }
    }
    catch (Exception e) { }

    errSource = runID;
    errType = 9;  // ERROR_NOT_IMPLEMENTED
    return null;
  }
  
  public void addParameters(short[] params) {
     bRunnable = execTask.setExecParameters(params);
  }

  public void addPortIn(int p, TitanTask t, int rp) {
    portsIn.add(new TPortBuf(p, t, rp));
  }

  public void addPortOut(int p, TitanTask t, int rp) {
    portsOut.add(new TPort(p, t, rp));
  }

  public void start() {
    bRunning = true;
    execThread = new Thread(this);
    execThread.start();
  }

  public void stop() {
    bRunning = false;
    bCanceled = true;
    if (execThread != null) {
      execThread.interrupt();
      synchronized(portsIn) {
        portsIn.notify();
      }
    }
  }

  public void run() {
    execTask.init();

    if (portsIn.size() == 0)
      return;

    while (!execThread.isInterrupted()) {
      while (inBufCount > 0) {
        // handle one packet from each input port, then repeat
        for (ListIterator li = portsIn.listIterator(); li.hasNext();) {
          TPortBuf tpb = (TPortBuf) li.next();
          if (tpb.buffer.size() > 0) {
            DataPacket data;
            synchronized (portsIn) {
              data = (DataPacket) tpb.buffer.removeFirst();
              inBufCount--;
            }
            execTask.inDataHandler(tpb.port, data);
          }
        }
      }

      synchronized (portsIn) {
        if (inBufCount == 0) {
          // wait for new data on input ports
          try {
            portsIn.wait();
          }
          catch (InterruptedException ie) { }
        }
      }
    }
  }

  public void send(int port, DataPacket data) {
    for (ListIterator li_out = portsOut.listIterator(); li_out.hasNext();) {
      TPort tp_out = (TPort) li_out.next();
      if (tp_out.port == port) {
        for (ListIterator li_in = tp_out.r_task.portsIn.listIterator(); li_in.hasNext();) {
          TPortBuf tpb_in = (TPortBuf) li_in.next();
          if (tpb_in.port == tp_out.r_port) {
            synchronized(tp_out.r_task.portsIn) {
              tpb_in.buffer.addLast(data);
              tp_out.r_task.inBufCount++;
              tp_out.r_task.portsIn.notify();
            }
            //Thread.yield();
            return;
          }
        }
      }
    }
  }

  public void globalSend(int address, int port, DataPacket dataPkt) {
    localNode.sendDataMsg(address, port, dataPkt.getDataArray());
  }
}
