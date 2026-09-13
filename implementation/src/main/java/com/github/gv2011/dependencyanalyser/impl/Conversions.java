package com.github.gv2011.dependencyanalyser.impl;

import static com.github.gv2011.util.BeanUtils.beanBuilder;

import org.apache.maven.project.MavenProject;

import com.github.gv2011.dependencyanalyser.api.ArtifactIdentity;
import com.github.gv2011.dependencyanalyser.api.ResolvedDependency;
import com.github.gv2011.util.icol.Opt;

public final class Conversions {

  private Conversions(){};

  public static ResolvedDependency toResolvedDependency(final MavenProject p){
    return null;
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
