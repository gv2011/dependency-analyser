package com.github.gv2011.dependencyanalyser.impl;

import static com.github.gv2011.util.BeanUtils.beanBuilder;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;

import com.github.gv2011.dependencyanalyser.api.Dependency;
import com.github.gv2011.dependencyanalyser.api.DependencyAnalyser;
import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;
import com.github.gv2011.dependencyanalyser.api.MavenScope;
import com.github.gv2011.dependencyanalyser.api.Project;
import com.github.gv2011.dependencyanalyser.api.Version;
import com.github.gv2011.util.icol.ICollections;
import com.github.gv2011.util.icol.ISet;
import com.github.gv2011.util.icol.Opt;

public class DependencyAnalyserImpl implements DependencyAnalyser {

  @Override
  public Project getProject(final Path projectDirectory) {
    return buildProject(projectDirectory.resolve("pom.xml").toFile());
  }

  @Override
  public Project getProject(final MavenCoordinates projectCoordinates) {
    final String pomContent = PomFetcher.fetchPomContent(projectCoordinates);
    final Path tempFile = createTempFile(pomContent);
    try {
      return buildProject(tempFile.toFile());
    }
    finally {
      deleteQuietly(tempFile);
    }
  }

  /**
   * Two separate model builds, deliberately: the effective one (for
   * coordinates()/parent()/dependencies() - dependencyManagement imports
   * already merged in, since that's what "effective" means) and the
   * interim one (for boms() - interpolated, but stopped before import
   * merging, since that step is exactly what erases the thing boms()
   * needs to see). See ModelBuilderSketch for why both exist.
   */
  private Project buildProject(final File pomFile) {
    final Model effective = ModelBuilderSketch.buildEffectiveModel(pomFile, new BridgingModelResolver());
    final Model interim = ModelBuilderSketch.buildInterimModel(pomFile, new BridgingModelResolver());
    final String pomContent;
    try {
      pomContent = Files.readString(pomFile.toPath(), StandardCharsets.UTF_8);
    }
    catch(final IOException e) {
      throw new UncheckedIOException(e);
    }
    return beanBuilder(Project.class)
      .set(Project::coordinates).to(Conversions.toMavenCoordinates(
        effective.getGroupId(), effective.getArtifactId(), effective.getVersion(), effective.getPackaging()
      ))
      .set(Project::parent).to(parentOf(effective))
      .set(Project::boms).to(bomsOf(interim))
      .set(Project::dependencies).to(dependenciesOf(effective))
      .set(Project::getVersionDeclarations).to(RawVersionDeclarations.read(pomContent))
      .build()
    ;
  }

  private static Opt<MavenCoordinates> parentOf(final Model effective) {
    final Parent parent = effective.getParent();
    return Opt.ofNullable(parent).map(p ->
      Conversions.toMavenCoordinates(p.getGroupId(), p.getArtifactId(), p.getVersion(), "pom")
    );
  }

  /**
   * From the INTERIM model only - the effective model has already had
   * its scope=import entries replaced by what they point to, per
   * Maven's own documented dependencyManagement-import behavior. See
   * ModelBuilderSketch.buildInterimModel's own javadoc.
   */
  private static ISet<Dependency> bomsOf(final Model interim) {
    final ISet.Builder<Dependency> result = ICollections.setBuilder();
    if(interim.getDependencyManagement()!=null) {
      interim.getDependencyManagement().getDependencies().forEach(d -> {
        if("import".equals(d.getScope())) {
          result.add(toDependency(d.getGroupId(), d.getArtifactId(), d.getVersion(), d.getType(), MavenScope.IMPORT));
        }
      });
    }
    return result.build();
  }

  private static ISet<Dependency> dependenciesOf(final Model effective) {
    final ISet.Builder<Dependency> result = ICollections.setBuilder();
    effective.getDependencies().forEach(d -> result.add(
      toDependency(d.getGroupId(), d.getArtifactId(), d.getVersion(), d.getType(), scopeOf(d.getScope()))
    ));
    return result.build();
  }

  /**
   * Effective dependencies always carry a real scope - modelNormalizer's
   * default-value injection supplies "compile" during model building
   * when none is stated. Defaulted here regardless, rather than assumed.
   */
  private static MavenScope scopeOf(final String scope) {
    return MavenScope.valueOf(Opt.ofNullable(scope).orElse("compile").toUpperCase(Locale.ROOT));
  }

  private static Dependency toDependency(
    final String groupId, final String artifactId, final String version, final String type, final MavenScope scope
  ) {
    return beanBuilder(Dependency.class)
      .set(Dependency::coordinates).to(
        Conversions.toMavenCoordinates(groupId, artifactId, version, Opt.ofNullable(type).orElse("jar"))
      )
      .set(Dependency::scope).to(scope)
      .build()
    ;
  }

  private static Path createTempFile(final String pomContent) {
    try {
      final Path file = Files.createTempFile("fetched-pom-", ".xml");
      Files.writeString(file, pomContent, StandardCharsets.UTF_8);
      return file;
    }
    catch(final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void deleteQuietly(final Path file) {
    try {
      Files.deleteIfExists(file);
    }
    catch(final IOException e) {
      // best-effort cleanup only - not worth failing the whole call for
    }
  }

  @Override
  public Version parseVersion(final String version) {
    return VersionImpl.parse(version);
  }

}
