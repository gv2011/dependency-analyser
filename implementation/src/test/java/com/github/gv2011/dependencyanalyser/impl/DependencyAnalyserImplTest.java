package com.github.gv2011.dependencyanalyser.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.github.gv2011.dependencyanalyser.api.DependencyAnalyser;

class DependencyAnalyserImplTest {

  @Test
  void loadServiceTest() {
    assertNotNull(DependencyAnalyser.instance());
  }

}
