package titancommon.node.tasks.test;

/**
 *
 * @author Jeremy Constantin <jeremyc@student.ethz.ch>
 */
public class CompleteTest {
  public static void main(String[] args) {
    System.out.println("Duplicator");
    TestEDuplicator.main(args);
    System.out.println("Mean");
    TestEMean.main(args);
    System.out.println("Max");
    TestEMax.main(args);
    System.out.println("Min");
    TestEMin.main(args);
    System.out.println("Variance");
    TestEVariance.main(args);
    System.out.println("ZeroCross");
    TestEZeroCross.main(args);
    System.out.println("Magnitude");
    TestEMagnitude.main(args);
  }
}
