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

import com.github.gv2011.dependencyanalyser.api.Dependency;
import com.github.gv2011.dependencyanalyser.api.DependencyAnalyser;
import com.github.gv2011.dependencyanalyser.api.MavenScope;
import com.github.gv2011.dependencyanalyser.api.Project;

class DependencyAnalyserImplTest {

  @Test
  void loadServiceTest() {
    assertNotNull(DependencyAnalyser.instance());
  }

  /**
   * No parent, no BOM import - deliberately, so BridgingModelResolver is
   * never actually called and this needs no network/repository access
   * at all. Covers the core mechanics only; a project with a real
   * parent/BOM chain needs *IT coverage instead (per PR26), not added
   * here yet.
   */
  @Test
  void getProjectOfSimplePomTest(@TempDir final Path dir) throws IOException {
    Files.writeString(dir.resolve("pom.xml"), """
      <?xml version="1.0" encoding="UTF-8"?>
      <project xmlns="http://maven.apache.org/POM/4.0.0">
        <modelVersion>4.0.0</modelVersion>
        <groupId>com.example</groupId>
        <artifactId>simple</artifactId>
        <version>1.0</version>
        <dependencies>
          <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>2.0.19</version>
          </dependency>
        </dependencies>
      </project>
      """, StandardCharsets.UTF_8);

    final Project project = new DependencyAnalyserImpl().getProject(dir);

    assertThat(project.coordinates().identity().groupId(), is("com.example"));
    assertThat(project.coordinates().identity().artifactId(), is("simple"));
    assertThat(project.coordinates().version().toString(), is("1.0"));
    assertThat("no <parent>", project.parent().isPresent(), is(false));
    assertThat("no BOM imports", project.boms().isEmpty(), is(true));

    final Dependency dependency = project.dependencies().stream()
      .filter(d -> d.coordinates().identity().artifactId().equals("slf4j-api"))
      .tryFindFirst()
      .orElseThrow(() -> new AssertionError("expected slf4j-api among effective dependencies"))
    ;
    assertThat(dependency.scope(), is(MavenScope.COMPILE));
    assertThat(dependency.coordinates().version().toString(), is("2.0.19"));

    final boolean declaredHere = project.getVersionDeclarations().stream()
      .anyMatch(vd -> vd.artifact().artifactId().equals("slf4j-api"));
    assertThat("slf4j-api's version is declared directly in this pom", declaredHere, is(true));
  }

}
