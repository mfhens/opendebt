package dk.ufst.opendebt.payment.immudb;

/**
 * Production adapter wrapping {@code io.codenotary.immudb4j.ImmuClient} (ADR-0029, TB-029).
 *
 * <p><b>NOT COMPILED IN SPIKE (TB-028)</b> — this class is provided as a reference implementation
 * showing the exact immudb4j API binding. It will be activated in TB-029 once:
 *
 * <ol>
 *   <li>{@code io.codenotary:immudb4j} is added to {@code opendebt-payment-service/pom.xml}
 *   <li>The dependency resolves (Maven Central or UFST Artifactory proxy)
 *   <li>No classpath conflicts exist with existing transitive dependencies
 *   <li>ADR-0029 moves from Proposed to Accepted
 * </ol>
 *
 * <p>When activating: delete {@link SpikeStubImmudbAdapter}, add the immudb4j dependency to
 * pom.xml, uncomment the imports below, and update {@link
 * dk.ufst.opendebt.payment.config.ImmudbConfig} to instantiate this class.
 *
 * <pre>
 * // Uncomment when immudb4j JAR is available:
 * // import io.codenotary.immudb4j.ImmuClient;
 *
 * public class RealImmudbAdapter implements ImmudbApiAdapter {
 *
 *   private final ImmuClient client;
 *
 *   public RealImmudbAdapter(ImmuClient client) {
 *     this.client = client;
 *   }
 *
 *   {@literal @}Override
 *   public void openSession(String database, String username, String password) throws Exception {
 *     client.openSession(database, username, password);
 *   }
 *
 *   {@literal @}Override
 *   public void set(String key, byte[] value) throws Exception {
 *     client.set(key, value);
 *   }
 *
 *   {@literal @}Override
 *   public void closeSession() throws Exception {
 *     client.closeSession();
 *   }
 * }
 * </pre>
 *
 * <p>AIDEV-TODO (TB-028-b): Confirm immudb4j availability on the build classpath:
 *
 * <pre>
 * &lt;!-- pom.xml addition when immudb4j is available: --&gt;
 * &lt;dependency&gt;
 *   &lt;groupId&gt;io.codenotary&lt;/groupId&gt;
 *   &lt;artifactId&gt;immudb4j&lt;/artifactId&gt;
 *   &lt;version&gt;0.9.4&lt;/version&gt;  &lt;!-- verify latest stable compatible with Java 21 --&gt;
 * &lt;/dependency&gt;
 * &lt;!-- Custom repository if not on Maven Central: --&gt;
 * &lt;repositories&gt;
 *   &lt;repository&gt;
 *     &lt;id&gt;codenotary-immudb4j&lt;/id&gt;
 *     &lt;url&gt;https://packages.codenotary.io/repository/maven-public&lt;/url&gt;
 *   &lt;/repository&gt;
 * &lt;/repositories&gt;
 * </pre>
 */
// SPIKE: This class is intentionally a documentation stub — it does not compile without immudb4j.
// See SpikeStubImmudbAdapter for the compilable placeholder used during TB-028.
public class RealImmudbAdapter implements ImmudbApiAdapter {

  // AIDEV-TODO (TB-029): Uncomment and implement when immudb4j is on the classpath.
  // private final /* io.codenotary.immudb4j. */ ImmuClient client;

  @Override
  public void openSession(String database, String username, String password) throws Exception {
    // TODO (TB-029): client.openSession(database, username, password);
    throw new UnsupportedOperationException(
        "Replace SpikeStubImmudbAdapter with RealImmudbAdapter (TB-029)");
  }

  @Override
  public void set(String key, byte[] value) throws Exception {
    // TODO (TB-029): client.set(key, value);
    throw new UnsupportedOperationException(
        "Replace SpikeStubImmudbAdapter with RealImmudbAdapter (TB-029)");
  }

  @Override
  public void closeSession() throws Exception {
    // TODO (TB-029): client.closeSession();
    throw new UnsupportedOperationException(
        "Replace SpikeStubImmudbAdapter with RealImmudbAdapter (TB-029)");
  }
}
