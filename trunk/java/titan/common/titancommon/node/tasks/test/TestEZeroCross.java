package titancommon.node.tasks.test;

import titancommon.node.TitanTask;
import titancommon.tasks.ZeroCross;

/**
 *
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class TestEZeroCross extends TestTemplateOnePort {
  TestEZeroCross(TitanTask t) { super(t); }

  // two extra parameters needed for ZeroCross
  public static short[] getPara() {
    short[] para = { 11, 11, 0, 3, 0, 8 };  // winSize, winShift, thrLow (16), thrUp (16)
    return para;
  }

  public static void main(String[] args) {
    TestEZeroCross test = new TestEZeroCross(new TitanTask(null, ZeroCross.TASKID, 0, getPara()));
    test.stdTest(args);
  }
}
