package com.github.gv2011.dependencyanalyser.api;

import com.github.gv2011.util.beans.Bean;

/**
 * A Maven project's own coordinates: its {@link ArtifactIdentity} plus
 * version. Distinct from {@link Dependency}, which additionally carries a
 * {@code MavenScope} - a Maven project has no scope of its own; scope is
 * a property of a dependency relationship, not of a project.
 */
public interface MavenCoordinates extends Bean {

  ArtifactIdentity identity();

  Version version();

}
