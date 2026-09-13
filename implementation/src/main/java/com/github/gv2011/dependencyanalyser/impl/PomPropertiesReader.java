package com.github.gv2011.dependencyanalyser.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

import com.github.gv2011.util.icol.Opt;

/**
 * Reads a Maven artifact's own pom.properties from the classpath
 * (META-INF/maven/&lt;groupId&gt;/&lt;artifactId&gt;/pom.properties) - the same
 * mechanism and location the previous version of this project used for
 * self-version-discovery. Only present once the artifact has actually gone
 * through the package phase - absent before that (e.g. a plain `mvn test`
 * run), which is a genuinely expected case, not an error.
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
    try(InputStream in = getClass().getResourceAsStream(resourcePath)) {
      if(in==null) {
        return Opt.empty();
      }
      final Properties properties = new Properties();
      properties.load(in);
      return Opt.of(properties);
    }
    catch(final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

}
