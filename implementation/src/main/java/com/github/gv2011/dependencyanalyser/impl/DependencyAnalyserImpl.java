package com.github.gv2011.dependencyanalyser.impl;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.github.gv2011.dependencyanalyser.api.DependencyAnalyser;
import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;
import com.github.gv2011.dependencyanalyser.api.Project;
import com.github.gv2011.dependencyanalyser.api.Version;

public class DependencyAnalyserImpl implements DependencyAnalyser {

  @Override
  public Project getProject(final Path projectDirectory) {
    final String pomContent;
    try {
      pomContent = Files.readString(projectDirectory.resolve("pom.xml"), StandardCharsets.UTF_8);
    }
    catch(final IOException e) {
      throw new UncheckedIOException(e);
    }
    return new LazyProject(pomContent);
  }

  @Override
  public Project getProject(final MavenCoordinates projectCoordinates) {
    return new LazyProject(PomFetcher.fetchPomContent(projectCoordinates));
  }

  @Override
  public Version parseVersion(final String version) {
    return VersionImpl.parse(version);
  }

}
