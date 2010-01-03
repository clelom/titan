package titancommon.node.tasks.test;

import titancommon.node.TitanTask;
import titancommon.tasks.Duplicator;

/**
 *
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class TestEDuplicator extends TestTemplateOnePort {
  TestEDuplicator(TitanTask t) { super(t); }

  // just one parameter allowed
  public static short[] getPara() {
    short[] para = { 1 };  // outputs
    return para;
  }

  public static void main(String[] args) {
    TestEDuplicator test = new TestEDuplicator(new TitanTask(null, Duplicator.TASKID, 0, getPara()));
    test.stdTest(args);
  }
}
