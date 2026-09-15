package com.github.gv2011.dependencyanalyser.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import com.github.gv2011.dependencyanalyser.api.PomDependencyDeclarations;

/**
 * Parses dependency-analyser-example's own, real pom.xml - fetched via
 * getPom the same way DependencyAnalyserImplIT does; see that class's
 * javadoc for the local-repository-install requirement this shares.
 *
 * <p>example/pom.xml states neither its own {@code groupId} nor its own
 * {@code version} - both are inherited from its {@code <parent>}. That is
 * exactly why PomDependencyDeclarations represents them as individual
 * {@code Opt}-wrapped attributes rather than a single, all-or-nothing
 * {@code coordinates()}: a real, unmodified pom hits this immediately,
 * not as some rare corner case.
 *
 * <p>example's own {@code slf4j-api} dependency has no {@code <version>}
 * of its own either (it relies on the root pom's dependencyManagement),
 * so it correctly does not appear in dependencyDeclarations() here - see
 * PomDependencyDeclarationsParserTest for a fixture where a dependency
 * does carry its own version.
 */
class PomDependencyDeclarationsParserIT {

  @Test
  void parsesExampleModulesRealPom() {
    final String pomContent = new DependencyAnalyserImpl().getPom(ExampleModule.coordinates());
    final PomDependencyDeclarations declarations = PomDependencyDeclarationsParser.parse(pomContent);

    assertThat(
      "groupId is inherited, not stated in example's own text",
      declarations.groupId().isPresent(),
      is(false)
    );
    assertThat(declarations.artifactId(), is("dependency-analyser-example"));
    assertThat(
      "version is inherited, not stated in example's own text",
      declarations.version().isPresent(),
      is(false)
    );

    assertThat("expected a <parent>", declarations.parent().isPresent(), is(true));
    assertThat(declarations.parent().get().identity().artifactId(), is("dependency-analyser"));

    assertThat(
      "slf4j-api has no version of its own in example/pom.xml - must not appear",
      declarations.dependencyDeclarations().isEmpty(),
      is(true)
    );
  }

}
