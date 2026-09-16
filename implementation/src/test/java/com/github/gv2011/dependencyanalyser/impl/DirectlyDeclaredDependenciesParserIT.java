package com.github.gv2011.dependencyanalyser.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import com.github.gv2011.dependencyanalyser.api.DirectlyDeclaredDependencies;
import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;
import com.github.gv2011.util.icol.Opt;

/**
 * Parses dependency-analyser-example's own, real pom.xml - fetched via
 * getPom the same way DependencyAnalyserImplIT does; see that class's
 * javadoc for the local-repository-install requirement this shares.
 *
 * <p>example/pom.xml states neither its own {@code groupId} nor its own
 * {@code version} - both are inherited from its {@code <parent>}. That's
 * not a problem here: this test supplies example's coordinates as
 * already-known (the same coordinates used to fetch it in the first
 * place), so DirectlyDeclaredDependenciesParser never has to derive them
 * from example's own text - see DirectlyDeclaredDependenciesParserTest
 * for the case where that derivation does happen, against a fixture
 * that states its own coordinates directly.
 *
 * <p>example's own {@code slf4j-api} dependency has no {@code <version>}
 * of its own either (it relies on the root pom's dependencyManagement),
 * so it correctly does not appear in dependencyDeclarations() here.
 */
class DirectlyDeclaredDependenciesParserIT {

  @Test
  void parsesExampleModulesRealPom() {
    final MavenCoordinates exampleCoordinates = ExampleModule.coordinates();
    final String pomContent = new DependencyAnalyserImpl().getPom(exampleCoordinates);
    final DirectlyDeclaredDependencies declarations =
      DirectlyDeclaredDependenciesParser.parse(pomContent, Opt.of(exampleCoordinates))
    ;

    assertThat(declarations.mavenCoordinates(), is(exampleCoordinates));

    assertThat("expected a <parent>", declarations.parent().isPresent(), is(true));
    assertThat(declarations.parent().get().identity().artifactId(), is("dependency-analyser"));

    assertThat(
      "slf4j-api has no version of its own in example/pom.xml - must not appear",
      declarations.dependencyDeclarations().isEmpty(),
      is(true)
    );
  }

}
