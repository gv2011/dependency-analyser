package com.github.gv2011.dependencyanalyser.api;

import com.github.gv2011.util.beans.Bean;

/**
 * A specific version of an artifact together with the Maven scope it has
 * for one particular project.
 */
public interface Dependency extends Bean{

  MavenCoordinates coordinates();

  MavenScope scope();

}
