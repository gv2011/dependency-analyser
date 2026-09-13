package com.github.gv2011.dependencyanalyser.mvnapi;

import java.nio.file.Path;

import org.apache.maven.cli.MavenApiImpl;

public interface MavenApi {

  static MavenApi createApi(){
    return new MavenApiImpl();
  }

  /**
   * Runs Maven against the given project directory. Never converts what
   * happened into a bare exit code - see {@link MavenApiResult}.
   *
   * <p>Temporarily redirects System.out for the duration of the call, to
   * capture what the goal itself wrote (see {@link MavenApiResult#output()}
   * and {@link CapturedSystemOut}) - some goals have no other way to report
   * their findings. Nothing reaches the real console or disk while this
   * runs, and System.out is always restored before returning, success or
   * failure.
   */
  MavenApiResult doMain(final String[] args, final Path projectDirectory);
}
