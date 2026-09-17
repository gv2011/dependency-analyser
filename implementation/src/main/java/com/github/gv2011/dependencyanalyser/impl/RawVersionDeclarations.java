package com.github.gv2011.dependencyanalyser.impl;

import static com.github.gv2011.util.BeanUtils.beanBuilder;

import java.io.IOException;
import java.io.StringReader;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.github.gv2011.dependencyanalyser.api.DependencySection;
import com.github.gv2011.dependencyanalyser.api.VersionDeclaration;
import com.github.gv2011.util.icol.ICollections;
import com.github.gv2011.util.icol.ISet;
import com.github.gv2011.util.icol.Opt;

/**
 * Reads one project's own pom.xml text - nothing more - via Maven's own
 * raw model reader ({@link MavenXpp3Reader}): no parent inheritance, no
 * BOM-import expansion, no interpolation. This is deliberately NOT the
 * same source as {@link ModelBuilderSketch} - Project.getVersionDeclarations()
 * is the one field on Project that stays unmerged by contract, so it
 * needs the raw reading, not the effective/interim model.
 *
 * <p>Because there's no interpolation here, a version given as a property
 * placeholder (e.g. {@code ${revision}}) comes back as that literal
 * string, not a resolved value - same exposure VersionImpl.parse has for
 * any raw-parsed version, not specific to this class.
 */
final class RawVersionDeclarations {

  private RawVersionDeclarations(){}

  static ISet<VersionDeclaration> read(final String pomContent) {
    final Model model = readModel(pomContent);
    final ISet.Builder<VersionDeclaration> result = ICollections.setBuilder();
    model.getDependencies().forEach(d -> addIfVersioned(result, d, DependencySection.DEPENDENCIES));
    final DependencyManagement management = model.getDependencyManagement();
    if(management!=null) {
      management.getDependencies().forEach(d -> addIfVersioned(result, d, DependencySection.DEPENDENCY_MANAGEMENT));
    }
    return result.build();
  }

  private static Model readModel(final String pomContent) {
    try {
      return new MavenXpp3Reader().read(new StringReader(pomContent));
    }
    catch(final IOException | XmlPullParserException e) {
      throw new IllegalArgumentException("Could not parse pom content as a Maven model", e);
    }
  }

  private static void addIfVersioned(
    final ISet.Builder<VersionDeclaration> result, final Dependency d, final DependencySection section
  ) {
    if(d.getVersion()==null) {
      return; // no <version> of its own - not represented, per VersionDeclaration's own contract
    }
    result.add(
      beanBuilder(VersionDeclaration.class)
        .set(VersionDeclaration::artifact).to(
          Conversions.toArtifactIdentity(d.getGroupId(), d.getArtifactId(), Opt.ofNullable(d.getClassifier()), type(d))
        )
        .set(VersionDeclaration::version).to(VersionImpl.parse(d.getVersion()))
        .set(VersionDeclaration::section).to(section)
        .build()
    );
  }

  /**
   * Carries a hardcoded "jar" default in the generated model class
   * itself (from maven.mdo), present on a bare read - not something
   * that depends on model building/merging. Applied defensively here
   * regardless, rather than assumed.
   */
  private static String type(final Dependency d) {
    return Opt.ofNullable(d.getType()).orElse("jar");
  }

}
