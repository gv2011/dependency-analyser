package com.github.gv2011.dependencyanalyser.impl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.apache.maven.model.Repository;

import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;
import com.github.gv2011.dependencyanalyser.mvnapi.MavenApi;
import com.github.gv2011.dependencyanalyser.mvnapi.MavenApiResult;
import com.github.gv2011.util.icol.ICollections;
import com.github.gv2011.util.icol.IList;

/**
 * Fetches an already-installed/published artifact's own pom.xml content,
 * given only its coordinates - no project on disk needed: this generates
 * its own throwaway, minimal project directory to run dependency:copy
 * from, since every Maven goal needs some project context to execute even
 * when what it fetches is entirely unrelated to that context.
 *
 * <p>dependency:copy writes to a file - there is no way around that, it is
 * how the goal itself works - but that file is this method's own internal,
 * transient detail: read and deleted, along with the throwaway project
 * directory, before returning. The caller only ever sees the content
 * itself, as a String, never a file path.
 */
public final class PomFetcher {

  private PomFetcher(){}

  public static String fetchPomContent(final MavenCoordinates coordinates) {
    return fetchPomContent(coordinates, ICollections.emptyList());
  }

  /**
   * @param repositories consulted in addition to whatever settings.xml
   *   already configures globally - needed for an artifact that lives
   *   only in a repository declared in some real project's own pom.xml
   *   (a private/internal repository being the common case), which the
   *   no-arg overload's repository-less throwaway project can't see.
   */
  public static String fetchPomContent(
    final MavenCoordinates coordinates, final IList<Repository> repositories
  ) {
    final Path projectDir = createThrowawayProject(repositories);
    try {
      final Path outputDir = createTempDir("pom-fetch-output-");
      try {
        return copyAndRead(coordinates, projectDir, outputDir);
      }
      finally {
        deleteRecursively(outputDir);
      }
    }
    finally {
      deleteRecursively(projectDir);
    }
  }

  private static String copyAndRead(
    final MavenCoordinates coordinates, final Path projectDir, final Path outputDir
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
        "-N", // this throwaway project only, not a reactor recursion
        "-B", // batch mode: no interactive prompts
        "dependency:copy",
        // groupId:artifactId:version:packaging - the artifact's own real
        // packaging (coordinates.identity().type()) is irrelevant here;
        // :pom always means "fetch the POM file itself", regardless of
        // what the artifact is otherwise packaged as.
        "-Dartifact="
          + coordinates.identity().groupId() + ":"
          + coordinates.identity().artifactId() + ":"
          + coordinates.version() + ":pom",
        "-DoutputDirectory=" + outputDir.toAbsolutePath(),
      },
      projectDir.toAbsolutePath().toString()
    );
    if(!result.exceptions().isEmpty()) {
      final RuntimeException toThrow = new RuntimeException(
        "dependency:copy failed for " + coordinates + ": " + result.exceptions().size() + " exception(s)"
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

  private static final String MINIMAL_POM_HEADER = """
    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0">
      <modelVersion>4.0.0</modelVersion>
      <groupId>com.github.gv2011.dependencyanalyser</groupId>
      <artifactId>pom-fetcher-throwaway</artifactId>
      <version>1</version>
    """;

  private static Path createThrowawayProject(final IList<Repository> repositories) {
    final Path dir = createTempDir("pom-fetch-project-");
    try {
      Files.writeString(dir.resolve("pom.xml"), buildPom(repositories), StandardCharsets.UTF_8);
    }
    catch(final IOException e) {
      throw new UncheckedIOException(e);
    }
    return dir;
  }

  private static String buildPom(final IList<Repository> repositories) {
    final StringBuilder pom = new StringBuilder(MINIMAL_POM_HEADER);
    if(!repositories.isEmpty()) {
      pom.append("  <repositories>\n");
      for(final Repository r: repositories) {
        pom.append("    <repository>\n")
          .append("      <id>").append(xmlEscape(r.getId())).append("</id>\n")
          .append("      <url>").append(xmlEscape(r.getUrl())).append("</url>\n")
          .append("    </repository>\n");
      }
      pom.append("  </repositories>\n");
    }
    pom.append("</project>\n");
    return pom.toString();
  }

  private static String xmlEscape(final String s) {
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  private static Path createTempDir(final String prefix) {
    try {
      return Files.createTempDirectory(prefix);
    }
    catch(final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void deleteRecursively(final Path dir) {
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
