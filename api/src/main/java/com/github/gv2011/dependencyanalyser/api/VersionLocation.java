package com.github.gv2011.dependencyanalyser.api;

import com.github.gv2011.util.beans.Bean;
import com.github.gv2011.util.icol.Opt;

/**
 * Where and how one dependency's version is declared: which pom declared
 * it, the version itself, and whether the declaration is a plain
 * {@code <dependencies>} entry or a {@code <dependencyManagement>} one.
 */
public interface VersionLocation extends Bean {

  /**
   * The pom that declared this version - absent means the leaf project
   * itself, which may not have fully-known coordinates of its own (see
   * {@link PomDependencyDeclarations}).
   */
  Opt<MavenCoordinates> declaringPom();

  Version version();

  /**
   * True if declared inside {@code <dependencyManagement>}; false if
   * declared directly in {@code <dependencies>}.
   */
  Boolean managed();

}
