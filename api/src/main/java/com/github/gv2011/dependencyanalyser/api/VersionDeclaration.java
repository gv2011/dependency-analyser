package com.github.gv2011.dependencyanalyser.api;

import com.github.gv2011.util.beans.Bean;

public interface VersionDeclaration extends Bean{

  /**
   * @return the artifact for that a version is specified
   */
  ArtifactIdentity artifact();

  /**
   * @return the project where the version is specified
   */
  MavenCoordinates declarationLocation();

  /**
   * @return the specified version
   */
  Version version();

  /**
   * @return whether the version is specified under dependencies or dependencyManagement
   */
  DependencySection section();

}
