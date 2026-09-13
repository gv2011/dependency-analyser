package org.apache.maven.cli;

/**
 * Sets a system property for the duration of a try-with-resources block,
 * restoring whatever was there before on close (or clearing the property
 * entirely, if it was previously unset).
 *
 * <p>Not a general-purpose fix for using system properties - this exists
 * specifically because MavenCliBase's own internals require
 * maven.multiModuleProjectDirectory to be set as a system property (Maven's
 * own design, not something this project chose), so it has to be set
 * somewhere; this at least makes the mutation scoped and reversible instead
 * of a permanent, uncoordinated side effect.
 */
final class TemporarySystemProperty implements AutoCloseable {

  private final String key;
  private final String previousValue;

  TemporarySystemProperty(final String key, final String value) {
    this.key = key;
    previousValue = System.getProperty(key);
    System.setProperty(key, value);
  }

  @Override
  public void close() {
    if(previousValue==null) {
      System.clearProperty(key);
    } else {
      System.setProperty(key, previousValue);
    }
  }

}
