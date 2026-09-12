package com.github.gv2011.dependencyanalyser.api;

import java.util.Optional;

/**
 * One artifact on a resolved classpath, after Maven's own conflict
 * resolution — corresponds to one line of {@code mvn dependency:list} output.
 */
public interface ResolvedDependency {

  String groupId();

  String artifactId();

  /**
   * Absent for the common case of no classifier (a plain jar); present for
   * e.g. {@code sources}, {@code tests}, or a platform-specific classifier.
   */
  Optional<String> classifier();

  /**
   * Packaging/type of the resolved artifact, e.g. {@code jar}, {@code pom}.
   */
  String type();

  String version();

  /**
   * The scope this specific dependency was actually resolved at — not the
   * {@link Classpath} that was queried for (which may include several
   * scopes at once; see {@link Classpath#TEST}).
   */
  MavenScope scope();

}
