package com.github.gv2011.dependencyanalyser.impl;

import static com.github.gv2011.util.BeanUtils.beanBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;

import com.github.gv2011.dependencyanalyser.api.Dependency;
import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;
import com.github.gv2011.dependencyanalyser.api.MavenScope;
import com.github.gv2011.dependencyanalyser.api.Project;
import com.github.gv2011.dependencyanalyser.api.VersionDeclaration;
import com.github.gv2011.util.icol.ICollections;
import com.github.gv2011.util.icol.ISet;
import com.github.gv2011.util.icol.Opt;

/**
 * Hand-written Project implementation - deliberately not Bean-proxied,
 * see Project's own javadoc for why. Backed by three independently
 * lazy, memoized (via {@link Lazy}) computations, not five: the
 * effective model build backs coordinates()/parent()/dependencies()
 * together (they're all read from the one Model), the interim model
 * build backs boms() alone (see ModelBuilderSketch for why that has to
 * be a separate build), and a raw text parse backs
 * getVersionDeclarations() alone. A caller that only ever calls
 * parent(), say, triggers the effective-model build and nothing else -
 * never the interim build, never the raw parse.
 *
 * <p>Thread-safe: each of the three computations is memoized via Lazy.
 */
final class LazyProject implements Project {

  private final String pomContent;
  private final Lazy<Model> effectiveModel = new Lazy<>(() -> buildModel(false));
  private final Lazy<Model> interimModel = new Lazy<>(() -> buildModel(true));
  private final Lazy<ISet<VersionDeclaration>> versionDeclarations =
    new Lazy<>(() -> RawVersionDeclarations.read(pomContent))
  ;

  LazyProject(final String pomContent) {
    this.pomContent = pomContent;
  }

  @Override
  public MavenCoordinates coordinates() {
    final Model m = effectiveModel.get();
    return Conversions.toMavenCoordinates(m.getGroupId(), m.getArtifactId(), m.getVersion(), packaging(m));
  }

  @Override
  public Opt<MavenCoordinates> parent() {
    final Parent p = effectiveModel.get().getParent();
    return Opt.ofNullable(p).map(pp ->
      Conversions.toMavenCoordinates(pp.getGroupId(), pp.getArtifactId(), pp.getVersion(), "pom")
    );
  }

  /**
   * From the interim model only - the effective model has already had
   * its scope=import entries replaced by what they point to. See
   * ModelBuilderSketch.buildInterimModel's own javadoc.
   */
  @Override
  public ISet<Dependency> boms() {
    final ISet.Builder<Dependency> result = ICollections.setBuilder();
    final Model m = interimModel.get();
    if(m.getDependencyManagement()!=null) {
      m.getDependencyManagement().getDependencies().forEach(d -> {
        if("import".equals(d.getScope())) {
          result.add(
            toDependency(d.getGroupId(), d.getArtifactId(), d.getVersion(), d.getType(), MavenScope.IMPORT)
          );
        }
      });
    }
    return result.build();
  }

  @Override
  public ISet<Dependency> dependencies() {
    final ISet.Builder<Dependency> result = ICollections.setBuilder();
    effectiveModel.get().getDependencies().forEach(d -> result.add(
      toDependency(d.getGroupId(), d.getArtifactId(), d.getVersion(), d.getType(), scopeOf(d.getScope()))
    ));
    return result.build();
  }

  @Override
  public ISet<VersionDeclaration> getVersionDeclarations() {
    return versionDeclarations.get();
  }

  /**
   * Each call writes pomContent to its own fresh, short-lived temp file
   * - ModelBuilderSketch needs a real File, and pomContent might have
   * come from a fetch (getProject(MavenCoordinates)), not an existing
   * one on disk. Deleted right after the build; nothing lingers.
   */
  private Model buildModel(final boolean interim) {
    final Path tempFile = createTempPomFile();
    try {
      return interim
        ? ModelBuilderSketch.buildInterimModel(tempFile.toFile(), new BridgingModelResolver())
        : ModelBuilderSketch.buildEffectiveModel(tempFile.toFile(), new BridgingModelResolver())
      ;
    }
    finally {
      deleteQuietly(tempFile);
    }
  }

  private Path createTempPomFile() {
    try {
      final Path file = Files.createTempFile("project-pom-", ".xml");
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

  /**
   * Effective dependencies always carry a real scope - modelNormalizer's
   * default-value injection supplies "compile" during model building
   * when none is stated. Defaulted here regardless, rather than assumed.
   */
  private static MavenScope scopeOf(final String scope) {
    return MavenScope.valueOf(Opt.ofNullable(scope).orElse("compile").toUpperCase(Locale.ROOT));
  }

  /**
   * Carries a hardcoded "jar" default in the generated model class
   * itself (from maven.mdo), present on a bare read - not something
   * that depends on model building/merging. Applied defensively here
   * regardless, rather than assumed.
   */
  private static String packaging(final Model m) {
    return Opt.ofNullable(m.getPackaging()).orElse("jar");
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

}
