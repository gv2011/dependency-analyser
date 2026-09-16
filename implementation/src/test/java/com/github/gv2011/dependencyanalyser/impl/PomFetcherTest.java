package com.github.gv2011.dependencyanalyser.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;

/**
 * Uses org.slf4j:slf4j-api as the example artifact - a real, well-known
 * dependency this reactor's own build already resolves into the local
 * repo (root pom.xml pins its version). Runs under {@code mvn test}
 * (surefire), not {@code mvn verify}: nothing here depends on this
 * module's own package output, and the goal it drives
 * (maven-dependency-plugin's dependency:copy) needs no more environment
 * setup than any other Maven goal - resolved once on a connected run,
 * then available offline on every later run the same way.
 */
class PomFetcherTest {

  @Test
  void fetchesSlf4jApiPom() {
    final MavenCoordinates projectCoordinates =
      Conversions.toMavenCoordinates("org.slf4j", "slf4j-api", "2.0.19", "jar")
    ;
    final String pomContent = new DependencyAnalyserImpl().getPom(projectCoordinates);
    assertThat(pomContent, containsString("<artifactId>slf4j-api</artifactId>"));
  }

}
