package com.github.gv2011.dependencyanalyser.impl;

import static com.github.gv2011.dependencyanalyser.impl.VersionImpl.parse;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelSource2;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;

import com.github.gv2011.dependencyanalyser.api.Repository;
import com.github.gv2011.dependencyanalyser.api.Version;
import com.github.gv2011.util.icol.ICollections;
import com.github.gv2011.util.icol.IList;

final class BridgingModelResolver implements ModelResolver {

  private final PomFetcher pomFetcher;
  private final List<Repository> repositories;

  BridgingModelResolver(final PomFetcher pomFetcher, final IList<Repository> seed) {
    this.pomFetcher = pomFetcher;
    this.repositories = new ArrayList<>(seed);
  }

  IList<Repository> repositories() {
    return ICollections.listFrom(repositories);
  }


  @Override
  public ModelSource2 resolveModel(final String groupId, final String artifactId, final String version)
    throws UnresolvableModelException
  {
    return fetch(groupId, artifactId, parse(version));
  }

  @Override
  public ModelSource2 resolveModel(final Parent parent) throws UnresolvableModelException {
    return fetch(parent.getGroupId(), parent.getArtifactId(), parse(parent.getVersion()));
  }

  @Override
  public ModelSource2 resolveModel(final Dependency dependency) throws UnresolvableModelException {
    return fetch(dependency.getGroupId(), dependency.getArtifactId(), parse(dependency.getVersion()));
  }

  private ModelSource2 fetch(final String groupId, final String artifactId, final Version version)
    throws UnresolvableModelException
  {
    try {
      return new FileModelSource(
        pomFetcher.fetchPom(Conversions.toMavenCoordinates(groupId, artifactId, version, "pom"), repositories())
        .toFile()
      );
    }
    catch(final RuntimeException e) {
      throw new UnresolvableModelException(
        "Could not fetch " + groupId + ":" + artifactId + ":" + version,
        groupId,
        artifactId,
        version.toString(),
        e
      );
    }
  }

  @Override
  public void addRepository(final org.apache.maven.model.Repository repository){
    addRepository(repository, false);
  }

  @Override
  public void addRepository(final org.apache.maven.model.Repository repository, final boolean replace){
    addRepository(Conversions.toRepository(repository), replace);
  }

  private void addRepository(final Repository repository, final boolean replace) {
    if(replace) {
      repositories.removeIf(r -> r.id().equals(repository.id()));
      repositories.add(repository);
    }
    else if(!isPresent(repository)) repositories.add(repository);
  }

  private boolean isPresent(final Repository repository){
    return repositories.stream().anyMatch(r -> r.id().equals(repository.id()));
  }

  @Override
  public ModelResolver newCopy() {
    return new BridgingModelResolver(pomFetcher, repositories());
  }

}
