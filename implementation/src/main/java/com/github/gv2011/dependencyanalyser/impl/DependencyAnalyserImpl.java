package com.github.gv2011.dependencyanalyser.impl;

import static com.github.gv2011.util.ex.Exceptions.notYetImplemented;

import java.nio.file.Path;

import com.github.gv2011.dependencyanalyser.api.DependencyAnalyser;
import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;
import com.github.gv2011.dependencyanalyser.api.Project;
import com.github.gv2011.dependencyanalyser.api.Version;

public class DependencyAnalyserImpl implements DependencyAnalyser {

  @Override
  public Project getProject(final Path projectDirectory) {
    return notYetImplemented();
  }

  @Override
  public Project getProject(final MavenCoordinates projectCoordinates) {
    return notYetImplemented();
  }

  @Override
  public Version parseVersion(final String version) {
    return VersionImpl.parse(version);
  }

}
