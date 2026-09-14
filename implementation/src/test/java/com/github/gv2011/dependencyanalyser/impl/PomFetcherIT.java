package com.github.gv2011.dependencyanalyser.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;

/**
 * Uses org.slf4j:slf4j-api as the example artifact - a real, well-known
 * dependency this reactor's own build already resolves into the local
 * repo (root pom.xml pins its version), so this doesn't strictly need
 * network access to pass, though it still shells out to embedded Maven
 * the same as PomFetcher always does.
 *
 * <p>Runs under {@code mvn verify} (failsafe), not {@code mvn test}.
 */
class PomFetcherIT {

  @Test
  void fetchesSlf4jApiPom() {
    final MavenCoordinates coordinates =
      Conversions.toMavenCoordinates("org.slf4j", "slf4j-api", "2.0.19", "jar")
    ;
    final String pomContent = new DependencyAnalyserImpl().getPom(coordinates);
    assertThat(pomContent, containsString("<artifactId>slf4j-api</artifactId>"));
  }

}
