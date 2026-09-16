package com.github.gv2011.dependencyanalyser.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * NOT YET RUN (no compiler/JVM-with-Maven-deps available while writing
 * this). Purpose: answer the open question from ModelBuilderSketch - does
 * ModelBuildingResult's raw model show a scope=import dependencyManagement
 * entry with its version already interpolated (property resolved), i.e.
 * a state that is neither pure-raw-text nor fully-effective?
 *
 * child's parent is found via relativePath (no ModelResolver needed for
 * that lookup) - avoids needing a real repository/resolver just to test
 * this one question. The BOM being imported is NOT a real artifact
 * anywhere, so the build may fail once/if it tries to actually resolve
 * that import - which raw-model method to call (see TODO below) and
 * whether that failure happens before or after raw-model access is
 * itself part of what this test should reveal.
 *
 * FIRST RUN: just read the console output, don't trust the (currently
 * absent) assertions - the exact API here (getRawModel() vs
 * getRawModel(String), whether build() throws before returning a usable
 * result at all) is unconfirmed. Once the actual output is seen, replace
 * the printlns with real assertions.
 */
class ModelBuilderRawModelTest {

  @Test
  void rawModelInterpolatedButImportNotYetMerged(@TempDir final Path dir) throws IOException {
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

    final DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
    request.setPomFile(dir.resolve("child-pom.xml").toFile());
    request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
    request.setProcessPlugins(false);
    // No ModelResolver set - relativePath resolves the parent without
    // one. If the build needs a resolver once it tries to actually
    // process the (non-existent) imported BOM, that's expected to
    // surface as a failure at some point - see try/catch below.

    ModelBuildingResult result = null;
    try {
      result = new DefaultModelBuilderFactory().newInstance().build(request);
    }
    catch(final Exception e) {
      // Don't swallow - print how far it got. A failure here is itself
      // informative (e.g. "requires a ModelResolver" tells us raw-model
      // access needs the full pipeline to at least attempt resolution).
      e.printStackTrace();
    }

    if(result!=null) {
      // TODO confirm exact method: getRawModel() vs getRawModel(String modelId)
      final Model raw = result.getRawModel();
      System.out.println("=== raw model dependencyManagement ===");
      if(raw.getDependencyManagement()==null) {
        System.out.println("(null - dependencyManagement not present on raw model)");
      }
      else {
        raw.getDependencyManagement().getDependencies().forEach(d ->
          System.out.println(
            d.getGroupId() + ":" + d.getArtifactId() + ":" + d.getVersion() + " scope=" + d.getScope()
          )
        );
      }
    }

    // Intentionally no assertions yet - run this, read the console
    // output above, THEN write real assertions against what actually
    // comes back (expected if the hypothesis holds: one dependency,
    // version "9.9.9" (interpolated, not "${bom.version}"), scope
    // "import" (not yet replaced)).
  }

}
