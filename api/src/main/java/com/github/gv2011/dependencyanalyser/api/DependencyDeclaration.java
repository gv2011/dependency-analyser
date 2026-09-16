package com.github.gv2011.dependencyanalyser.api;

import com.github.gv2011.util.beans.Bean;

/**
 * One version-bearing dependency declaration, exactly as written in a
 * single pom.xml's own {@code <dependencies>} or
 * {@code <dependencyManagement>} element - not merged with anything
 * inherited from a parent, and not expanded from an imported BOM.
 *
 * <p>A declaration with no {@code <version>} of its own (relying on
 * {@code dependencyManagement}, inherited or otherwise, to supply one) is
 * not represented by this type at all - see
 * {@link DirectlyDeclaredDependencies#dependencyDeclarations()}.
 */
public interface DependencyDeclaration extends Bean {

  /**
   * The artifact this declaration is for, including its version -
   * exactly what this pom's own {@code <version>} states, never
   * inferred or inherited from elsewhere.
   */
  MavenCoordinates coordinates();

  /**
   * True if declared inside {@code <dependencyManagement>}; false if
   * declared directly in {@code <dependencies>}.
   */
  Boolean managed();

  /**
   * True only when {@link #managed()} is true and this entry is a BOM
   * import ({@code <scope>import</scope><type>pom</type>}) - a reference
   * to another pom.xml whose own {@code <dependencyManagement>} should be
   * treated as if written here, not a dependency in its own right.
   */
  Boolean isBomImport();

}
