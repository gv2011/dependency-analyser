package com.github.gv2011.dependencyanalyser.api;

import com.github.gv2011.util.beans.Bean;
import com.github.gv2011.util.icol.ISet;
import com.github.gv2011.util.icol.Opt;

/**
 * Corresponds to what an effective POM tells you, with exception of getVersionDeclarations.
 */
public interface Project extends Bean{

  MavenCoordinates coordinates();

  Opt<MavenCoordinates> parent();

  ISet<Dependency> boms();

  ISet<Dependency> dependencies();

  /**
   * @return version declarations directly done in this projects pom file.
   */
  ISet<VersionDeclaration> getVersionDeclarations();

}
