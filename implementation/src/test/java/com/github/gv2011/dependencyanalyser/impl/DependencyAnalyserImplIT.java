package com.github.gv2011.dependencyanalyser.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;

import com.github.gv2011.dependencyanalyser.api.Classpath;
import com.github.gv2011.dependencyanalyser.api.Dependency;
import com.github.gv2011.util.icol.ISet;

/**
 * Exercises getPom and getDependencies together, against a genuine,
 * independently built project: fetches dependency-analyser-example's own
 * pom.xml back by coordinates (getPom - resolved purely from the local
 * repository, no dependence on this reactor's own source-directory
 * layout), writes it into a fresh directory under target/, then runs
 * getDependencies(...) against that directory the same as it would
 * run against any real project.
 *
 * <p>dependency-analyser-example exists specifically to be this fixture:
 * its own pom.xml is the place to add further, more interesting or
 * corner-case dependencies (exclusions, a BOM import, etc.) as they
 * become worth testing - unlike a production module's pom (e.g. api's),
 * whose own dependency set can change for reasons unrelated to what this
 * test is meant to check. This class's job is exercising the getPom +
 * getDependencies combination against whatever example's pom
 * currently declares, not asserting one fixed dependency set forever.
 *
 * <p><b>Requires dependency-analyser-example already installed to the
 * local repository:</b> getPom resolves purely from the local/remote
 * repository, never from this reactor's own in-memory build state.
 * This module's test-scope dependency on the example module forces
 * reactor build order (example built before implementation), but that
 * alone does not install it - only a build whose requested goal actually
 * reaches {@code install} does (the documented way to build this project
 * is {@code mvn clean install}). A bare {@code mvn verify}, on a module
 * never installed before, will not satisfy this. As with PomFetcherTest:
 * ordinary connected Maven use is enough - install once while online,
 * and every later run, including offline, succeeds the same way any
 * other Maven goal does after its first connected use.
 *
 * <p>Runs under {@code mvn verify} (failsafe), not {@code mvn test} - the
 * local-repository-install requirement above is a genuine, unavoidable
 * reactor-ordering dependency, not just a naming convention.
 */
class DependencyAnalyserImplIT {

  @Test
  void getDependenciesOfFetchedExamplePom() throws IOException {
    final String pomContent = new DependencyAnalyserImpl().getPom(ExampleModule.coordinates());

    final Path projectDirectory = createTimestampedDirectory();
    Files.writeString(projectDirectory.resolve("pom.xml"), pomContent, StandardCharsets.UTF_8);

    final ISet<Dependency> deps =
      new DependencyAnalyserImpl().getDependencies(projectDirectory, Classpath.MAIN)
    ;
    final boolean containsSlf4jApi = deps.stream().anyMatch(d ->
      d.coordinates().identity().groupId().equals("org.slf4j")
      && d.coordinates().identity().artifactId().equals("slf4j-api")
    );
    assertThat(
      "expected org.slf4j:slf4j-api among example's resolved dependencies, found: " + deps,
      containsSlf4jApi,
      is(true)
    );
  }

  /**
   * A fresh directory per run, under target/ (cleaned by mvn clean, not
   * committed) - named with the current instant in ISO 8601 basic format
   * (no ':'/'-' separators, since ':' is not a valid filename character
   * on Windows), so multiple runs' output can be told apart and
   * inspected afterwards instead of overwriting each other.
   */
  private static Path createTimestampedDirectory() {
    final DateTimeFormatter format =
      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssSSS").withZone(ZoneOffset.UTC)
    ;
    final Path dir = Paths.get("target", "example-project-" + format.format(Instant.now()));
    try {
      Files.createDirectories(dir);
    }
    catch(final IOException e) {
      throw new UncheckedIOException(e);
    }
    return dir;
  }

}
