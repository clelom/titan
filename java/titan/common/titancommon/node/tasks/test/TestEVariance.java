package titancommon.node.tasks.test;

import titancommon.node.TitanTask;
import titancommon.tasks.Variance;

/**
 *
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class TestEVariance extends TestTemplateOnePort {
  TestEVariance(TitanTask t) { super(t); }
  
  public static void main(String[] args) {
    TestEVariance test = new TestEVariance(new TitanTask(null, Variance.TASKID, 0, getPara()));
    test.stdTest(args);
  }
}
