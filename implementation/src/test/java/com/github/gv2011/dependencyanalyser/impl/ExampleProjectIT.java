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
import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;
import com.github.gv2011.dependencyanalyser.api.Version;
import com.github.gv2011.util.icol.ISet;

/**
 * Exercises getPom and resolvedDependencies together, against a genuine,
 * independently built project: fetches dependency-analyser-example's own
 * pom.xml back by coordinates (the real dependency:copy path via
 * DependencyAnalyser.getPom - not a directory already on disk, unlike
 * DependencyAnalyserImplIT's test against the api module), writes it into
 * a fresh directory under target/, then runs resolvedDependencies(...)
 * against that directory the same as it would run against any real
 * project.
 *
 * <p>dependency-analyser-example's own pom.xml is the place to add
 * further, more interesting or corner-case dependencies (exclusions, a
 * BOM import, etc.) as they become worth testing - this class's job is
 * exercising the getPom + resolvedDependencies combination against
 * whatever that pom currently declares, not asserting one fixed
 * dependency set forever.
 *
 * <p><b>Requires dependency-analyser-example already installed to the
 * local repository:</b> getPom resolves purely from the local/remote
 * repository, never from this reactor's own in-memory build state.
 * implementation/pom.xml's test-scope dependency on the example module
 * forces reactor build order (example built before implementation), but
 * that alone does not install it - only a build whose requested goal
 * actually reaches {@code install} does (the documented way to build
 * this project is {@code mvn clean install}). A bare {@code mvn verify},
 * on a module never installed before, will not satisfy this. As with
 * PomFetcherTest: ordinary connected Maven use is enough - install once
 * while online, and every later run, including offline, succeeds the
 * same way any other Maven goal does after its first connected use.
 *
 * <p>Runs under {@code mvn verify} (failsafe), not {@code mvn test} - the
 * local-repository-install requirement above is a genuine, unavoidable
 * reactor-ordering dependency, not just a naming convention.
 */
class ExampleProjectIT {

  private static final String GROUP_ID = "com.github.gv2011";
  private static final String IMPLEMENTATION_ARTIFACT_ID = "dependency-analyser-implementation";
  private static final String EXAMPLE_ARTIFACT_ID = "dependency-analyser-example";

  @Test
  void resolvedDependenciesOfFetchedExamplePom() throws IOException {
    final MavenCoordinates exampleCoordinates = Conversions.toMavenCoordinates(
      GROUP_ID, EXAMPLE_ARTIFACT_ID, reactorVersion().toString(), "jar"
    );

    final String pomContent = new DependencyAnalyserImpl().getPom(exampleCoordinates);

    final Path projectDirectory = createTimestampedDirectory();
    Files.writeString(projectDirectory.resolve("pom.xml"), pomContent, StandardCharsets.UTF_8);

    final ISet<Dependency> deps =
      new DependencyAnalyserImpl().resolvedDependencies(projectDirectory, Classpath.MAIN)
    ;
    final boolean containsSlf4jApi = deps.stream().anyMatch(d ->
      d.coordinates().identity().groupId().equals("org.slf4j")
      && d.coordinates().identity().artifactId().equals("slf4j-api")
    );
    assertThat(
      "expected org.slf4j:slf4j-api among " + EXAMPLE_ARTIFACT_ID + "'s resolved dependencies, found: " + deps,
      containsSlf4jApi,
      is(true)
    );
  }

  /**
   * example shares this reactor's version with implementation - neither
   * overrides its parent's version - so implementation's own
   * pom.properties (read via PomPropertiesReader) gives example's
   * correct version too. Presence is guaranteed here: this class only
   * ever runs as an IT, i.e. after packaging.
   */
  private static Version reactorVersion() {
    return new PomPropertiesReader(GROUP_ID, IMPLEMENTATION_ARTIFACT_ID).readVersion().get();
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
