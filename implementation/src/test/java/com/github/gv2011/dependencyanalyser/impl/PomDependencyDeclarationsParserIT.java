package com.github.gv2011.dependencyanalyser.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.github.gv2011.util.ex.NotYetImplementedException;

/**
 * Parses dependency-analyser-example's own, real pom.xml - fetched via
 * getPom the same way DependencyAnalyserImplIT does; see that class's
 * javadoc for the local-repository-install requirement this shares.
 *
 * <p>example/pom.xml states no {@code <version>} of its own - it inherits
 * the reactor's version from its {@code <parent>} - which is exactly the
 * "not yet implemented" edge case
 * PomDependencyDeclarationsParser was deliberately left unresolved for,
 * per PomDependencyDeclarations's own contract (groupId/version may be
 * inherited; artifactId cannot be). This test asserts that edge case
 * actually triggers on a real, unmodified pom.xml - not a contrived one -
 * rather than exercising the successful path; see
 * PomDependencyDeclarationsParserTest for that, against a hand-written
 * fixture that does state its own version.
 *
 * <p>Because PomDependencyDeclarationsParser.parse(...) builds its result
 * eagerly, this one field failing means parent() and
 * dependencyDeclarations() cannot be exercised via this real fixture
 * either, even though nothing is wrong with either of them here - worth
 * knowing if this ever needs revisiting.
 */
class PomDependencyDeclarationsParserIT {

  @Test
  void exampleModuleOwnVersionIsInheritedNotYetImplemented() {
    final String pomContent = new DependencyAnalyserImpl().getPom(ExampleModule.coordinates());

    assertThrows(
      NotYetImplementedException.class,
      () -> PomDependencyDeclarationsParser.parse(pomContent)
    );
  }

}
