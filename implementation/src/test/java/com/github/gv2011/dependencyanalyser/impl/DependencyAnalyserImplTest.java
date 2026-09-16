package com.github.gv2011.dependencyanalyser.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.github.gv2011.dependencyanalyser.api.DependencyAnalyser;
import com.github.gv2011.dependencyanalyser.api.DirectlyDeclaredDependencies;

class DependencyAnalyserImplTest {

  @Test
  void loadServiceTest() {
    assertNotNull(DependencyAnalyser.instance());
  }

  /**
   * Writes the same hand-written fixture DirectlyDeclaredDependenciesParserTest
   * uses into a fresh JUnit-managed temp directory, rather than pointing at
   * any checked-out sibling module directory - same reasoning as
   * DependencyAnalyserImplIT's own move away from Paths.get("..", "api"):
   * no dependence on the reactor's source-directory layout being present
   * at test run time.
   */
  @Test
  void directlyDeclaredDependenciesOfDirectoryTest(@TempDir final Path tempDir) throws IOException {
    Files.writeString(
      tempDir.resolve("pom.xml"), TestResources.read("/sample-pom.xml"), StandardCharsets.UTF_8
    );
    final DirectlyDeclaredDependencies declarations =
      new DependencyAnalyserImpl().directlyDeclaredDependencies(tempDir)
    ;
    assertThat(declarations.mavenCoordinates().identity().artifactId(), is("sample-pom-declarations-fixture"));
  }

}
