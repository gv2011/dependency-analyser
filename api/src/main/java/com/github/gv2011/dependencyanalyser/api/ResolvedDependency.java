package com.github.gv2011.dependencyanalyser.api;

/**
 * One artifact on a resolved classpath, after Maven's own conflict
 * resolution — corresponds to one line of {@code mvn dependency:list} output.
 * One specific version of an {@link ArtifactIdentity} — one issue, not the
 * publication itself.
 */
public interface ResolvedDependency extends ArtifactIdentity {

  String version();

  /**
   * The scope this specific dependency was actually resolved at — not the
   * {@link Classpath} that was queried for (which may include several
   * scopes at once; see {@link Classpath#TEST}).
   */
  MavenScope scope();

}
