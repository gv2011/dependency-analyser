package com.github.gv2011.dependencyanalyser.impl;

import static com.github.gv2011.util.BeanUtils.beanBuilder;
import static com.github.gv2011.util.ex.Exceptions.notYetImplemented;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Function;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.slf4j.Logger;

import com.github.gv2011.dependencyanalyser.api.Dependency;
import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;
import com.github.gv2011.dependencyanalyser.api.MavenScope;
import com.github.gv2011.dependencyanalyser.api.Project;
import com.github.gv2011.dependencyanalyser.api.Repository;
import com.github.gv2011.dependencyanalyser.api.VersionDeclaration;
import com.github.gv2011.util.icol.ICollections;
import com.github.gv2011.util.icol.IList;
import com.github.gv2011.util.icol.ISet;
import com.github.gv2011.util.icol.Opt;

/**
 * Hand-written Project implementation - deliberately not Bean-proxied,
 * see Project's own javadoc for why. Backed by three independently
 * lazy, memoized (via {@link Lazy}) computations, not five: the
 * effective model build backs coordinates()/additionalRepositories()/
 * parent()/dependencies() together (they're all read from the one
 * build - additionalRepositories() specifically from the resolver that
 * build used, not the Model itself), the interim model build backs
 * boms() alone (see ModelBuilderSketch for why that has to be a
 * separate build), and a raw text parse backs getVersionDeclarations()
 * alone. A caller that only ever calls parent(), say, triggers the
 * effective-model build and nothing else - never the interim build,
 * never the raw parse.
 *
 * <p>Thread-safe: each of the three computations is memoized via Lazy.
 */
final class LazyProject implements Project {

  private static final Logger LOG = getLogger(LazyProject.class);

  private final String pomContent;
  private final IList<org.apache.maven.model.Repository> seedRepositories;
  private final Lazy<MavenCoordinates> lightweightCoordinates;
  private final Lazy<EffectiveBuild> effectiveBuild;
  private final Lazy<Model> interimModel;
  private final Lazy<ISet<VersionDeclaration>> versionDeclarations;

  /**
   * @param additionalRepositories seeded into the model build - repositories
   *   already known before this project's own text is even read (typically:
   *   what the referring project in an ongoing walk had already
   *   accumulated). See DependencyAnalyser.getProject's own javadoc.
   */
  LazyProject(final String pomContent, final IList<Repository> additionalRepositories) {
    this.pomContent = pomContent;
    // Assigned here, not as field initializers: field initializers run
    // top-to-bottom before the constructor body, so a lambda in an
    // earlier one referencing pomContent (assigned only below) isn't
    // provably initialized yet at that point - a real compile error,
    // not a style choice.
    this.seedRepositories = Conversions.toMavenRepositories(additionalRepositories);
    this.lightweightCoordinates = new Lazy<>(this::computeLightweightCoordinates);
    this.effectiveBuild = new Lazy<>(this::buildEffective);
    this.interimModel = new Lazy<>(this::buildInterim);
    this.versionDeclarations = new Lazy<>(() -> RawVersionDeclarations.read(this.pomContent));
  }

  /**
   * @param repositories what the resolver used for this build ended up
   *   with - the seed plus whatever this project's own text (and its
   *   parent chain) contributed. Not the same thing as reading
   *   Model.getRepositories() directly, which would only show this one
   *   project's own <repositories> element, not the accumulated result.
   */
  private record EffectiveBuild(Model model, IList<org.apache.maven.model.Repository> repositories) {}

  @Override
  public MavenCoordinates coordinates() {
    return lightweightCoordinates.get();
  }

  @Override
  public String toString() {
    return coordinates().toString();
  }

  @Override
  public IList<Repository> additionalRepositories() {
    return Conversions.toRepositories(effectiveBuild.get().repositories());
  }

  @Override
  public Opt<MavenCoordinates> parent() {
    final Parent p = effectiveBuild.get().model().getParent();
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
    effectiveBuild.get().model().getDependencies().forEach(d -> result.add(
      toDependency(d.getGroupId(), d.getArtifactId(), d.getVersion(), d.getType(), scopeOf(d.getScope()))
    ));
    return result.build();
  }

  @Override
  public ISet<VersionDeclaration> getVersionDeclarations() {
    return versionDeclarations.get();
  }

  /**
   * Reads groupId/artifactId/version from this project's own raw text
   * alone - no full model build, no network, no temp file for a Maven
   * invocation. artifactId is always stated directly (Maven doesn't
   * allow inheriting it). groupId/version, if not stated directly, are
   * inherited from the parent - but the parent's own coordinates are
   * already sitting right there in this pom's own {@code <parent>}
   * element (Maven requires that reference to state the parent's real
   * coordinates), so no fetch of the parent's own pom is needed either.
   * Only fails loudly when even that isn't available: no parent at all,
   * or the parent's own version was itself omitted (inferred via
   * relativePath, MNG-624) - e.g. PomFetcher-based resolution would be
   * needed for that, out of scope for this lightweight path.
   */
  private MavenCoordinates computeLightweightCoordinates() {
    final Model raw = RawPom.read(pomContent);
    final String groupId = Opt.ofNullable(raw.getGroupId()).orElseGet(() -> parentField(raw, Parent::getGroupId));
    final String version = Opt.ofNullable(raw.getVersion()).orElseGet(() -> parentField(raw, Parent::getVersion));
    return Conversions.toMavenCoordinates(groupId, raw.getArtifactId(), version, packaging(raw));
  }

  private static String parentField(final Model raw, final Function<Parent, String> field) {
    final Parent parent = raw.getParent();
    if(parent==null) {
      return notYetImplemented(
        "groupId/version not stated, and no <parent> to inherit them from, for " + raw.getArtifactId()
      );
    }
    final String value = field.apply(parent);
    if(value==null) {
      // Since Maven 3.5 (MNG-624) a <parent> may omit <version> and have
      // it inferred from <relativePath> instead - not resolvable from
      // this pom's own text alone.
      return notYetImplemented(
        "parent version omitted (inferred via relativePath, MNG-624) for "
        + parent.getGroupId() + ":" + parent.getArtifactId()
      );
    }
    return value;
  }

  private EffectiveBuild buildEffective() {
    final Path tempFile = createTempPomFile();
    try {
      final BridgingModelResolver resolver = new BridgingModelResolver(seedRepositories);
      final Model model = ModelBuilderSketch.buildEffectiveModel(tempFile.toFile(), resolver);
      LOG.info("Built effective model of {}", Conversions.toMavenCoordinates(model));
      return new EffectiveBuild(model, resolver.repositories());
    }
    finally {
      deleteQuietly(tempFile);
    }
  }

  private Model buildInterim() {
    final Path tempFile = createTempPomFile();
    try {
      return ModelBuilderSketch.buildInterimModel(tempFile.toFile(), new BridgingModelResolver(seedRepositories));
    }
    finally {
      deleteQuietly(tempFile);
    }
  }

  /**
   * Each call writes pomContent to its own fresh, short-lived temp file
   * - ModelBuilderSketch needs a real File, and pomContent might have
   * come from a fetch (getProject(MavenCoordinates)), not an existing
   * one on disk. Deleted right after the build; nothing lingers.
   */
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
  static String packaging(final Model m) {
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
