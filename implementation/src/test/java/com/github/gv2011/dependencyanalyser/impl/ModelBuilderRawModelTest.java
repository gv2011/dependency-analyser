package com.github.gv2011.dependencyanalyser.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Confirms, against real Maven model-building (not just source reading),
 * that ModelBuildingRequest.setTwoPhaseBuilding(true) gives an interim
 * model that is interpolated (property placeholders resolved) but has
 * NOT yet had its dependencyManagement imports merged - the one state
 * that shows a BOM import as itself, with a real, usable version.
 *
 * <p>parent is found via relativePath, so no ModelResolver capable of
 * fetching anything is needed here - the imported BOM (not a real
 * artifact) is never actually resolved, since that step (phase 2) is
 * exactly what this test skips.
 */
class ModelBuilderRawModelTest {

  @Test
  void interimModelIsInterpolatedButImportNotYetMerged(@TempDir final Path dir) throws IOException {
    Files.writeString(dir.resolve("parent-pom.xml"), """
      <?xml version="1.0" encoding="UTF-8"?>
      <project xmlns="http://maven.apache.org/POM/4.0.0">
        <modelVersion>4.0.0</modelVersion>
        <groupId>com.example</groupId>
        <artifactId>parent</artifactId>
        <version>1.0</version>
        <packaging>pom</packaging>
        <properties>
          <bom.version>9.9.9</bom.version>
        </properties>
      </project>
      """, StandardCharsets.UTF_8);

    Files.writeString(dir.resolve("child-pom.xml"), """
      <?xml version="1.0" encoding="UTF-8"?>
      <project xmlns="http://maven.apache.org/POM/4.0.0">
        <modelVersion>4.0.0</modelVersion>
        <parent>
          <groupId>com.example</groupId>
          <artifactId>parent</artifactId>
          <version>1.0</version>
          <relativePath>parent-pom.xml</relativePath>
        </parent>
        <artifactId>child</artifactId>
        <dependencyManagement>
          <dependencies>
            <dependency>
              <groupId>com.example</groupId>
              <artifactId>some-bom</artifactId>
              <version>${bom.version}</version>
              <type>pom</type>
              <scope>import</scope>
            </dependency>
          </dependencies>
        </dependencyManagement>
      </project>
      """, StandardCharsets.UTF_8);

    final Model interim = ModelBuilderSketch.buildInterimModel(dir.resolve("child-pom.xml").toFile(), null);

    final List<Dependency> managed = interim.getDependencyManagement().getDependencies();
    assertThat("expected exactly the one BOM entry, unmerged", managed.size(), is(1));

    final Dependency bom = managed.get(0);
    assertThat(bom.getArtifactId(), is("some-bom"));
    assertThat("scope=import must still be present, not yet replaced", bom.getScope(), is("import"));
    assertThat("property must already be interpolated to a real version", bom.getVersion(), is("9.9.9"));
  }

}
