package com.github.gv2011.dependencyanalyser.api;

/**
 * A resolved dependency's actual Maven scope (the last column of
 * {@code mvn dependency:list} output) — not to be confused with
 * {@link Classpath}, which is the classpath being queried for, not a
 * per-dependency fact.
 */
public enum MavenScope {

  COMPILE,
  PROVIDED,
  RUNTIME,
  TEST,
  SYSTEM

}
