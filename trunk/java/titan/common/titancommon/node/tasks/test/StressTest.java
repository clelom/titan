/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package titancommon.node.tasks.test;

/**
 *
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class StressTest {
  public static final int WIN_SIZE = 4;
  public static final int WIN_SHIFT = 2;

  public static String getConfig(int n) {
    int start_node = 100;
    String text = "";
    text += "tasks=[\r\n"
          + "  SimpleWriter(10):(nodeID=" + start_node + "),\r\n"
          + "  Duplicator(" + n + "):(nodeID=" + start_node + ")";

    for (int i = 0; i < n; i++) {
      // limitation of only 15 tasks per node...
      int node = start_node + (i / 3) + 1;
      text += ",\r\n"
            + "  Duplicator(3):(nodeID=" + node + "),\r\n"
            + "  Mean(" + WIN_SIZE + "," + WIN_SHIFT + "):(nodeID=" + node + "),\r\n"
            + "  Variance(" + WIN_SIZE + "," + WIN_SHIFT + "):(nodeID=" + node + "),\r\n"
            + "  ZeroCross(" + WIN_SIZE + "," + WIN_SHIFT + ",10,20):(nodeID=" + node + "),\r\n"
            + "  Sink():(nodeID=" + node + ")";
    }

    text += "\r\n"
          + "]\r\n"
          + "\r\n"
          + "connections=[\r\n"
          + "  Connection(0,0, 1,0)";

    for (int i = 0; i < n; i++) {
      int base = i * 5 + 2;
      text += ",\r\n"
            + "  Connection(1," + i + ", " + base + ",0)";
    }

    for (int i = 0; i < n; i++) {
      int base = i * 5 + 2;
      text += ",\r\n"
            + "  Connection(" + base + ",0, " + (base + 1) + ",0),\r\n"
            + "  Connection(" + base + ",1, " + (base + 2) + ",0),\r\n"
            + "  Connection(" + base + ",2, " + (base + 3) + ",0),\r\n"
            + "  Connection(" + (base + 1) + ",0, " + (base + 4) + ",0),\r\n"
            + "  Connection(" + (base + 2) + ",0, " + (base + 4) + ",1),\r\n"
            + "  Connection(" + (base + 3) + ",0, " + (base + 4) + ",2)";
    }

    text += "\r\n"
          + "]\r\n";

    return text;
  }

  public static void main(String[] args) {
    System.out.println(getConfig(1));
    System.out.println(getConfig(2));
    System.out.println(getConfig(3));
  }
}
