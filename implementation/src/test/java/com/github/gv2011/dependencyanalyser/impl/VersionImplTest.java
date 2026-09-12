package com.github.gv2011.dependencyanalyser.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.github.gv2011.dependencyanalyser.api.Version;
import com.github.gv2011.util.ex.NotYetImplementedException;

class VersionImplTest {

  @Test
  void testMajor() {
    //TODO
    final Version version = VersionImpl.parse("2.3.4");
    assertThrows(NotYetImplementedException.class, version::major);
  }

}
