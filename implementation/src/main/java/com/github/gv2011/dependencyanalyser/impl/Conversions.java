package com.github.gv2011.dependencyanalyser.impl;

import static com.github.gv2011.util.BeanUtils.beanBuilder;
import static com.github.gv2011.util.Verify.notNull;

import java.net.URI;
import java.util.Locale;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;

import com.github.gv2011.dependencyanalyser.api.ArtifactIdentity;
import com.github.gv2011.dependencyanalyser.api.Dependency;
import com.github.gv2011.dependencyanalyser.api.DependencySection;
import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;
import com.github.gv2011.dependencyanalyser.api.MavenScope;
import com.github.gv2011.dependencyanalyser.api.Repository;
import com.github.gv2011.dependencyanalyser.api.RepositoryId;
import com.github.gv2011.dependencyanalyser.api.Version;
import com.github.gv2011.dependencyanalyser.api.VersionDeclaration;
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

  public static MavenCoordinates toMavenCoordinates(final Model model){
    return toMavenCoordinates(
      notNull(model.getGroupId()),
      notNull(model.getArtifactId()),
      VersionImpl.parse(model.getVersion()),
      packaging(model)
    );
  }

  /**
   * For coordinates known directly, rather than read from a MavenProject
   * (e.g. an artifact this reactor doesn't build itself). type is the
   * artifact's own real packaging (e.g. "jar"), not the type of anything
   * that might later be fetched about it.
   */
  public static MavenCoordinates toMavenCoordinates(
    final String groupId, final String artifactId, final Version version, final String type
  ){
    return beanBuilder(MavenCoordinates.class)
      .set(MavenCoordinates::identity).to(
        toArtifactIdentity(groupId, artifactId, Opt.empty(), type)
      )
      .set(MavenCoordinates::version).to(version)
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

  public static Repository toRepository(final org.apache.maven.model.Repository r) {
    return beanBuilder(Repository.class)
      .set(Repository::id).to(TypedString.create(RepositoryId.class, r.getId()))
      .set(Repository::url).to(URI.create(r.getUrl()))
      .build()
    ;
  }

  public static Dependency toDependency(final org.apache.maven.model.Dependency mavenDependency){
    return beanBuilder(Dependency.class)
      .set(Dependency::coordinates).to(coordinates(mavenDependency))
      .set(Dependency::scope).to(scope(mavenDependency))
      .build()
    ;
  }

  public static VersionDeclaration toVersionDeclaration(
    final org.apache.maven.model.Dependency d, final DependencySection section
  ) {
    return beanBuilder(VersionDeclaration.class)
      .set(VersionDeclaration::artifact).to(identity(d))
      .set(VersionDeclaration::version).to(VersionImpl.parse(d.getVersion()))
      .set(VersionDeclaration::section).to(section)
      .build()
    ;
  }

  public static MavenCoordinates coordinates(final org.apache.maven.model.Dependency d) {
    return beanBuilder(MavenCoordinates.class)
      .set(MavenCoordinates::identity).to(identity(d))
      .set(MavenCoordinates::version).to(VersionImpl.parse(d.getVersion()))
      .build()
    ;
  }

  public static ArtifactIdentity identity(final org.apache.maven.model.Dependency d) {
    return toArtifactIdentity(d.getGroupId(), d.getArtifactId(), Opt.ofNullable(d.getClassifier()), type(d));
  }

  public static String type(final org.apache.maven.model.Dependency d) {
    return Opt.ofNullable(d.getType()).orElse("jar");
  }

  public static MavenScope scope(final org.apache.maven.model.Dependency d) {
    return MavenScope.valueOf(Opt.ofNullable(d.getScope()).orElse("compile").toUpperCase(Locale.ROOT));
  }

  private static String packaging(final Model m) {
    return Opt.ofNullable(m.getPackaging()).orElse("jar");
  }

}
