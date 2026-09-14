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
   * The resolved dependencies of one Maven module, after Maven's own
   * conflict resolution — i.e. what {@code mvn dependency:list} reports, not
   * the module's own directly-declared {@code <dependencies>}.
   *
   * @param projectDirectory directory containing the module's {@code pom.xml}
   * @param classpath which classpath to resolve; see {@link Classpath}
   */
  ISet<Dependency> resolvedDependencies(Path projectDirectory, Classpath classpath);

  Version parseVersion(String version);

}
