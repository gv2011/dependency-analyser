package com.github.gv2011.dependencyanalyser.mvnapi;

import com.github.gv2011.dependencyanalyser.api.ArtifactIdentity;
import com.github.gv2011.dependencyanalyser.api.Version;
import com.github.gv2011.util.beans.Bean;

/**
 * A Maven project's own coordinates: its {@link ArtifactIdentity} plus
 * version. Distinct from {@link com.github.gv2011.dependencyanalyser.api.ResolvedDependency},
 * which additionally carries a {@code MavenScope} - the project being built
 * is not a dependency of anything here, so it has no scope to report.
 */
public interface MavenCoordinates extends Bean {

  ArtifactIdentity identity();

  Version version();

}
