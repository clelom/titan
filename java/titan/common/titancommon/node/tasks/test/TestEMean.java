package titancommon.node.tasks.test;

import titancommon.node.TitanTask;
import titancommon.tasks.Mean;

/**
 *
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class TestEMean extends TestTemplateOnePort {
  TestEMean(TitanTask t) { super(t); }

  public static void main(String[] args) {
    TestEMean test = new TestEMean(new TitanTask(null, Mean.TASKID, 0, getPara()));
    test.stdTest(args);
  }
}
