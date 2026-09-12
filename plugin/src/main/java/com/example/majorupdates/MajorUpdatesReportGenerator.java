package com.example.majorupdates;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.versions.api.AbstractVersionDetails;

import com.example.majorupdates.Dependency.DependencyKind;
import com.example.majorupdates.util.GroupAndArtifact;
import com.google.common.collect.ImmutableList;

/**
 * Generates a report -- CSV or JSON, per {@link OutputFormat} -- of available major version
 * updates, either to a file ({@link #generate}) or directly to {@link System#out}
 * ({@link #generateToConsole}, for callers -- see {@code major-updates-invoker} -- that must not
 * write anything to the analyzed project's own filesystem at all).
 */
final class MajorUpdatesReportGenerator {

  private final VersionsHelperFacade versionsHelper;
  private final CsvFormatter csvFormatter = new CsvFormatter();
  private final JsonFormatter jsonFormatter = new JsonFormatter();

  MajorUpdatesReportGenerator(final VersionsHelperFacade versionsHelper) {
      this.versionsHelper = versionsHelper;
  }

  void generate(
    final ImmutableList<MavenProject> reactorProjects, final Path outputFile,
    final OutputFormat outputFormat
  ) throws MojoExecutionException {
    try (final Writer writer = Files.newBufferedWriter(outputFile)) {
      writeReport(writer, updatesFor(reactorProjects), outputFormat);
    } catch (final Exception e) {
      throw new MojoExecutionException("Failed writing report", e);
    }
  }

  /**
   * Writes the report directly to {@link System#out}. Deliberately never closes the underlying
   * stream -- unlike {@link #generate}'s file, closing {@code System.out} mid-build would break
   * Maven's own console output for the rest of the build, since this runs mid-execution, not at
   * JVM shutdown. Only flushed.
   */
  void generateToConsole(
    final ImmutableList<MavenProject> reactorProjects, final OutputFormat outputFormat
  ) {
    final Writer writer = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);
    writeReport(writer, updatesFor(reactorProjects), outputFormat);
    flush(writer);
  }

  private ImmutableList<UpdateRow> updatesFor(final ImmutableList<MavenProject> reactorProjects) {
    return getUpdates(reactorProjects).collect(ImmutableList.toImmutableList());
  }

  private void writeReport(
    final Writer writer, final ImmutableList<UpdateRow> updates, final OutputFormat outputFormat
  ) {
    if (outputFormat == OutputFormat.CSV) {
      writeLine(writer, csvFormatter.header());
      updates.forEach(update->writeLine(writer, csvFormatter.format(update)));
    } else {
      writeLine(writer, jsonFormatter.format(GroupAndArtifact.VERSION, updates));
    }
  }

  Stream<UpdateRow> getUpdates(final ImmutableList<MavenProject> reactorProjects){
    return reactorProjects.stream()
      .flatMap(project->
        Arrays.stream(DependencyKind.values())
        .flatMap(kind->kind.getDependencies(project))
        .flatMap(dependency->
          getLatestMajorUpdate(dependency)
          .map(majorVersion->{
            final String module = project.getArtifactId();
            return new UpdateRow(module, dependency, majorVersion);
          })
          .stream()
        )
      )
    ;
  }

  /**
   * @return The latest major version newer than dependency or empty.
   *
   * <p>The actual filter/comparison logic ({@code ArtifactVersions.filter(...)},
   * {@code getNewestUpdateWithinSegment(...)}) no longer lives here -- it moved into
   * {@code MajorUpdatesMojo.VersionsHelperAdapter}, reached via
   * {@link Dependency#lookupLatestMajorUpdate}. This method's only remaining job is the gate:
   * don't look anything up at all when no version is declared.
   */
  final Optional<Version> getLatestMajorUpdate(final Dependency dependency) {
    return dependency.version()
      .flatMap(_->
        dependency.lookupLatestMajorUpdate(versionsHelper, this::isEligibleForMajorUpdate)
      )
      .map(Version::of)
    ;
  }

  private boolean isEligibleForMajorUpdate(final String version) {
    return !AbstractVersionDetails.isPreReleaseVersion(version);
  }

  /**
   * Writes {@code "\n"} explicitly, not the platform line separator ({@code BufferedWriter
   * .newLine()}'s behavior) -- deliberate, not an oversight, so file output is byte-identical
   * regardless of the platform this plugin runs on, rather than silently varying (CRLF on
   * Windows, LF elsewhere).
   */
  private static void writeLine(final Writer writer, final String line) {
    try {
      writer.write(line);
      writer.write("\n");
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void flush(final Writer writer) {
    try {
      writer.flush();
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
