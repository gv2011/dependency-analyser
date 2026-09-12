package com.github.gv2011.dependencyanalyser.api;

/**
 * One artifact on a resolved classpath, after Maven's own conflict
 * resolution — corresponds to one line of {@code mvn dependency:list} output.
 * Has an {@link ArtifactIdentity} plus the version and scope it was actually
 * resolved at; it is not itself a kind of identity.
 */
public interface ResolvedDependency {

  ArtifactIdentity identity();

  String version();

  /**
   * The scope this specific dependency was actually resolved at — not the
   * {@link Classpath} that was queried for (which may include several
   * scopes at once; see {@link Classpath#TEST}).
   */
  MavenScope scope();

}
