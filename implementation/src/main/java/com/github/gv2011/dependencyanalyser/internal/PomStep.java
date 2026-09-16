package com.github.gv2011.dependencyanalyser.internal;

import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;
import com.github.gv2011.util.beans.Bean;

/**
 * One step in the path from a leaf project to some other pom reachable
 * from it: that pom's own coordinates, and how it was reached from the
 * pom before it in the path (see {@link PomRelation}).
 *
 * <p>A full path is an ordered list of these ({@code IList<PomStep>}).
 * An empty path means "the leaf project itself" - there is no step for
 * the leaf, since it isn't reached from anything.
 */
public interface PomStep extends Bean {

  PomRelation relation();

  MavenCoordinates projectCoordinates();

}
