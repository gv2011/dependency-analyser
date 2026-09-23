package com.github.gv2011.dependencyanalyser.api;

import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;

import com.github.gv2011.util.icol.IList;

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

  /**
   * @param additionalRepositories repositories to search in addition to
   *   whatever this project's own text (and its parent chain) declares.
   *   Typically {@link Project#additionalRepositories()} from whichever
   *   project referenced this one - see that method's own javadoc.
   *   Central is always searched too, automatically; do not include it
   *   here.
   */
  Project getProject(Path projectDirectory, IList<Repository> additionalRepositories);

  /**
   * @param additionalRepositories see the other {@code getProject}
   *   overload - same meaning here.
   */
  Project getProject(MavenCoordinates projectCoordinates, IList<Repository> additionalRepositories);

  Version parseVersion(String version);

  RepositoryId parseRepositoryId(String repositoryId);

  /**
   * {@code groupId:artifactId:type[:classifier]} - the format of Maven's own
   * {@code Artifact.getDependencyConflictId()}, see
   * https://github.com/apache/maven/blob/maven-3.9.11/maven-artifact/src/main/java/org/apache/maven/artifact/DefaultArtifact.java
   */
  String format(ArtifactIdentity artifactIdentity);

  /**
   * {@code groupId:artifactId:type[:classifier]:version} - the format of Maven's
   * own {@code Artifact.getId()}, as also shown by {@code dependency:list} (without the scope).
   */
  String format(MavenCoordinates mavenCoordinates);

}
