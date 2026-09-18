package com.github.gv2011.dependencyanalyser.api;

import com.github.gv2011.util.icol.IList;
import com.github.gv2011.util.icol.ISet;
import com.github.gv2011.util.icol.Opt;

/**
 * Corresponds to what an effective POM tells you, with exception of boms and getVersionDeclarations.
 *
 * <p>Deliberately not a {@code Bean}: an effective POM build (and, for
 * boms(), a separate one) can be genuinely expensive - implementations
 * are expected to compute lazily and cache, not eagerly build
 * everything just because some Project was constructed.
 */
public interface Project {

  MavenCoordinates coordinates();

  /**
   * List of repositories Maven searches for an artifact. Central is
   * always used last and is not included here.
   *
   * <p>Pass this straight into the next {@code getProject(...)} call
   * when resolving something this project references (its parent, a
   * BOM). It already includes everything this project itself declares,
   * on top of whatever was passed in when this Project was created - so
   * there is nothing left to add yourself.
   */
  IList<Repository> additionalRepositories();

  Opt<MavenCoordinates> parent();

  ISet<Dependency> boms();

  ISet<Dependency> dependencies();

  /**
   * @return version declarations directly done in this projects pom file.
   */
  ISet<VersionDeclaration> getVersionDeclarations();

}
