package com.github.gv2011.dependencyanalyser.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.github.gv2011.dependencyanalyser.api.Version;
import com.github.gv2011.util.icol.Opt;

/**
 * Reads dependency-analyser-implementation's own, real pom.properties -
 * unlike PomPropertiesReaderTest's hand-written fake, this depends on
 * build phase (see PomPropertiesReader's own javadoc): absent before
 * packaging, present by the time an integration test runs, which is
 * exactly why this is an IT and not a plain unit test.
 *
 * <p>Runs under {@code mvn verify} (failsafe), not {@code mvn test}.
 */
class PomPropertiesReaderIT {

  private static final String GROUP_ID = "com.github.gv2011";
  private static final String ARTIFACT_ID = "dependency-analyser-implementation";

  @Test
  void readPomPropertiesTest() {
    final Opt<Properties> properties = new PomPropertiesReader(GROUP_ID, ARTIFACT_ID).readPomProperties();
    assertThat(properties.isPresent(), is(true));
    assertThat(properties.get().getProperty(PomPropertiesReader.VERSION_PROPERTY), is("0.1.0-SNAPSHOT"));
  }

  @Test
  void readVersionTest() {
    final Opt<Version> version = new PomPropertiesReader(GROUP_ID, ARTIFACT_ID).readVersion();
    assertThat(version.isPresent(), is(true));
    assertThat(version.get().toString(), is("0.1.0-SNAPSHOT"));
  }

}
