package com.example.majorupdates;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;

abstract class Dependency {

  abstract DependencyKind kind();

  abstract String groupId();

  abstract String artifactId();

  abstract Optional<Version> version();

  abstract Optional<ArtifactVersion> lookupLatestMajorUpdate(
    final VersionsHelperFacade versionsHelper, final Predicate<String> eligible
  );


  @Override
  public String toString() {
    return kind()+"|"+groupId()+":"+artifactId()+version().map(v->":"+v).orElse("");
  }

  @Override
  public final int hashCode() {
    return Arrays.hashCode(asArray());
  }

  @Override
  public final boolean equals(final Object obj) {
    return obj instanceof Dependency
      ? Arrays.equals(((Dependency)obj).asArray(), this.asArray())
      : false
    ;
  }

  private final Object[] asArray() {
    return new Object[]{kind(), groupId(), artifactId(), version()};
  }


  /**
   * Where an entry in the report was declared.
   */
  static enum DependencyKind {
    /** A normal {@code <dependency>} entry. */
    DEPENDENCY {
      @Override
      Stream<Dependency> getDependencies(final MavenProject project) {
        return project.getDependencies().stream().map(d -> new DefaultDependency(this, d));
      }
    },
    DEPENDENCY_MANAGEMENT {
      @Override
      Stream<Dependency> getDependencies(final MavenProject project) {
        return Optional.ofNullable(project.getDependencyManagement()).stream()
          .flatMap(dm -> dm.getDependencies().stream())
          .map(d -> new DefaultDependency(this, d))
        ;
      }
    },
    PLUGIN {
      @Override
      Stream<Dependency> getDependencies(final MavenProject project) {
        return project.getBuild().getPlugins().stream().map(PluginDependency::new);
      }
    };

    abstract Stream<Dependency> getDependencies(final MavenProject project);
  }


  static final class DefaultDependency extends Dependency{
    private final DependencyKind kind;
    private final org.apache.maven.model.Dependency mavenDependency;

    DefaultDependency(final DependencyKind kind, final org.apache.maven.model.Dependency mavenDependency){
      this.kind = kind;
      this.mavenDependency = mavenDependency;
    }

    @Override
    DependencyKind kind() {return kind;}

    @Override
    String groupId() {return mavenDependency.getGroupId();}

    @Override
    String artifactId() {return mavenDependency.getArtifactId();}

    @Override
    Optional<Version> version() {
      return Optional.ofNullable(mavenDependency.getVersion()).map(Version::parse);
    }

    @Override
    Optional<ArtifactVersion> lookupLatestMajorUpdate(
      final VersionsHelperFacade versionsHelper, final Predicate<String> eligible
    ){
      return versionsHelper.lookupLatestMajorUpdate(mavenDependency, eligible);
    }
  }


  static final class PluginDependency extends Dependency{
    private final Plugin plugin;

    PluginDependency(final Plugin plugin) {this.plugin = plugin;}

    @Override
    DependencyKind kind() {return DependencyKind.PLUGIN;}

    @Override
    String groupId() {return plugin.getGroupId();}

    @Override
    String artifactId() {return plugin.getArtifactId();}

    @Override
    Optional<Version> version() {
      return Optional.ofNullable(plugin.getVersion()).map(Version::parse);
    }

    @Override
    Optional<ArtifactVersion> lookupLatestMajorUpdate(
      final VersionsHelperFacade versionsHelper, final Predicate<String> eligible
    ){
      return versionsHelper.lookupLatestMajorUpdate(plugin, eligible);
    }
  }
}