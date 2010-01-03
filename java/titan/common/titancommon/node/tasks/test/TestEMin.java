package titancommon.node.tasks.test;

import titancommon.node.TitanTask;
import titancommon.tasks.Min;

/**
 *
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class TestEMin extends TestTemplateOnePort {
  TestEMin(TitanTask t) { super(t); }

  public static void main(String[] args) {
    TestEMin test = new TestEMin(new TitanTask(null, Min.TASKID, 0, getPara()));
    test.stdTest(args);
  }
}
