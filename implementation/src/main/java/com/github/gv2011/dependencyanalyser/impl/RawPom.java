package com.github.gv2011.dependencyanalyser.impl;

import java.io.IOException;
import java.io.StringReader;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Reads one project's own pom.xml text - nothing more - via Maven's own
 * raw model reader ({@link MavenXpp3Reader}): no parent inheritance, no
 * BOM-import expansion, no interpolation. Shared by whichever of this
 * package's classes need to look at one project's own, unmerged text.
 */
final class RawPom {

  private RawPom(){}

  static Model read(final String pomContent) {
    try {
      return new MavenXpp3Reader().read(new StringReader(pomContent));
    }
    catch(final IOException | XmlPullParserException e) {
      throw new IllegalArgumentException("Could not parse pom content as a Maven model", e);
    }
  }

}
