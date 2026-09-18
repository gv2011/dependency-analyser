package com.github.gv2011.dependencyanalyser.impl;

import static com.github.gv2011.util.BeanUtils.beanBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.project.MavenProject;

import com.github.gv2011.dependencyanalyser.api.ArtifactIdentity;
import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;
import com.github.gv2011.dependencyanalyser.api.Repository;
import com.github.gv2011.dependencyanalyser.api.RepositoryId;
import com.github.gv2011.util.icol.ICollections;
import com.github.gv2011.util.icol.IList;
import com.github.gv2011.util.icol.Opt;
import com.github.gv2011.util.tstr.TypedString;

public final class Conversions {

  private Conversions(){};

  public static MavenCoordinates toMavenCoordinates(final MavenProject p){
    return beanBuilder(MavenCoordinates.class)
      .set(MavenCoordinates::identity).to(
        toArtifactIdentity(p.getGroupId(), p.getArtifactId(), Opt.empty(), p.getPackaging())
      )
      .set(MavenCoordinates::version).to(VersionImpl.parse(p.getVersion()))
      .build()
    ;
  }

  /**
   * For coordinates known directly, rather than read from a MavenProject
   * (e.g. an artifact this reactor doesn't build itself). type is the
   * artifact's own real packaging (e.g. "jar"), not the type of anything
   * that might later be fetched about it.
   */
  public static MavenCoordinates toMavenCoordinates(
    final String groupId, final String artifactId, final String version, final String type
  ){
    return beanBuilder(MavenCoordinates.class)
      .set(MavenCoordinates::identity).to(
        toArtifactIdentity(groupId, artifactId, Opt.empty(), type)
      )
      .set(MavenCoordinates::version).to(VersionImpl.parse(version))
      .build()
    ;
  }

  public static ArtifactIdentity toArtifactIdentity(
    final String groupId, final String artifactId, final Opt<String> classifier, final String type
  ) {
    return beanBuilder(ArtifactIdentity.class)
      .set(ArtifactIdentity::groupId).to(groupId)
      .set(ArtifactIdentity::artifactId).to(artifactId)
      .set(ArtifactIdentity::classifier).to(classifier)
      .set(ArtifactIdentity::type).to(type)
      .build()
    ;
  }

  public static org.apache.maven.model.Repository toMavenRepository(final Repository r) {
    final org.apache.maven.model.Repository result = new org.apache.maven.model.Repository();
    result.setId(r.id().toString());
    result.setUrl(r.url().toString());
    return result;
  }

  public static List<org.apache.maven.model.Repository> toMavenRepositories(
    final IList<Repository> repositories
  ) {
    final List<org.apache.maven.model.Repository> result = new ArrayList<>();
    repositories.forEach(r -> result.add(toMavenRepository(r)));
    return result;
  }

  public static Repository toRepository(final org.apache.maven.model.Repository r) {
    return beanBuilder(Repository.class)
      .set(Repository::id).to(TypedString.create(RepositoryId.class, r.getId()))
      .set(Repository::url).to(URI.create(r.getUrl()))
      .build()
    ;
  }

  public static IList<Repository> toRepositories(final List<org.apache.maven.model.Repository> repositories) {
    final IList.Builder<Repository> result = ICollections.listBuilder();
    repositories.forEach(r -> result.add(toRepository(r)));
    return result.build();
  }

}
