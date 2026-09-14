package com.github.gv2011.dependencyanalyser.api;

import com.github.gv2011.util.beans.Bean;
import com.github.gv2011.util.icol.Opt;

/**
 * The identity of a Maven artifact across all its released versions — e.g.
 * "The New York Times, International Edition" as opposed to one specific
 * day's issue of it (see {@link MavenCoordinates}, which adds the
 * version).
 *
 * <p>groupId + artifactId + classifier + type: matches Maven's own notion of
 * "the same artifact at a different version" ({@code Artifact.key()}).
 * Classifier and type are included deliberately — two artifacts that share
 * a groupId, artifactId and version but differ in classifier or type (e.g.
 * a platform-specific native jar) are genuinely different artifacts, not
 * two versions of the same one.
 */
public interface ArtifactIdentity extends Bean{

  String groupId();

  String artifactId();

  /**
   * Absent for the common case of no classifier (a plain jar); present for
   * e.g. {@code sources}, {@code tests}, or a platform-specific classifier.
   */
  Opt<String> classifier();

  /**
   * Packaging/type, e.g. {@code jar}, {@code pom}.
   */
  String type();

}
