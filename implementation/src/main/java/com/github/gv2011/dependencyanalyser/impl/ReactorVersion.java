package com.github.gv2011.dependencyanalyser.impl;

import java.util.Objects;

import com.github.gv2011.dependencyanalyser.api.Version;
import com.github.gv2011.util.icol.Opt;

/**
 * This reactor's own version, read from dependency-analyser-implementation's
 * own installed pom.properties. Every module in this reactor shares the
 * same version (no module overrides its parent's version), so this is also
 * the correct version for e.g. dependency-analyser-example - no need to
 * read that module's own pom.properties separately.
 *
 * <p>Absent before this module has actually gone through the package phase
 * - e.g. in a plain {@code mvn test} run - which is a genuinely expected
 * case, not an error; callers that do need it present (e.g. an
 * integration test, which runs after packaging) should say so clearly
 * themselves rather than this class guessing at their context.
 */
public final class ReactorVersion {

  private static final String GROUP_ID = "com.github.gv2011";
  private static final String ARTIFACT_ID = "dependency-analyser-implementation";

  private ReactorVersion(){}

  public static Opt<Version> get() {
    return new PomPropertiesReader(GROUP_ID, ARTIFACT_ID).readPomProperties()
      .map(p -> Objects.requireNonNull(p.getProperty(PomPropertiesReader.VERSION_PROPERTY)))
      .map(VersionImpl::parse)
    ;
  }

}
