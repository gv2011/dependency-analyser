package com.github.gv2011.dependencyanalyser.impl;

import static com.github.gv2011.util.BeanUtils.beanBuilder;

import java.nio.file.Path;
import java.util.Optional;

import org.apache.maven.cli.MavenApiImpl;

import com.github.gv2011.dependencyanalyser.api.ArtifactIdentity;
import com.github.gv2011.dependencyanalyser.api.Classpath;
import com.github.gv2011.dependencyanalyser.api.DependencyAnalyser;
import com.github.gv2011.dependencyanalyser.api.MavenScope;
import com.github.gv2011.dependencyanalyser.api.ResolvedDependency;
import com.github.gv2011.dependencyanalyser.api.Version;
import com.github.gv2011.dependencyanalyser.mvnapi.MavenApi;
import com.github.gv2011.dependencyanalyser.mvnapi.MavenApiResult;
import com.github.gv2011.util.icol.ICollections;
import com.github.gv2011.util.icol.ISet;
import com.github.gv2011.util.icol.Opt;

/**
 * Embeds Maven via {@link MavenApiImpl} and drives the real, unmodified
 * {@code dependency:list} goal, rather than re-implementing dependency
 * resolution/scope-inclusion logic directly against lower-level Maven APIs.
 * Chosen over the alternative (resolving via {@code ProjectBuilder} plus the
 * repository system directly, skipping goal execution) because
 * {@code dependency:list}'s {@code includeScope} handling is a real, tested,
 * documented mechanism - reusing it avoids re-deriving which scopes belong
 * on which classpath ourselves. This trade-off can be revisited if driving a
 * full goal execution per call turns out to be too slow or too fragile in
 * practice.
 */
public class DependencyAnalyserImpl implements DependencyAnalyser{

  private final MavenApi mavenApi = MavenApi.createApi();

  @Override
  public ISet<ResolvedDependency> resolvedDependencies(final Path projectDirectory, final Classpath classpath) {
    return parseOutput(runDependencyList(projectDirectory, includeScope(classpath)));
  }

  /**
   * Maps our {@link Classpath} to dependency:list's own {@code includeScope}
   * values. Per its reference documentation: "runtime" includes compile and
   * runtime scope; "test" includes compile, runtime and test scope (and
   * handles provided/system correctly) - this is Maven's own tested
   * scope-inclusion logic, not re-derived here.
   */
  private static String includeScope(final Classpath classpath) {
    return switch(classpath) {
      case MAIN -> "runtime";
      case TEST -> "test";
    };
  }

  /**
   * No {@code -DoutputFile} - dependency:list writes its result to
   * System.out by default, which MavenApi captures and returns via
   * MavenApiResult.output() (see CapturedSystemOut); nothing is written to
   * disk.
   */
  private String runDependencyList(final Path projectDirectory, final String includeScope) {
    final MavenApiResult result = mavenApi.doMain(
      new String[]{
        "-N", // this project directory only, not a reactor recursion
        "-B", // batch mode: no interactive prompts
        "dependency:list",
        "-DincludeScope=" + includeScope,
      },
      projectDirectory
    );
    if(!result.exceptions().isEmpty()) {
      final RuntimeException toThrow = new RuntimeException(
        "mvn dependency:list failed with " + result.exceptions().size() + " exception(s); "
        + "project: " + result.project()
      );
      result.exceptions().forEach(toThrow::addSuppressed);
      throw toThrow;
    }
    return result.output();
  }

  private static ISet<ResolvedDependency> parseOutput(final String output) {
    final var result = ICollections.<ResolvedDependency>setBuilder();
    output.lines().forEach(rawLine -> parseLine(rawLine).ifPresent(result::add));
    return result.build();
  }

  /**
   * One line of {@code mvn dependency:list} output is
   * {@code groupId:artifactId:type:version:scope}, or, when a classifier is
   * present, {@code groupId:artifactId:type:classifier:version:scope} -
   * matching Maven's own {@code Artifact} coordinate-string format. An
   * optional {@code  -- module <name>} suffix (JPMS automatic-module-name
   * info, seen when resolving on a JDK that reports it) is stripped first if
   * present.
   *
   * <p>Not verified against real output (no Maven available in the
   * environment this was written in) - tolerant of anything that isn't a
   * 5- or 6-field coordinate line (blank lines, a possible banner line),
   * treating those as not a dependency line rather than failing.
   */
  private static Optional<ResolvedDependency> parseLine(final String rawLine) {
    final String line = rawLine.split(" -- ", 2)[0].strip();
    final String[] parts = line.split(":");
    if(parts.length!=5 && parts.length!=6) {
      return Optional.empty();
    }
    final String groupId = parts[0];
    final String artifactId = parts[1];
    final String type = parts[2];
    final Opt<String> classifier = parts.length==6 ? Opt.of(parts[3]) : Opt.empty();
    final String version = parts[parts.length-2];
    final MavenScope scope = MavenScope.valueOf(parts[parts.length-1].toUpperCase());
    return Optional.of(
      beanBuilder(ResolvedDependency.class)
        .set(ResolvedDependency::identity).to(
          beanBuilder(ArtifactIdentity.class)
          .set(ArtifactIdentity::groupId).to(groupId)
          .set(ArtifactIdentity::artifactId).to(artifactId)
          .set(ArtifactIdentity::classifier).to(classifier)
          .set(ArtifactIdentity::type).to(type)
          .build()
        )
        .set(ResolvedDependency::version).to(VersionImpl.parse(version))
        .set(ResolvedDependency::scope).to(scope)
        .build()
    );
  }

  @Override
  public Version parseVersion(final String version) {
    return VersionImpl.parse(version);
  }

}
