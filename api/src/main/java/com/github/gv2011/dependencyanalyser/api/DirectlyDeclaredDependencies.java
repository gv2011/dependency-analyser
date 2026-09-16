package com.github.gv2011.dependencyanalyser.api;

import com.github.gv2011.util.beans.Bean;
import com.github.gv2011.util.icol.ISet;
import com.github.gv2011.util.icol.Opt;

/**
 * The directly declared dependencies of a project - dependencies stated
 * explicitly, not (only) transitively, in the project's own pom.xml -
 * together with the project's own coordinates and its parent, if it has
 * one.
 *
 * <p>Building block for walking a project's own parent chain and imported
 * BOMs by hand: {@link #parent()} gives the next project to read for the
 * parent chain, and a {@link DependencyDeclaration#isBomImport()} entry
 * in {@link #dependencyDeclarations()} gives the next project to read
 * for a BOM. That walk, and any decision about which of the projects
 * reached this way count as "one's own", is deliberately left to the
 * caller - this type only reports one project's own declarations.
 */
public interface DirectlyDeclaredDependencies extends Bean {

  MavenCoordinates mavenCoordinates();

  /**
   * This project's own parent, if it has one.
   */
  Opt<MavenCoordinates> parent();

  /**
   * Every version-bearing dependency declaration this project's own
   * pom.xml contains - direct and managed alike, BOM imports included. A
   * declaration with no version of its own is not represented here at
   * all.
   */
  ISet<DependencyDeclaration> dependencyDeclarations();

}
