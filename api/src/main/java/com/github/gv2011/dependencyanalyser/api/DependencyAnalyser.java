package com.github.gv2011.dependencyanalyser.api;

import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;

import com.github.gv2011.util.icol.ISet;

/**
 * Entry point for analysing a Maven project already checked out on disk.
 *
 * <p>Obtain an instance via {@link #instance()}; callers never depend on any
 * concrete implementation class directly.
 */
public interface DependencyAnalyser {

  /**
   * Locates the {@link ServiceLoader}-registered implementation on the
   * classpath.
   *
   * @throws NoSuchElementException if no implementation is registered
   */
  static DependencyAnalyser instance() {
    return ServiceLoader.load(DependencyAnalyser.class).findFirst().get();
  }

  MavenCoordinates getCoordinates(Path projectDirectory);

  ISet<Dependency> getDependencies(MavenCoordinates project, Classpath classpath);

  ISet<ArtifactIdentity> getDirectlyDeclaredDependencies(MavenCoordinates project);

  ISet<VersionDeclaration> getVersionDeclarations(MavenCoordinates project);

  Version parseVersion(String version);

}
