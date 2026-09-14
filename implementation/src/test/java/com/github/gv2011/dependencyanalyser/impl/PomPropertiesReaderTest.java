package com.github.gv2011.dependencyanalyser.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.github.gv2011.dependencyanalyser.api.Version;
import com.github.gv2011.util.icol.Opt;

/**
 * Uses a controlled fixture resource (src/test/resources/META-INF/maven/
 * com.github.gv2011.dependencyanalyser.test/pom-properties-reader-fixture/
 * pom.properties) rather than any real module's own pom.properties, so
 * every case here is deterministic regardless of build phase.
 */
class PomPropertiesReaderTest {

  private static final String GROUP_ID = "com.github.gv2011.dependencyanalyser.test";
  private static final String ARTIFACT_ID = "pom-properties-reader-fixture";

  @Test
  void readPomPropertiesTest() {
    final Opt<Properties> properties = new PomPropertiesReader(GROUP_ID, ARTIFACT_ID).readPomProperties();
    assertThat(properties.isPresent(), is(true));
    assertThat(properties.get().getProperty(PomPropertiesReader.VERSION_PROPERTY), is("1.2.3-test"));
  }

  @Test
  void readPomPropertiesNotFoundTest() {
    final Opt<Properties> properties = new PomPropertiesReader(GROUP_ID, "no-such-artifact").readPomProperties();
    assertThat(properties.isPresent(), is(false));
  }

  @Test
  void readVersionTest() {
    final Opt<Version> version = new PomPropertiesReader(GROUP_ID, ARTIFACT_ID).readVersion();
    assertThat(version.isPresent(), is(true));
    assertThat(version.get().toString(), is("1.2.3-test"));
  }

  @Test
  void readVersionNotFoundTest() {
    final Opt<Version> version = new PomPropertiesReader(GROUP_ID, "no-such-artifact").readVersion();
    assertThat(version.isPresent(), is(false));
  }

}
