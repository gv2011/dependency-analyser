package com.example.majorupdates.util;

import java.util.Objects;
import java.util.Optional;

public final class GroupAndArtifact {

  public static final String GROUP_ID = "com.github.gv2011";
  static final String ARTIFACT_ID = "dependency-analyser-shared";

  public static final Optional<String> VERSION =
    new PomPropertiesReader(GROUP_ID, ARTIFACT_ID).readPomProperties()
    .map(p->Objects.requireNonNull(p.getProperty(PomPropertiesReader.VERSION_PROPERTY)))
  ;

}
