package com.github.gv2011.dependencyanalyser.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Not part of the tool itself - see the module's pom.xml for why this
 * project exists.
 */
public final class Example {

  private static final Logger LOG = LoggerFactory.getLogger(Example.class);

  private Example(){}

  public static void main(final String[] args) {
    LOG.info("This is the dependency-analyser-example project.");
  }

}
