package com.github.gv2011.dependencyanalyser.api;

import java.nio.file.Path;

/**
 * Entry point for analysing a Maven project already checked out on disk.
 *
 * <p>Obtain an instance via {@link #instance()}; callers never depend on any
 * concrete implementation class directly.
 */
public interface Context {

  Project getProject(Path projectDirectory);

  Project getProject(MavenCoordinates projectCoordinates);

}
