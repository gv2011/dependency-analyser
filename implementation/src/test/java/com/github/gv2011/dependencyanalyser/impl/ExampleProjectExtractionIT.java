package com.github.gv2011.dependencyanalyser.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.github.gv2011.dependencyanalyser.api.Classpath;
import com.github.gv2011.dependencyanalyser.api.ResolvedDependency;
import com.github.gv2011.dependencyanalyser.mvnapi.MavenApi;
import com.github.gv2011.dependencyanalyser.mvnapi.MavenApiResult;
import com.github.gv2011.util.icol.ISet;

/**
 * Exercises the whole path end to end against a genuine, independently
 * built project, rather than one hand-assembled inline: extracts the
 * example module's own sources attachment - installed by the reactor
 * before this runs, see implementation/pom.xml's dependency on
 * dependency-analyser-example, which forces that build order - into a
 * fresh temp directory, then runs resolvedDependencies(...) against that
 * extracted copy, the same as it would run against any real project on
 * disk.
 *
 * <p>The temp directory is deliberately not deleted afterwards, so it can
 * be inspected - see the logged path.
 *
 * <p>Runs under `mvn verify` (failsafe), not `mvn test` - shells out to
 * embedded Maven twice (dependency:unpack, then dependency:list via
 * DependencyAnalyserImpl). Not run or verified in the environment this was
 * written in (no Maven available there).
 */
class ExampleProjectExtractionIT {

  @Test
  void extractedExampleModuleHasSlf4jApi() throws IOException {
    final String version = exampleModuleVersion();

    final Path tempDir = Files.createTempDirectory("dependency-analyser-example-");
    System.out.println("Extracted dependency-analyser-example sources to: " + tempDir);

    final MavenApiResult unpackResult = MavenApi.createApi().doMain(
      new String[]{
        "-N", // this project directory only, not a reactor recursion
        "-B", // batch mode: no interactive prompts
        "dependency:unpack",
        // groupId:artifactId:version:packaging:classifier, per
        // dependency:unpack's own documented <artifact> format.
        "-Dartifact=com.github.gv2011:dependency-analyser-example:" + version + ":jar:sources",
        "-DoutputDirectory=" + tempDir.toAbsolutePath(),
      },
      System.getProperty("user.dir")
    );
    assertThat(
      "dependency:unpack failed: " + unpackResult.exceptions(),
      unpackResult.exceptions().isEmpty(),
      is(true)
    );

    final ISet<ResolvedDependency> deps =
      new DependencyAnalyserImpl().resolvedDependencies(tempDir, Classpath.MAIN)
    ;
    final boolean containsSlf4jApi = deps.stream().anyMatch(d ->
      d.identity().groupId().equals("org.slf4j")
      && d.identity().artifactId().equals("slf4j-api")
    );
    assertThat(containsSlf4jApi, is(true));
  }

  /**
   * Reads dependency-analyser-example's own version from its
   * pom.properties, published into the regular jar's
   * META-INF/maven/&lt;groupId&gt;/&lt;artifactId&gt;/ path by every real
   * Maven build (and, after an Eclipse "Maven &gt; Update Project", by
   * m2e too) - the same classpath-resource technique the previous version
   * of this project used to determine its own version at runtime, chosen
   * specifically because it works the same way whether this test runs via
   * `mvn verify` or directly from an IDE, unlike a system property a build
   * plugin would have to set up front.
   */
  private static String exampleModuleVersion() throws IOException {
    final String resourcePath =
      "META-INF/maven/com.github.gv2011/dependency-analyser-example/pom.properties";
    try(InputStream in = ExampleProjectExtractionIT.class.getClassLoader().getResourceAsStream(resourcePath)) {
      if(in==null) {
        throw new IllegalStateException(
          "Resource not found on classpath: " + resourcePath + " - is dependency-analyser-example "
          + "on this module's test classpath, and has it actually been mvn-installed "
          + "(or, in Eclipse, has Maven > Update Project been run since)?"
        );
      }
      final Properties properties = new Properties();
      properties.load(in);
      return Objects.requireNonNull(
        properties.getProperty("version"), "pom.properties has no 'version' property"
      );
    }
  }

}
