package com.github.gv2011.dependencyanalyser.api;

/**
 * A Maven dependency scope: {@code compile}, {@code provided},
 * {@code runtime}, {@code test}, {@code system}, or {@code import} (see
 * {@link #IMPORT} for why that last one doesn't fit the others). See
 * Maven's own reference for what each of the first five means and how it affects a dependency's
 * transitivity: <a href="https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html">
 * Introduction to the Dependency Mechanism</a>.
 *
 * <p>Not to be confused with {@link Classpath}: a scope is a property of
 * one dependency; a classpath is a set of artifacts. Which scopes
 * contribute to which classpath is itself part of Maven's own scope
 * semantics, linked above, and is not repeated here.
 */
public enum MavenScope {

  COMPILE,
  PROVIDED,
  RUNTIME,
  TEST,
  SYSTEM,

  /**
   * Not a classpath scope like the other five - only valid inside
   * {@code <dependencyManagement>}, where it means "merge this other
   * pom's own dependencyManagement in here" (a BOM import), rather than
   * "this artifact belongs on some classpath". No artifact with this
   * scope is ever resolved onto a classpath.
   */
  IMPORT

}
