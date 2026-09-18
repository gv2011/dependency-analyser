package com.github.gv2011.dependencyanalyser.api;

import com.github.gv2011.util.icol.ISet;
import com.github.gv2011.util.icol.Opt;

/**
 * Corresponds to what an effective POM tells you, with exception of getVersionDeclarations.
 *
 * <p>Deliberately not a {@code Bean}: an effective POM build (and, for
 * boms(), a separate one) can be genuinely expensive - implementations
 * are expected to compute lazily and cache, not eagerly build
 * everything just because some Project was constructed.
 */
public interface Project {

  MavenCoordinates coordinates();

  Opt<MavenCoordinates> parent();

  ISet<Dependency> boms();

  ISet<Dependency> dependencies();

  /**
   * @return version declarations directly done in this projects pom file.
   */
  ISet<VersionDeclaration> getVersionDeclarations();

}
