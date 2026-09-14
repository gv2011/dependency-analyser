package com.github.gv2011.dependencyanalyser.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import com.github.gv2011.dependencyanalyser.api.Classpath;
import com.github.gv2011.dependencyanalyser.api.ResolvedDependency;
import com.github.gv2011.util.icol.ISet;

/**
 * Actually shells out to embedded Maven and hits Maven Central - runs under
 * `mvn verify` (failsafe), not `mvn test`.
 */
class DependencyAnalyserImplIT {

  @Test
  void testResolvedDependenciesOfApiModule() {
    // The api module's own pom.xml declares exactly one dependency:
    // com.github.gv2011:util-apis. Checking for its presence rather than
    // asserting the full resolved set, since util-apis' own transitive
    // dependencies (if any) aren't known here.
    final Path apiModuleDirectory = Paths.get("..", "api");
    final ISet<ResolvedDependency> deps =
      new DependencyAnalyserImpl().resolvedDependencies(apiModuleDirectory, Classpath.MAIN)
    ;
    final boolean containsUtilApis = deps.stream().anyMatch(d ->
      d.identity().groupId().equals("com.github.gv2011")
      && d.identity().artifactId().equals("util-apis")
    );
    assertThat(containsUtilApis, is(true));
  }

}
