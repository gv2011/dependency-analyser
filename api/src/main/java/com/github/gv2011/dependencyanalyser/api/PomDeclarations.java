package com.github.gv2011.dependencyanalyser.api;

import com.github.gv2011.util.beans.Bean;
import com.github.gv2011.util.icol.ISet;
import com.github.gv2011.util.icol.Opt;

/**
 * What a single pom.xml's own text declares - unmerged: no parent
 * inheritance, no BOM-import expansion, no interpolation. See
 * {@link DependencyAnalyser#pomDeclarations(java.nio.file.Path)} and
 * {@link DependencyAnalyser#pomDeclarations(MavenCoordinates)}.
 *
 * <p>Building block for walking a project's own parent chain and imported
 * BOMs by hand: {@link #parent()} gives the next pom to read for the
 * parent chain, and a {@link DependencySpecification#isBomImport()} entry
 * in {@link #dependencySpecifications()} gives the next pom to read for a
 * BOM. That walk, and any decision about which of the poms reached this
 * way count as "one's own", is deliberately left to the caller - this
 * type only reports one pom's own text.
 */
public interface PomDeclarations extends Bean {

  /**
   * This pom's own {@code <parent>} coordinates, if it has one.
   */
  Opt<MavenCoordinates> parent();

  /**
   * Every version-bearing dependency declaration this pom.xml's own text
   * contains - direct and managed alike, BOM imports included. A
   * declaration with no {@code <version>} of its own is not represented
   * here at all.
   */
  ISet<DependencySpecification> dependencySpecifications();

}
