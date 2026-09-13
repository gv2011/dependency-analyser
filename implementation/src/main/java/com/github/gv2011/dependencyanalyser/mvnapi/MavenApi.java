package com.github.gv2011.dependencyanalyser.mvnapi;

import org.apache.maven.cli.MavenApiImpl;
import org.apache.maven.cli.MavenCliBase;

public interface MavenApi {

  public static final String MULTIMODULE_PROJECT_DIRECTORY = MavenCliBase.MULTIMODULE_PROJECT_DIRECTORY;

  static MavenApi createApi(){
    return new MavenApiImpl();
  }

  /**
   * Runs Maven against the given working directory. Never reads or writes
   * System.out/System.err, never converts what happened into a bare exit
   * code - see MavenApiResult.
   */
  MavenApiResult doMain(final String[] args, final String workingDirectory);
}
