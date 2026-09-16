package com.github.gv2011.dependencyanalyser.impl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;
import com.github.gv2011.dependencyanalyser.mvnapi.MavenApi;
import com.github.gv2011.dependencyanalyser.mvnapi.MavenApiResult;
import com.github.gv2011.util.icol.Opt;

/**
 * Fetches an already-installed/published artifact's own pom.xml content,
 * given only its coordinates.
 *
 * <p>Repository resolution needs a real Maven project as context - one
 * whose own (possibly inherited) {@code <repositories>} actually cover
 * wherever the requested artifact lives, if that's anywhere other than
 * Central or whatever settings.xml configures globally. Two modes,
 * chosen at construction via the {@code context} constructor parameter:
 *
 * <ul>
 * <li>{@code Opt.empty()}: context-free - a synthetic, throwaway project
 * with no {@code <repositories>} of its own is used, equivalent to
 * running Maven from some arbitrary, non-project folder. Only whatever
 * settings.xml configures globally (or Central) is visible.</li>
 * <li>{@code Opt.of(path)}: the real project at {@code path} is used as
 * the resolution context directly, so its own (and inherited)
 * repositories apply. Needed for an artifact that lives in a repository
 * declared only in that project's own pom.xml (or one it inherits) - a
 * private/internal repository being the common case.</li>
 * </ul>
 */
public final class PomFetcher {

  private final Opt<Path> context;

  public PomFetcher(final Opt<Path> context) {
    this.context = context;
  }

  public String fetchPomContent(final MavenCoordinates projectCoordinates) {
    return context.isPresent()
      ? fetchUsing(projectCoordinates, context.get())
      : fetchContextFree(projectCoordinates)
    ;
  }

  private String fetchContextFree(final MavenCoordinates projectCoordinates) {
    final Path throwawayProjectDir = createThrowawayProject();
    try {
      return fetchUsing(projectCoordinates, throwawayProjectDir);
    }
    finally {
      deleteRecursively(throwawayProjectDir);
    }
  }

  private String fetchUsing(final MavenCoordinates projectCoordinates, final Path projectDir) {
    final Path outputDir = createTempDir("pom-fetch-output-");
    try {
      return copyAndRead(projectCoordinates, projectDir, outputDir);
    }
    finally {
      deleteRecursively(outputDir);
    }
  }

  private String copyAndRead(
    final MavenCoordinates projectCoordinates, final Path projectDir, final Path outputDir
  ) {
    // MavenApi requires this system property to be set; normally the `mvn`
    // launcher script sets it, which programmatic embedding bypasses. Same
    // pattern as DependencyAnalyserImpl.runDependencyList.
    System.setProperty(
      MavenApi.MULTIMODULE_PROJECT_DIRECTORY,
      projectDir.toAbsolutePath().toString()
    );
    final MavenApiResult result = MavenApi.createApi().doMain(
      new String[]{
        "-N", // just this one project, not a reactor recursion
        "-B", // batch mode: no interactive prompts
        "dependency:copy",
        // groupId:artifactId:version:packaging - the artifact's own real
        // packaging (projectCoordinates.identity().type()) is irrelevant here;
        // :pom always means "fetch the POM file itself", regardless of
        // what the artifact is otherwise packaged as.
        "-Dartifact="
          + projectCoordinates.identity().groupId() + ":"
          + projectCoordinates.identity().artifactId() + ":"
          + projectCoordinates.version() + ":pom",
        "-DoutputDirectory=" + outputDir.toAbsolutePath(),
      },
      projectDir.toAbsolutePath().toString()
    );
    if(!result.exceptions().isEmpty()) {
      final RuntimeException toThrow = new RuntimeException(
        "dependency:copy failed for " + projectCoordinates + ": " + result.exceptions().size() + " exception(s)"
      );
      result.exceptions().forEach(toThrow::addSuppressed);
      throw toThrow;
    }
    final List<Path> files;
    try(var listing = Files.list(outputDir)) {
      files = listing.toList();
    }
    catch(final IOException e) {
      throw new UncheckedIOException(e);
    }
    if(files.size()!=1) {
      throw new IllegalStateException(
        "Expected exactly one file in " + outputDir + " after dependency:copy, found "
        + files.size() + ": " + files
      );
    }
    try {
      return Files.readString(files.get(0), StandardCharsets.UTF_8);
    }
    catch(final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static final String MINIMAL_POM = """
    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0">
      <modelVersion>4.0.0</modelVersion>
      <groupId>com.github.gv2011.dependencyanalyser</groupId>
      <artifactId>pom-fetcher-throwaway</artifactId>
      <version>1</version>
    </project>
    """;

  private Path createThrowawayProject() {
    final Path dir = createTempDir("pom-fetch-project-");
    try {
      Files.writeString(dir.resolve("pom.xml"), MINIMAL_POM, StandardCharsets.UTF_8);
    }
    catch(final IOException e) {
      throw new UncheckedIOException(e);
    }
    return dir;
  }

  private Path createTempDir(final String prefix) {
    try {
      return Files.createTempDirectory(prefix);
    }
    catch(final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void deleteRecursively(final Path dir) {
    try(var walk = Files.walk(dir)) {
      walk.sorted(Comparator.reverseOrder()).forEach(p -> {
        try {
          Files.delete(p);
        }
        catch(final IOException e) {
          throw new UncheckedIOException(e);
        }
      });
    }
    catch(final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

}
