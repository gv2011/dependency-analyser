package com.github.gv2011.dependencyanalyser.mvnapi;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Redirects System.out to an in-memory buffer for the duration of a
 * try-with-resources block, restoring the original stream on close. Nothing
 * is ever written to disk, and nothing reaches the real console while this
 * is open.
 */
public final class CapturedSystemOut implements AutoCloseable {

  private final PrintStream previous;
  private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

  public CapturedSystemOut() {
    previous = System.out;
    System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
  }

  public String capturedText() {
    return buffer.toString(StandardCharsets.UTF_8);
  }

  @Override
  public void close() {
    System.setOut(previous);
  }

}
