package com.github.gv2011.dependencyanalyser.impl;

import static com.github.gv2011.util.BeanUtils.beanBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.apache.maven.cli.MavenApiImpl;

import com.github.gv2011.dependencyanalyser.api.Classpath;
import com.github.gv2011.dependencyanalyser.api.DependencyAnalyser;
import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;
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
    final Path outputFile;
    try {
      outputFile = Files.createTempFile("dependency-list-", ".txt");
    }
    catch(final IOException e) {
      throw new UncheckedIOException(e);
    }
    try {
      runDependencyList(projectDirectory, includeScope(classpath), outputFile);
      return parseOutputFile(outputFile);
    }
    finally {
      try {
        Files.deleteIfExists(outputFile);
      }
      catch(final IOException e) {
        throw new UncheckedIOException(e);
      }
    }
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
   * Not thread-safe / not reentrant: sets the
   * {@code maven.multiModuleProjectDirectory} system property for the
   * duration of the call. Callers must not invoke this concurrently from
   * multiple threads.
   */
  private void runDependencyList(
    final Path projectDirectory, final String includeScope, final Path outputFile
  ) {
    // MavenApi requires this system property to be set; normally the `mvn`
    // launcher script sets it, which programmatic embedding bypasses.
    System.setProperty(
      MavenApi.MULTIMODULE_PROJECT_DIRECTORY,
      projectDirectory.toAbsolutePath().toString()
    );
    final MavenApiResult result = mavenApi.doMain(
      new String[]{
        "-N", // this project directory only, not a reactor recursion
        "-B", // batch mode: no interactive prompts
        "dependency:list",
        "-DincludeScope=" + includeScope,
        "-DoutputFile=" + outputFile.toAbsolutePath(),
      },
      projectDirectory.toAbsolutePath().toString()
    );
    if(!result.exceptions().isEmpty()) {
      final RuntimeException toThrow = new RuntimeException(
        "mvn dependency:list failed with " + result.exceptions().size() + " exception(s); "
        + "project: " + result.project()
      );
      result.exceptions().forEach(toThrow::addSuppressed);
      throw toThrow;
    }
  }

  private static ISet<ResolvedDependency> parseOutputFile(final Path outputFile) {
    final List<String> lines;
    try {
      lines = Files.readAllLines(outputFile, StandardCharsets.UTF_8);
    }
    catch(final IOException e) {
      throw new UncheckedIOException(e);
    }
    final var result = ICollections.<ResolvedDependency>setBuilder();
    for(final String rawLine: lines) {
      parseLine(rawLine).ifPresent(result::add);
    }
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
   * <p>Tolerant of anything that isn't a 5- or 6-field coordinate line
   * (blank lines, a possible banner line), treating those as not a
   * dependency line rather than failing.
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
        .set(ResolvedDependency::coordinates).to(
          beanBuilder(MavenCoordinates.class)
          .set(MavenCoordinates::identity).to(
            Conversions.toArtifactIdentity(groupId, artifactId, classifier, type)
          )
          .set(MavenCoordinates::version).to(VersionImpl.parse(version))
          .build()
        )
        .set(ResolvedDependency::scope).to(scope)
        .build()
    );
  }

  @Override
  public Version parseVersion(final String version) {
    return VersionImpl.parse(version);
  }

}
