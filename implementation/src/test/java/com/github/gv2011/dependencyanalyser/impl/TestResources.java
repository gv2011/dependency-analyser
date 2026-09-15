package com.github.gv2011.dependencyanalyser.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Test-support only: reads a classpath test resource (src/test/resources)
 * as a String.
 */
final class TestResources {

  private TestResources(){}

  static String read(final String path) {
    try(InputStream in = TestResources.class.getResourceAsStream(path)) {
      if(in==null) {
        throw new IllegalStateException("Test resource not found: " + path);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    catch(final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

}
