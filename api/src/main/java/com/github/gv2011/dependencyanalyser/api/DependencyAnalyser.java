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

  /**
   * The dependencies of one Maven module — the actual artifacts it needs
   * — as opposed to the dependency declarations in the module's own
   * {@code pom.xml}, which only declare them.
   *
   * <p>A declaration and a dependency are not the same thing, and don't
   * correspond one-to-one. A single declaration can pull in further
   * dependencies transitively (through the declared artifact's own
   * declarations, and so on) that this module never declares itself.
   * Conversely, when two declarations - direct or transitive - specify
   * different versions of the same artifact, only one version is
   * actually needed, so only that one is a dependency; and a declared
   * exclusion can mean an artifact that would otherwise be needed isn't
   * one at all. What this method returns is that actual, final set: the
   * same set a real build of the project would put on its classpath.
   *
   * @param projectDirectory directory containing the module's {@code pom.xml}
   * @param classpath which classpath to resolve; see {@link Classpath}
   */
  ISet<Dependency> getDependencies(Path projectDirectory, Classpath classpath);

  /**
   * What the given project's own pom.xml declares, read directly from
   * disk - unmerged with anything from a parent or an imported BOM. See
   * {@link PomDependencyDeclarations}.
   *
   * @param projectDirectory directory containing the module's {@code pom.xml}
   */
  PomDependencyDeclarations pomDependencyDeclarations(Path projectDirectory);

  /**
   * What the pom.xml identified by these coordinates declares - fetched
   * via {@link #getPom(MavenCoordinates)}, then read the same way as the
   * {@link #pomDependencyDeclarations(Path)} overload. Typically used for
   * a parent or an imported BOM, not the leaf project itself, which
   * usually has a {@code projectDirectory} to read directly instead.
   */
  PomDependencyDeclarations pomDependencyDeclarations(MavenCoordinates coordinates);

  Version parseVersion(String version);

  String getPom(MavenCoordinates coordinates);

}
