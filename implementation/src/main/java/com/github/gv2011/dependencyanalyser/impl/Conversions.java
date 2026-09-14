package com.github.gv2011.dependencyanalyser.impl;

import static com.github.gv2011.util.BeanUtils.beanBuilder;

import org.apache.maven.project.MavenProject;

import com.github.gv2011.dependencyanalyser.api.ArtifactIdentity;
import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;
import com.github.gv2011.util.icol.Opt;

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

}
