package com.example.majorupdates.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.Properties;

public final class PomPropertiesReader {

  public static final String VERSION_PROPERTY = "version";

  private final String groupId;
  private final String artifactId;

  public PomPropertiesReader(final String groupId, final String artifactId) {
    this.groupId = groupId;
    this.artifactId = artifactId;
  }

  public Optional<Properties> readPomProperties() {
    final String resourcePath = "/META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
    final Properties properties = new Properties();
    try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
      if (in == null) {
        return Optional.empty();
      }
      properties.load(in);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
    return Optional.of(properties);
  }
}