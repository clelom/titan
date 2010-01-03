package titancommon.node;

import titan.messages.SerialMsg;
import titancommon.tasks.Communicator;

/**
 *
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class NodeConfig {
  private TitanLocalNode localNode;
  private int cfgID;
  private boolean bAutoStart;
  private int commTaskIdx;
  private TitanTask[] tasks;
  private int taskIdx;
  private int connCountTotal;
  private int connIdx;
  private int masterAddr;
  private boolean bComplete;
  private boolean bRunnable;
  private boolean bRunning;
  private int errSource;
  private int errType;

  public NodeConfig(TitanLocalNode lnode, int cid, boolean auto_start, int num_tasks, int num_conns, int maddr) {
    localNode = lnode;
    cfgID = cid;
    bAutoStart = auto_start;
    commTaskIdx = -1;
    tasks = new TitanTask[num_tasks];
    taskIdx = 0;
    connCountTotal = num_conns;
    connIdx = 0;
    masterAddr = maddr;
    bComplete = false;
    bRunnable = false;
    bRunning = false;
    errType = -1;
    errSource = -1;
  }

  public int       getConfigID()   { return cfgID; }
  public boolean   isAutoStart()   { return bAutoStart; }
  public int       getMasterAddr() { return masterAddr; }
  public boolean   isComplete()    { return bComplete; }
  public boolean   isRunnable()    { return bRunnable; }
  public boolean   isRunning()     { return bRunning; }
  public int       getErrSource()  { return errSource; }
  public int       getErrType()    { return errType; }
  public TitanTask getCommTask()   { return tasks[commTaskIdx]; }

  public int addTasks(SerialMsg msg, int idx) {
    int cid = msg.getElement_data(idx) >> 4;
    int task_cnt = msg.getElement_data(idx) & 0x0F;

    if (cid != cfgID) {
      System.err.println("invalid config id");
      return -1;
    }

    idx++;
    for (int t = 0; t < task_cnt; t++) {
      int taskID = (msg.getElement_data(idx) << 8) + msg.getElement_data(idx + 1);
      idx += 2;
      int runID = msg.getElement_data(idx++);
      short[] param = new short[msg.getElement_data(idx++)];
      for (int p = 0; p < param.length; p++) {
        param[p] = msg.getElement_data(idx + p);
      }
      idx += param.length;

      if (taskID == Communicator.TASKID) {
        if (commTaskIdx == -1) {
          commTaskIdx = taskIdx;
        }
        else {
          System.err.println("error: more than one COM task");
          return -1;
        }
      }
      
      if ( runID >= tasks.length ) {
         System.err.println("error: trying to configure too high runID: " + runID + " (taskID="+taskID+")");
         continue;
      }
      
      if ( tasks[runID] != null ) {
         if ( tasks[runID].getTaskID() != taskID ) {
            System.err.println("Error configuring task RunID="+runID+" taskID=" + taskID + ": differing existing taskID " + tasks[runID].getTaskID());
         }
         
         tasks[runID].addParameters(param);
      } else {
         tasks[taskIdx++] = new TitanTask(localNode, taskID, runID, param);
      }
    }

    checkConfigComplete();

    return idx;
  }

  public int addConnections(SerialMsg msg, int idx) {
    int cid = msg.getElement_data(idx) >> 4;
    int conn_cnt = msg.getElement_data(idx) & 0x0F;

    if (cid != cfgID) {
      System.err.println("invalid config id");
      return -1;
    }

    if (conn_cnt > connCountTotal - connIdx) {
      return -1;
    }

    idx++;
    for (int c = 0; c < conn_cnt; c++) {
      int to_id = msg.getElement_data(idx++);
      int to_port = msg.getElement_data(idx++);
      int ti_id = msg.getElement_data(idx++);
      int ti_port = msg.getElement_data(idx++);

      TitanTask outTask = null;
      TitanTask inTask = null;
      for (int t = 0; t < taskIdx; t++) {
        int rid = tasks[t].getRunID();
        if (rid == to_id) {
          outTask = tasks[t];
        }
        else if (rid == ti_id) {
          inTask = tasks[t];
        }
      }

      if (outTask == null || inTask == null) {
        System.err.println("invalid conection (task not found)");
        return -1;
      }

      outTask.addPortOut(to_port, inTask, ti_port);
      inTask.addPortIn(ti_port, outTask, to_port);

      connIdx++;
    }

    checkConfigComplete();

    return idx;
  }

  private void checkConfigComplete() {
    if ((taskIdx == tasks.length) && (connIdx == connCountTotal)) {
      bComplete = true;
      bRunnable = checkRunnable();
    }
  }

  private boolean checkRunnable() {
    for (int i = 0; i < tasks.length; i++) {
      if (!tasks[i].isRunnable()) {
        errSource = tasks[i].getErrSource();
        errType = tasks[i].getErrType();
        return false;
      }
    }
    return true;
  }

  public void start() {
    if (bRunning) return;

    for (int i = 0; i < tasks.length; i++) {
      tasks[i].start();
    }
    bRunning = true;
  }

  public void stop() {
    if (!bRunning) return;

    for (int i = 0; i < tasks.length; i++) {
      tasks[i].stop();
    }
    bRunning = false;
  }
}
