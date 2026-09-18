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
import com.github.gv2011.dependencyanalyser.api.Repository;
import com.github.gv2011.util.icol.ICollections;

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

    final Project project = new DependencyAnalyserImpl().getProject(dir, ICollections.<Repository>listBuilder().build());

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
   * own &lt;repositories&gt; entry, whose imported BOM only resolves
   * through that declared repository - not Central, not any default.
   *
   * <p>More representative of the original bug report than an earlier
   * attempt at this test (parent-based, using dilbertside/bom via
   * JitPack): that report's actual symptom was missing versions across
   * many Spring Boot artifacts, almost certainly a BOM-import problem,
   * not a parent-resolution one. BOM-import resolution only happens
   * during the effective model build (dependencies()) -
   * ModelBuilderSketch.buildInterimModel deliberately stops before that
   * step, so boms() alone never actually fetches the imported pom at
   * all, just reads the raw reference. This test exercises dependencies()
   * specifically, the path that does.
   *
   * <p>Uses androidx.compose:compose-bom:2023.10.00, genuinely hosted at
   * Google's Maven repository (maven.google.com, confirmed via
   * mvnrepository.com's own "located at Google repository" note) - not
   * part of Maven's default resolution, so it needs the same explicit
   * repository declaration a private/internal repository would. Chosen
   * over the earlier JitPack-based target specifically for
   * immutability: Google's Maven repository hosts artifacts published
   * through a real release process, the same operating model Central
   * uses - not JitPack's build-on-demand-per-request model, whose
   * public artifacts are only immutable 7 days after publishing (own
   * documented policy, not an infrastructure guarantee) and have a
   * documented failure mode where a cache-evicted, never-rebuildable
   * (source archived) artifact becomes permanently unresolvable.
   *
   * <p>material3's exact managed version isn't asserted - only that
   * compose-bom's import resolved it to a real, non-empty version at
   * all, which is what the repository-propagation fix enables; hard-coding
   * the exact pinned version would make this fragile against a future
   * compose-bom update without testing anything more meaningful.
   *
   * <p>Needs real network access to maven.google.com and Central -
   * "ordinary connected Maven use", same standard already applied
   * elsewhere in this project (e.g. PomFetcherTest): not special setup,
   * works offline after the first connected run. See PR26 for why that
   * keeps this a Test, not an *IT.
   */
  @Test
  void getProjectOfChildDeclaringRepositoryTest(@TempDir final Path dir) throws IOException {
    Files.writeString(dir.resolve("pom.xml"), """
      <?xml version="1.0" encoding="UTF-8"?>
      <project xmlns="http://maven.apache.org/POM/4.0.0">
        <modelVersion>4.0.0</modelVersion>
        <groupId>com.example</groupId>
        <artifactId>uses-google-maven-bom</artifactId>
        <version>1.0</version>
        <repositories>
          <repository>
            <id>google</id>
            <url>https://maven.google.com</url>
          </repository>
        </repositories>
        <dependencyManagement>
          <dependencies>
            <dependency>
              <groupId>androidx.compose</groupId>
              <artifactId>compose-bom</artifactId>
              <version>2023.10.00</version>
              <type>pom</type>
              <scope>import</scope>
            </dependency>
          </dependencies>
        </dependencyManagement>
        <dependencies>
          <dependency>
            <groupId>androidx.compose.material3</groupId>
            <artifactId>material3</artifactId>
          </dependency>
        </dependencies>
      </project>
      """, StandardCharsets.UTF_8);

    final Project project = new DependencyAnalyserImpl().getProject(dir, ICollections.<Repository>listBuilder().build());

    final Dependency material3 = project.dependencies().stream()
      .filter(d -> d.coordinates().identity().artifactId().equals("material3"))
      .tryFindFirst()
      .orElseThrow(() -> new AssertionError(
        "expected material3 among effective dependencies - the BOM import likely failed to resolve"
      ))
    ;
    assertThat(
      "material3's version should have been supplied by the imported BOM",
      material3.coordinates().version().toString().isBlank(),
      is(false)
    );
  }

}
