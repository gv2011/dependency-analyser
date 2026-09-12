package com.github.gv2011.dependencyanalyser.api;

/**
 * The identity of a Maven artifact across all its released versions — e.g.
 * "The New York Times" the publication, as opposed to one specific edition
 * of it (see {@link ResolvedDependency}, which adds the version).
 *
 * <p>Just groupId + artifactId. Classifier and type distinguish which
 * attached file of a given version (e.g. the sources jar), not which
 * version — they belong on {@link ResolvedDependency}, not here.
 */
public interface GroupAndArtifact {

  String groupId();

  String artifactId();

}
