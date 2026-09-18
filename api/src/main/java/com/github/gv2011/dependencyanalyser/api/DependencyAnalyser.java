package com.github.gv2011.dependencyanalyser.api;

import java.util.NoSuchElementException;
import java.util.ServiceLoader;

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

  Context createContext();

  Version parseVersion(String version);

}
