package com.github.gv2011.dependencyanalyser.api;

import com.github.gv2011.util.beans.Bean;

/**
 * A Maven project's own coordinates: its {@link ArtifactIdentity} plus
 * version. Distinct from {@link ResolvedDependency}, which additionally
 * carries a {@code MavenScope} - the project being built is not a
 * dependency of anything here, so it has no scope to report.
 */
public interface MavenCoordinates extends Bean {

  ArtifactIdentity identity();

  Version version();

}
