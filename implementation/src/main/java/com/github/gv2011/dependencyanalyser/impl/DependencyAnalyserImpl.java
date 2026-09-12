package com.github.gv2011.dependencyanalyser.impl;

import static com.github.gv2011.dependencyanalyser.api.MavenScope.COMPILE;
import static com.github.gv2011.util.BeanUtils.beanBuilder;

import java.nio.file.Path;

import com.github.gv2011.dependencyanalyser.api.ArtifactIdentity;
import com.github.gv2011.dependencyanalyser.api.Classpath;
import com.github.gv2011.dependencyanalyser.api.DependencyAnalyser;
import com.github.gv2011.dependencyanalyser.api.ResolvedDependency;
import com.github.gv2011.dependencyanalyser.api.Version;
import com.github.gv2011.util.icol.ICollections;
import com.github.gv2011.util.icol.ISet;

public class DependencyAnalyserImpl implements DependencyAnalyser{

  @Override
  public ISet<ResolvedDependency> resolvedDependencies(final Path projectDirectory, final Classpath classpath) {
    return ICollections.<ResolvedDependency>setBuilder()
      .add(beanBuilder(ResolvedDependency.class)
        .set(ResolvedDependency::identity).to(beanBuilder(ArtifactIdentity.class)
          .set(ArtifactIdentity::groupId).to("some.group")
          .set(ArtifactIdentity::artifactId).to("artifact-1")
          .set(ArtifactIdentity::type).to("jar")
          .build()
        )
        .set(ResolvedDependency::version).to(parseVersion("1.2.3"))
        .set(ResolvedDependency::scope).to(COMPILE)
        .build()
      )
      .build()
    ;
  }

  @Override
  public Version parseVersion(final String version) {
    return VersionImpl.parse(version);
  }

}
