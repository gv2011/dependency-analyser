package com.github.gv2011.dependencyanalyser.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.github.gv2011.dependencyanalyser.api.Classpath;
import com.github.gv2011.dependencyanalyser.api.ResolvedDependency;
import com.github.gv2011.dependencyanalyser.api.Version;
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
    // example shares this reactor's version with implementation (no module
    // overrides its parent's version), so ReactorVersion.get() - which
    // reads implementation's own pom.properties - is also example's
    // correct version; no need for a separate read of example's own.
    // Absence here (unlike ReactorVersion's own general contract) IS an
    // error: an integration test runs under `mvn verify`, after
    // packaging, so this should always be present by the time this line
    // runs - .orElseThrow(...) not independently confirmed as Opt's exact
    // method name (couldn't verify Opt's API directly), but Opt mirrors
    // java.util.Optional's naming throughout everywhere else it's used in
    // this codebase.
    final Version version = ReactorVersion.get().orElseThrow(() -> new IllegalStateException(
      "dependency-analyser-implementation's pom.properties not found on the classpath - required for "
      + "this integration test (it should always be present here, since integration tests run after "
      + "packaging). In Eclipse, this usually means Maven > Update Project (or a real 'mvn install') "
      + "hasn't been run since the module was last built."
    ));

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
      Path.of(System.getProperty("user.dir"))
    );
    assertThat(
      "dependency:unpack failed: " + unpackResult.exceptions(),
      unpackResult.exceptions().isEmpty(),
      is(true)
    );

    // Fail here, clearly, if extraction didn't actually produce a real
    // project - rather than several layers down inside embedded Maven's
    // own MissingProjectException, which this exact case has hit before:
    // a stale sources jar in the local repo, from before includePom was
    // added, still missing the POM. `mvn clean` alone, or Eclipse's
    // "Maven > Update Project" alone, does not fix this - neither
    // actually re-installs anything to the local repo; only a real
    // `mvn install` (of at least the example module) does.
    final Path pomFile = tempDir.resolve("pom.xml");
    assertThat(
      "pom.xml missing from extracted sources jar at " + pomFile
      + " - most likely fix: run 'mvn install' (not just 'mvn clean', and not just Eclipse's "
      + "'Maven > Update Project', neither of which re-installs anything to the local repo) "
      + "for at least the example module, then re-run this test. If that doesn't help: is "
      + "example/pom.xml's maven-source-plugin execution still configured with includePom=true?",
      Files.exists(pomFile),
      is(true)
    );
    final Path exampleSourceFile =
      tempDir.resolve("com/github/gv2011/dependencyanalyser/example/Example.java")
    ;
    assertThat(
      "Example.java missing from extracted sources jar at " + exampleSourceFile
      + " - the sources jar should contain .java files, not just the POM",
      Files.exists(exampleSourceFile),
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
