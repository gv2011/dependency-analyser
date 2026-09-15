package com.github.gv2011.dependencyanalyser.impl;

import static com.github.gv2011.util.BeanUtils.beanBuilder;
import static com.github.gv2011.util.ex.Exceptions.notYetImplemented;

import java.io.IOException;
import java.io.StringReader;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.github.gv2011.dependencyanalyser.api.DependencyDeclaration;
import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;
import com.github.gv2011.dependencyanalyser.api.PomDependencyDeclarations;
import com.github.gv2011.util.icol.ICollections;
import com.github.gv2011.util.icol.ISet;
import com.github.gv2011.util.icol.Opt;

/**
 * Reads one pom.xml's own text - nothing more - via Maven's own raw model
 * reader ({@link MavenXpp3Reader}). "Raw" here means exactly what
 * {@link PomDependencyDeclarations} itself promises: no parent
 * inheritance, no BOM-import expansion, no interpolation. This class does
 * no fetching of its own; the caller supplies the pom.xml content,
 * however it was obtained (straight from disk for the leaf project,
 * {@code DependencyAnalyser.getPom(...)} for anything else).
 */
public final class PomDependencyDeclarationsParser {

  private PomDependencyDeclarationsParser(){}

  public static PomDependencyDeclarations parse(final String pomContent) {
    final Model model = readModel(pomContent);
    return beanBuilder(PomDependencyDeclarations.class)
      .set(PomDependencyDeclarations::groupId).to(Opt.ofNullable(model.getGroupId()))
      .set(PomDependencyDeclarations::artifactId).to(model.getArtifactId())
      .set(PomDependencyDeclarations::version).to(
        Opt.ofNullable(model.getVersion()).map(VersionImpl::parse)
      )
      .set(PomDependencyDeclarations::parent).to(parent(model))
      .set(PomDependencyDeclarations::dependencyDeclarations).to(dependencyDeclarations(model))
      .build()
    ;
  }

  private static Model readModel(final String pomContent) {
    try {
      return new MavenXpp3Reader().read(new StringReader(pomContent));
    }
    catch(final IOException | XmlPullParserException e) {
      throw new IllegalArgumentException("Could not parse pom content as a Maven model", e);
    }
  }

  private static Opt<MavenCoordinates> parent(final Model model) {
    final Parent parent = model.getParent();
    if(parent==null) {
      return Opt.empty();
    }
    if(parent.getVersion()==null) {
      // Since Maven 3.5 (MNG-624) a <parent> may omit <version> and have
      // it inferred from <relativePath> instead - not resolvable from
      // this pom's own text alone.
      return notYetImplemented(
        "parent version omitted (inferred via relativePath, MNG-624) for "
        + parent.getGroupId() + ":" + parent.getArtifactId()
      );
    }
    return Opt.of(
      Conversions.toMavenCoordinates(
        parent.getGroupId(), parent.getArtifactId(), parent.getVersion(), "pom"
      )
    );
  }

  private static ISet<DependencyDeclaration> dependencyDeclarations(final Model model) {
    final ISet.Builder<DependencyDeclaration> result = ICollections.setBuilder();
    model.getDependencies().forEach(d -> addIfVersioned(result, d, false));
    final DependencyManagement management = model.getDependencyManagement();
    if(management!=null) {
      management.getDependencies().forEach(d -> addIfVersioned(result, d, true));
    }
    return result.build();
  }

  private static void addIfVersioned(
    final ISet.Builder<DependencyDeclaration> result, final Dependency d, final boolean managed
  ) {
    if(d.getVersion()==null) {
      return; // no <version> of its own - not represented, per DependencyDeclaration's own contract
    }
    final boolean bomImport =
      managed && "import".equals(d.getScope()) && "pom".equals(type(d))
    ;
    result.add(
      beanBuilder(DependencyDeclaration.class)
        .set(DependencyDeclaration::coordinates).to(
          Conversions.toMavenCoordinates(d.getGroupId(), d.getArtifactId(), d.getVersion(), type(d))
        )
        .set(DependencyDeclaration::managed).to(managed)
        .set(DependencyDeclaration::isBomImport).to(bomImport)
        .build()
    );
  }

  /**
   * Dependency.getType() carries a hardcoded "jar" default in the
   * generated model class itself (from maven.mdo), present on a bare
   * read - not something that depends on model building/merging.
   * Applied defensively here regardless, rather than assumed.
   */
  private static String type(final Dependency d) {
    return Opt.ofNullable(d.getType()).orElse("jar");
  }

}
