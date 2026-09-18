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

  /**
   * Real-world case for the repository-propagation fix
   * (BridgingModelResolver.addRepository): a child pom that declares its
   * own &lt;repositories&gt; entry, whose &lt;parent&gt; only resolves
   * through that declared repository - not Central, not any default.
   *
   * <p>Uses dilbertside/bom (github.com/dilbertside/bom, artifact
   * com.github.dilbertside:bom:5.2.4) as the parent target - a real,
   * public "Bill of Materials" project, genuinely packaging=pom (unlike
   * an earlier attempt using jitpack/maven-simple, a plain jar example
   * that failed Maven's own "parent must be packaging=pom" validation -
   * that failure happened AFTER our resolver had already found and read
   * the pom via the declared repository, confirming the fix itself
   * worked; it was purely the wrong choice of target artifact).
   *
   * <p>relativePath is set empty deliberately, to force resolution
   * through the declared repository rather than a filesystem lookup.
   *
   * <p>Version 5.2.4 specifically, not a newer one: confirmed via
   * mvnrepository.com's own index of versions JitPack actually built
   * and published successfully. An earlier attempt used 5.3.1 - taken
   * from JitPack's auto-generated usage-instructions page, which
   * reflects the repo's latest tag/description, not confirmation that
   * version was ever actually built - and 5.3.1 doesn't appear in
   * mvnrepository's index at all. Requesting a never-built version
   * triggers JitPack's build-on-demand system live, during the test
   * run, which can take a very long time or never complete - almost
   * certainly what actually happened.
   *
   * <p>Needs real network access to jitpack.io and Central - "ordinary
   * connected Maven use", same standard already applied elsewhere in
   * this project (e.g. PomFetcherTest): not special setup, works
   * offline after the first connected run. See PR26 for why that keeps
   * this a Test, not an *IT.
   */
  @Test
  void getProjectOfChildDeclaringRepositoryTest(@TempDir final Path dir) throws IOException {
    Files.writeString(dir.resolve("pom.xml"), """
      <?xml version="1.0" encoding="UTF-8"?>
      <project xmlns="http://maven.apache.org/POM/4.0.0">
        <modelVersion>4.0.0</modelVersion>
        <parent>
          <groupId>com.github.dilbertside</groupId>
          <artifactId>bom</artifactId>
          <version>5.2.4</version>
          <relativePath/>
        </parent>
        <artifactId>uses-jitpack-parent</artifactId>
        <repositories>
          <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
          </repository>
        </repositories>
      </project>
      """, StandardCharsets.UTF_8);

    final Project project = new DependencyAnalyserImpl().getProject(dir);

    assertThat("parent should resolve via the child's own declared repository", project.parent().isPresent(), is(true));
    assertThat(project.parent().get().identity().groupId(), is("com.github.dilbertside"));
    assertThat(project.parent().get().identity().artifactId(), is("bom"));
    assertThat(project.parent().get().version().toString(), is("5.2.4"));
  }

}
