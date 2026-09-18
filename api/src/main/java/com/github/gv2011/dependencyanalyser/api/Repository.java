package com.github.gv2011.dependencyanalyser.api;

import java.net.URI;

import com.github.gv2011.util.beans.Bean;

/**
 * A Maven repository - just enough to identify and reach one, not
 * Maven's own richer notion (layout, release/snapshot policies, etc.,
 * none of which this project uses). Kept separate from
 * org.apache.maven.model.Repository so that type stays internal, the
 * same reasoning MavenCoordinates/ArtifactIdentity already follow for
 * Maven's own model classes.
 */
public interface Repository extends Bean {

  /**
   * Needed for identity, not just labeling: whether a later declaration
   * replaces an earlier one (Maven's own addRepository(repository,
   * replace) semantics) is decided by matching this.
   */
  RepositoryId id();

  URI url();

}