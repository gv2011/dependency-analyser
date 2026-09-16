package com.github.gv2011.dependencyanalyser.impl;

import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;
import com.github.gv2011.dependencyanalyser.api.Version;

/**
 * Test-support only: dependency-analyser-example's own coordinates, for
 * tests that need to getPom(...) it rather than read it from a directory
 * on disk. Requires it already installed to the local repository - see
 * DependencyAnalyserImplIT's own javadoc for what that does and does not
 * guarantee.
 */
final class ExampleModule {

  private static final String GROUP_ID = "com.github.gv2011";
  private static final String IMPLEMENTATION_ARTIFACT_ID = "dependency-analyser-implementation";
  private static final String ARTIFACT_ID = "dependency-analyser-example";

  private ExampleModule(){}

  static MavenCoordinates projectCoordinates() {
    return Conversions.toMavenCoordinates(GROUP_ID, ARTIFACT_ID, reactorVersion().toString(), "jar");
  }

  /**
   * example shares this reactor's version with implementation - neither
   * overrides its parent's version - so implementation's own
   * pom.properties (read via PomPropertiesReader) gives example's
   * correct version too. Presence is guaranteed here: only called from
   * IT classes, which only ever run after packaging.
   */
  private static Version reactorVersion() {
    return new PomPropertiesReader(GROUP_ID, IMPLEMENTATION_ARTIFACT_ID).readVersion().get();
  }

}
