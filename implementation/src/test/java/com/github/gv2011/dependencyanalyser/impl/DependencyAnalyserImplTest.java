package com.github.gv2011.dependencyanalyser.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import com.github.gv2011.dependencyanalyser.api.Classpath;
import com.github.gv2011.dependencyanalyser.api.DependencyAnalyser;
import com.github.gv2011.dependencyanalyser.api.ResolvedDependency;
import com.github.gv2011.util.BeanUtils;
import com.github.gv2011.util.icol.ISet;

class DependencyAnalyserImplTest {

  @Test
  void loadServiceTest() {
    assertNotNull(DependencyAnalyser.instance());
  }

  @Test
  void testResolvedDependencies() {
    final ISet<ResolvedDependency> deps =
      new DependencyAnalyserImpl().resolvedDependencies(Paths.get("."), Classpath.MAIN)
    ;
    assertThat(deps, hasSize(1));
    final String json = BeanUtils.typeRegistry().beanType(ResolvedDependency.class).toJson(deps.single()).serialize();
    assertThat(
      json.trim(),
      is(
        """
        {
          "scope": "COMPILE",
          "version": "1.2.3",
          "identity": {
            "artifactId": "artifact-1",
            "groupId": "some.group",
            "type": "jar"
          }
        }
        """.trim()
      )
    );
  }

}
