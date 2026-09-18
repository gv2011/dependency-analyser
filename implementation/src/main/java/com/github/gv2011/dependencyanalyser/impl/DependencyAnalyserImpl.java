package com.github.gv2011.dependencyanalyser.impl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.github.gv2011.dependencyanalyser.api.DependencyAnalyser;
import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;
import com.github.gv2011.dependencyanalyser.api.Project;
import com.github.gv2011.dependencyanalyser.api.Repository;
import com.github.gv2011.dependencyanalyser.api.RepositoryId;
import com.github.gv2011.dependencyanalyser.api.Version;
import com.github.gv2011.util.icol.IList;
import com.github.gv2011.util.tstr.TypedString;

public class DependencyAnalyserImpl implements DependencyAnalyser {

  @Override
  public Project getProject(final Path projectDirectory, final IList<Repository> additionalRepositories) {
    final String pomContent;
    try {
      pomContent = Files.readString(projectDirectory.resolve("pom.xml"), StandardCharsets.UTF_8);
    }
    catch(final IOException e) {
      throw new UncheckedIOException(e);
    }
    return new LazyProject(pomContent, additionalRepositories);
  }

  @Override
  public Project getProject(final MavenCoordinates projectCoordinates, final IList<Repository> additionalRepositories) {
    // The fetch of this project's own pom needs the given repositories
    // too, not just the model build that happens afterward - otherwise
    // an artifact that lives only in one of them could never be found
    // in the first place.
    final String pomContent = PomFetcher.fetchPomContent(
      projectCoordinates, Conversions.toMavenRepositories(additionalRepositories)
    );
    return new LazyProject(pomContent, additionalRepositories);
  }

  @Override
  public Version parseVersion(final String version) {
    return VersionImpl.parse(version);
  }

  @Override
  public RepositoryId parseRepositoryId(final String repositoryId) {
    return TypedString.create(RepositoryId.class, repositoryId);
  }

}
