package titancommon.node.tasks.test;

import titancommon.node.TitanTask;
import titancommon.tasks.Max;

/**
 *
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class TestEMax extends TestTemplateOnePort {
  TestEMax(TitanTask t) { super(t); }

  public static void main(String[] args) {
    TestEMax test = new TestEMax(new TitanTask(null, Max.TASKID, 0, getPara()));
    test.stdTest(args);
  }
}
