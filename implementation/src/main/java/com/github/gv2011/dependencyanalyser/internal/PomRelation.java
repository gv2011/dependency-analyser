package com.github.gv2011.dependencyanalyser.internal;

/**
 * How one pom in a parent/BOM-import chain was reached from the pom
 * before it in that chain.
 */
public enum PomRelation {

  /** Reached via the previous pom's own {@code <parent>} element. */
  PARENT,

  /**
   * Reached via a {@code <dependencyManagement>} entry in the previous
   * pom with {@code <scope>import</scope><type>pom</type>} - a BOM
   * import.
   */
  BOM_IMPORT

}
