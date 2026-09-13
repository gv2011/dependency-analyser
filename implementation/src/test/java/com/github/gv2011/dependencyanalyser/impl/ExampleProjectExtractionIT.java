package com.github.gv2011.dependencyanalyser.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
 * dependency-analyser-example:sources, which forces that build order -
 * into a fresh temp directory, then runs resolvedDependencies(...) against
 * that extracted copy, the same as it would run against any real project
 * on disk.
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
    final String version = System.getProperty("dependencyAnalyserExampleVersion");
    if(version==null) {
      fail(
        "System property dependencyAnalyserExampleVersion not set - "
        + "see implementation/pom.xml's failsafe configuration."
      );
    }

    final Path tempDir = Files.createTempDirectory("dependency-analyser-example-");
    System.out.println("Extracted dependency-analyser-example sources to: " + tempDir);

    final String workingDirectory = System.getProperty("user.dir");
    System.setProperty(MavenApi.MULTIMODULE_PROJECT_DIRECTORY, workingDirectory);
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
      workingDirectory
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

}
