package com.github.gv2011.dependencyanalyser.api;

import com.github.gv2011.util.beans.Bean;
import com.github.gv2011.util.icol.ISet;
import com.github.gv2011.util.icol.Opt;

/**
 * Represents a node in a Maven dependency tree.
 *
 * <p>The root node corresponds to the project itself, while child nodes
 * represent direct dependencies (depth 1) or transitive dependencies (depth 2+).
 */
public interface DependencyNode extends Bean {

  /**
   * The Maven coordinates (identity + version) of this node.
   */
  MavenCoordinates coordinates();

  /**
   * The effective scope of this dependency relative to the root project.
   *
   * <p>Absent for the root project node itself. Present for all dependency nodes.
   */
  Opt<MavenScope> scope();

  /**
   * Indicates whether this dependency is directly declared in the current module's
   * {@code <dependencies>} section or directly inherited from a parent POM's
   * {@code <dependencies>} section (depth 1), as opposed to being brought in purely
   * transitively by another third-party library.
   */
  Boolean isDirect();

  /**
   * Indicates whether this dependency was inherited directly from a parent POM.
   */
  Boolean isInherited();

  /**
   * The child dependencies pulled in by this node.
   */
  ISet<DependencyNode> children();

}