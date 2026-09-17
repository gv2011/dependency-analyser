package com.github.gv2011.dependencyanalyser.impl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.InvalidRepositoryException;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;

/**
 * Lets Maven's own model-building process (parent and BOM-import
 * resolution, inside {@link org.apache.maven.model.building.ModelBuilder})
 * fetch whatever pom it needs via {@link PomFetcher} - already a real,
 * working fetch mechanism - rather than wiring a full RepositorySystem-
 * based resolver from scratch.
 *
 * <p>addRepository(...) is a deliberate no-op: PomFetcher resolves via a
 * real embedded Maven invocation that reads settings.xml/repositories
 * itself, so this resolver has no repository list of its own to add to.
 */
final class BridgingModelResolver implements ModelResolver {

  @Override
  public ModelSource resolveModel(final String groupId, final String artifactId, final String version)
    throws UnresolvableModelException
  {
    return fetch(groupId, artifactId, version);
  }

  @Override
  public ModelSource resolveModel(final Parent parent) throws UnresolvableModelException {
    return fetch(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
  }

  @Override
  public ModelSource resolveModel(final Dependency dependency) throws UnresolvableModelException {
    return fetch(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
  }

  private ModelSource fetch(final String groupId, final String artifactId, final String version)
    throws UnresolvableModelException
  {
    final String pomContent;
    try {
      pomContent = PomFetcher.fetchPomContent(
        Conversions.toMavenCoordinates(groupId, artifactId, version, "pom")
      );
    }
    catch(final RuntimeException e) {
      throw new UnresolvableModelException(
        "Could not fetch " + groupId + ":" + artifactId + ":" + version, groupId, artifactId, version, e
      );
    }
    try {
      final Path tempFile = Files.createTempFile("resolved-pom-", ".xml");
      tempFile.toFile().deleteOnExit();
      Files.writeString(tempFile, pomContent, StandardCharsets.UTF_8);
      return new FileModelSource(tempFile.toFile());
    }
    catch(final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void addRepository(final Repository repository) throws InvalidRepositoryException {
    // no-op - see class javadoc
  }

  @Override
  public void addRepository(final Repository repository, final boolean replace)
    throws InvalidRepositoryException
  {
    // no-op - see class javadoc
  }

  @Override
  public ModelResolver newCopy() {
    return new BridgingModelResolver();
  }

}
