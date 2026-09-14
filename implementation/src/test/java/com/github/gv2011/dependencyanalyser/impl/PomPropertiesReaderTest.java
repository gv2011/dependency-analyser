package com.github.gv2011.dependencyanalyser.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

/**
 * Uses a controlled fixture resource (src/test/resources/META-INF/maven/
 * com.github.gv2011.dependencyanalyser.test/pom-properties-reader-fixture/
 * pom.properties) rather than this module's own real pom.properties, so
 * both cases below are deterministic regardless of build phase - unlike
 * ReactorVersionTest, which genuinely can't be (see its own javadoc).
 */
class PomPropertiesReaderTest {

  @Test
  void readsKnownFixture() {
    final var properties = new PomPropertiesReader(
      "com.github.gv2011.dependencyanalyser.test", "pom-properties-reader-fixture"
    ).readPomProperties();
    assertThat(properties.isPresent(), is(true));
    assertThat(
      properties.get().getProperty(PomPropertiesReader.VERSION_PROPERTY),
      is("1.2.3-test")
    );
  }

  @Test
  void absentForUnknownCoordinates() {
    final var properties = new PomPropertiesReader(
      "com.github.gv2011.dependencyanalyser.test", "no-such-artifact"
    ).readPomProperties();
    assertThat(properties.isPresent(), is(false));
  }

}
