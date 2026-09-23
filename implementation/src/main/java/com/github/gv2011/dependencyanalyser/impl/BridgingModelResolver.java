package com.github.gv2011.dependencyanalyser.impl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.building.ModelSource2;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;

import com.github.gv2011.dependencyanalyser.api.Repository;
import com.github.gv2011.util.icol.ICollections;
import com.github.gv2011.util.icol.IList;

/**
 * Lets Maven's own model-building process (parent and BOM-import
 * resolution, inside {@link org.apache.maven.model.building.ModelBuilder})
 * fetch whatever pom it needs via {@link PomFetcher} - already a real,
 * working fetch mechanism - rather than wiring a full RepositorySystem-
 * based resolver from scratch.
 *
 * <p>Stateful, as {@link ModelResolver}'s own contract requires: as the
 * model builder reads each level of the parent chain, it calls
 * addRepository(...) with whatever that level's own pom declares, before
 * asking this resolver to fetch the next level - the same accumulation
 * a real Maven build does. Repositories collected so far are passed to
 * every subsequent fetch, so an artifact living only in a repository
 * declared partway up the chain (a private/internal one, typically) is
 * still visible once that declaration has been read.
 *
 * <p>The mutable internal state (repositories, a plain ArrayList) is
 * inherent to what this class is - a stateful resolver ModelResolver's
 * own contract requires being able to mutate. At its actual borders -
 * the constructor and the repositories() accessor - it deals only in
 * the project's immutable IList, per this project's own convention.
 */
final class BridgingModelResolver implements ModelResolver {

  private final List<Repository> repositories;

  /**
   * @param seed repositories already known before this resolver starts -
   *   e.g. what an earlier project in the same walk (a referring
   *   project) had accumulated. Copied, not held by reference: further
   *   addRepository(...) calls mutate this resolver's own list only.
   */
  BridgingModelResolver(final IList<Repository> seed) {
    this.repositories = new ArrayList<>(seed);
  }

  /**
   * Everything known to this resolver right now - the seed it was given
   * plus whatever addRepository(...) has added since. Read after a
   * build completes to find out what that build's own project (and
   * whatever it inherited) actually declared.
   */
  IList<Repository> repositories() {
    return ICollections.listFrom(repositories);
  }

  // ModelSource itself is deprecated in favor of ModelSource2 (its
  // javadoc says so directly), but the three overrides below can't use
  // the replacement - ModelResolver's own interface declares ModelSource
  // as their return type, not something this class controls. fetch(...)
  // returns the non-deprecated ModelSource2 instead; ModelSource2 extends
  // ModelSource, so it still satisfies the overrides.

  @SuppressWarnings("deprecation") // see comment
  @Override
  public ModelSource resolveModel(final String groupId, final String artifactId, final String version)
    throws UnresolvableModelException
  {
    return fetch(groupId, artifactId, version);
  }

  @SuppressWarnings("deprecation") // see comment
  @Override
  public ModelSource resolveModel(final Parent parent) throws UnresolvableModelException {
    return fetch(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
  }

  @SuppressWarnings("deprecation") // see comment
  @Override
  public ModelSource resolveModel(final Dependency dependency) throws UnresolvableModelException {
    return fetch(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
  }

  private ModelSource2 fetch(final String groupId, final String artifactId, final String version)
    throws UnresolvableModelException
  {
    final String pomContent;
    try {
      pomContent = new PomFetcher().fetchPomContent(
        Conversions.toMavenCoordinates(groupId, artifactId, version, "pom"), repositories()
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

  @Override//import org.apache.maven.model.Repository;
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
    // A genuine copy, not a shared reference: newCopy() exists so a
    // caller can branch the resolver's state (e.g. per BOM import) -
    // further addRepository(...) calls on the copy must not leak back
    // into this instance's own list. The constructor already copies its
    // seed, so passing repositories() directly here is enough.
    return new BridgingModelResolver(repositories());
  }

}
