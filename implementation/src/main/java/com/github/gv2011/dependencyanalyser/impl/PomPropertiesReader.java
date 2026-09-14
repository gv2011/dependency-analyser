package com.github.gv2011.dependencyanalyser.impl;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

import com.github.gv2011.dependencyanalyser.api.Version;
import com.github.gv2011.util.ann.Nullable;
import com.github.gv2011.util.icol.Opt;

/**
 * Reads a Maven artifact's own pom.properties from the classpath
 * (META-INF/maven/&lt;groupId&gt;/&lt;artifactId&gt;/pom.properties).
 *
 * <p><b>Availability:</b>
 * <ul>
 * <li>Under Maven, reliably present only in integration tests
 *     (maven-failsafe-plugin, integration-test/verify phases, which run
 *     after {@code package} - not maven-surefire-plugin's unit tests,
 *     which run before it). Absent otherwise: {@link Opt#empty()}, not an
 *     error.
 * <li>In Eclipse: Maven &gt; Update Project usually creates it.
 * </ul>
 */
public final class PomPropertiesReader {

  public static final String VERSION_PROPERTY = "version";

  private final String groupId;
  private final String artifactId;

  public PomPropertiesReader(final String groupId, final String artifactId) {
    this.groupId = groupId;
    this.artifactId = artifactId;
  }

  public Opt<Properties> readPomProperties() {
    final String resourcePath = "/META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
    try(@Nullable InputStream in = getClass().getResourceAsStream(resourcePath)) {
      return in==null ? Opt.empty() : Opt.of(loadProperties(in));
    }
    catch(final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public Opt<Version> readVersion() {
    return readPomProperties()
      .map(p -> requireNonNull(p.getProperty(VERSION_PROPERTY)))
      .map(VersionImpl::parse)
    ;
  }

  private static Properties loadProperties(final InputStream in) throws IOException {
    final Properties properties = new Properties();
    properties.load(in);
    return properties;
  }

}
