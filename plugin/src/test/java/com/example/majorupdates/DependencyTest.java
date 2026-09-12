package com.example.majorupdates;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import com.example.majorupdates.Dependency.DefaultDependency;
import com.example.majorupdates.Dependency.DependencyKind;
import com.google.gson.GsonBuilder;

class DependencyTest {

  static Dependency createTestDependency(){
    final org.apache.maven.model.Dependency mavenDependency;
    {
      mavenDependency = new org.apache.maven.model.Dependency();
      mavenDependency.setGroupId("com.fasterxml.jackson.core");
      mavenDependency.setArtifactId("jackson-databind");
      mavenDependency.setVersion("2.15.2");
    }
    return new DefaultDependency(DependencyKind.DEPENDENCY, mavenDependency);
  }

  @Test
  void testToJson() {
    final String json = new GsonBuilder().setPrettyPrinting().create().toJson(createTestDependency()).trim();
    assertThat(
      json,
      is(
        """
        {
          "kind": "DEPENDENCY",
          "mavenDependency": {
            "groupId": "com.fasterxml.jackson.core",
            "artifactId": "jackson-databind",
            "version": "2.15.2",
            "type": "jar"
          }
        }
        """.trim()
      )
    );
  }

}
