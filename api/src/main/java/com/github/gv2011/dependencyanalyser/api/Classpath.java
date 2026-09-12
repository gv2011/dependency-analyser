package com.github.gv2011.dependencyanalyser.api;

/**
 * Which classpath of a Maven module to resolve dependencies for.
 *
 * <p>Deliberately does not cover the build classpath (plugins and their own
 * dependencies) — that is not part of a module's {@code <dependencies>}
 * resolution at all, and needs a different mechanism entirely.
 */
public enum Classpath {

  /**
   * Compile + runtime scope: what actually ships / is consumed at runtime.
   */
  MAIN,

  /**
   * Test scope: everything {@link #MAIN} contains, plus test-only additions.
   */
  TEST

}
