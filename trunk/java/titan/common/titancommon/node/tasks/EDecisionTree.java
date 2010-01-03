package titancommon.node.tasks;

import titancommon.node.TitanTask;
import titancommon.node.DataPacket;
import titancommon.tasks.DecisionTree;

/**
 * decision tree classifier.
 *
 * @author Clemens Lombriser <lombriser@ife.ee.ethz.ch>
 */
public class EDecisionTree extends DecisionTree implements ExecutableTitanTask {
  private TitanTask tTask;
  public void setTitanTask(TitanTask tsk) { tTask = tsk; }

  /**
   * Used to build up the decision tree
   */
  private class TreeNode {
     public int operator;
     public int value;
     public TreeNode(int _operator, int _value) {
        operator = _operator;
        value = _value;
     }
     TreeNode nextTrue;
     TreeNode nextFalse;
  }

  private TreeNode [] m_treeNodes;
  private short [] m_input;
  
  
  public boolean setExecParameters(short[] param) {
     
     int nodesInMsg = 0;
     int curIndex   = 0;
     
     // check whether this is the first node
     if(m_treeNodes==null) {
        nodesInMsg = (param[0] & 0x7F);
        int totalNodes = param[1];
        int totalFeatures = param[2];
        m_input = new short[totalFeatures];
        System.out.println("EDecisionTree: Configuring DecisionTree with " + totalNodes + " nodes and " + totalFeatures + " features. First message contains "+nodesInMsg+" nodes");
        
        m_treeNodes = new TreeNode[totalNodes];
        
        curIndex = 3;
     } else {
        nodesInMsg = param[0];
        curIndex = 1;
     }
     
     int nextNode = 0;
     while(m_treeNodes[nextNode] != null && nextNode != m_treeNodes.length) nextNode++;

     if (nextNode==m_treeNodes.length) {
        System.out.println("Error: received too many decision tree nodes");
        
     }
     
     // instantiate nodes
     for (int i=0; i < nodesInMsg; i++ ) {
        
        TreeNode newNode = new TreeNode(param[curIndex++], (short)((param[curIndex++]<<8) + param[curIndex++] ));
        
        System.out.println("Adding node: operator " + newNode.operator + " value " + newNode.value + " at position " + nextNode);
        m_treeNodes[nextNode++] = newNode;
     }
     
     // check whether we are done
     if (m_treeNodes.length == nextNode) {
        
        BuildTreeResult levelNodes = new BuildTreeResult(0,1);
        int prevLevelNodes = 0;
        int curConfigNode = 0;
        
        do {
           levelNodes = configConstructTree(curConfigNode, 0, 0, levelNodes.levelChildren);
           System.out.println("Level has " + levelNodes.levelNodes + " nodes and " + levelNodes.levelChildren + " children");
           prevLevelNodes += levelNodes.levelNodes;
           curConfigNode += levelNodes.levelNodes;
        } while( levelNodes.levelChildren != 0 );
        
        if (prevLevelNodes+levelNodes.levelNodes != m_treeNodes.length) {
           System.err.println("EDecisionTree: Tree construction failed");
        }
     }
     
    return true;
  }

  public void init() {
  }

  public void inDataHandler(int port, DataPacket data) {
     
    if (data.sdata.length < 2) {
      System.err.println("No data delivered");
      return;
    }
     
    if ( port > m_input.length ) {
       System.err.println("Received data at nonexistent port");
    }

     m_input[port] = (short)(data.sdata[0] + (data.sdata[1] << 8));
     
     //TODO: do something more sophisticated here - check whether all inputs 
     //      have been updated
     if (port == 0) {
        //System.out.println("running classify now");
        classify();
     }
  }
  
  private class BuildTreeResult {
     int levelNodes;
     int levelChildren;
     public BuildTreeResult(int ln, int lc) {levelNodes=ln; levelChildren=lc;}
  }
  private BuildTreeResult configConstructTree(int curNode, int prevChildren, int prevLevelNodes, int totalLevelNodes) {
     if (prevLevelNodes >= totalLevelNodes) {
        return new BuildTreeResult(0, 0);
     }
     
     int localChildren = (m_treeNodes[curNode].operator == TDO_SETCLASS)? 0: 2;

     BuildTreeResult result = configConstructTree(curNode+1, prevChildren+localChildren, prevLevelNodes+1, totalLevelNodes);
     
     // if we have children, set references
     if (localChildren != 0) {
        m_treeNodes[curNode].nextTrue  = m_treeNodes[curNode + result.levelNodes + prevChildren+1];
        m_treeNodes[curNode].nextFalse = m_treeNodes[curNode + result.levelNodes + prevChildren+2];
        result.levelChildren += localChildren;
        System.out.println("Node operator: " + m_treeNodes[curNode].operator + " children (" + (curNode + result.levelNodes + prevChildren + 1) + "," +(curNode + result.levelNodes + prevChildren + 2) + ")");
     }
     
     
     result.levelNodes += 1;
  
     return result;
  }
  
  private void classify() {
     
     TreeNode curNode = m_treeNodes[0];
     
     while (curNode.operator != TDO_SETCLASS) {
        
        switch (curNode.operator&0xF0) {
           case TDO_SMALLER_THAN:
              curNode = (m_input[curNode.operator&0xF] < curNode.value) ? curNode.nextTrue : curNode.nextFalse;
              break;
           case TDO_LARGER_THAN:
              curNode = (m_input[curNode.operator&0xF] > curNode.value) ? curNode.nextTrue : curNode.nextFalse;
              break;
           case TDO_EQUAL:
              curNode = (m_input[curNode.operator&0xF] == curNode.value) ? curNode.nextTrue : curNode.nextFalse;
              break;
           default:
              System.err.println("Unknown operator");
              return;
        }
        
     }
     
     
      short[] data = new short[1];
      data[0] = (short) curNode.value;
      tTask.send(0, new DataPacket(data));
      //System.out.println("Sending Value " + data[0]);
  }


}
