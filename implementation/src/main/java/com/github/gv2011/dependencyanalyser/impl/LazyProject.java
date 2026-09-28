package com.github.gv2011.dependencyanalyser.impl;

import static com.github.gv2011.dependencyanalyser.impl.Conversions.toDependency;
import static com.github.gv2011.dependencyanalyser.impl.Conversions.toMavenCoordinates;
import static com.github.gv2011.dependencyanalyser.impl.Conversions.toVersionDeclaration;
import static com.github.gv2011.dependencyanalyser.impl.VersionImpl.parse;
import static com.github.gv2011.util.Verify.notNull;
import static com.github.gv2011.util.Verify.verifyEqual;
import static com.github.gv2011.util.ex.Exceptions.call;
import static com.github.gv2011.util.ex.Exceptions.notYetImplemented;
import static com.github.gv2011.util.icol.ICollections.toISet;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.slf4j.Logger;

import com.github.gv2011.dependencyanalyser.api.Dependency;
import com.github.gv2011.dependencyanalyser.api.DependencySection;
import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;
import com.github.gv2011.dependencyanalyser.api.MavenScope;
import com.github.gv2011.dependencyanalyser.api.Project;
import com.github.gv2011.dependencyanalyser.api.Repository;
import com.github.gv2011.dependencyanalyser.api.Version;
import com.github.gv2011.dependencyanalyser.api.VersionDeclaration;
import com.github.gv2011.util.icol.IList;
import com.github.gv2011.util.icol.ISet;
import com.github.gv2011.util.icol.Opt;


final class LazyProject implements Project {

  private static final Logger LOG = getLogger(LazyProject.class);

  private final String pomContent;
  private final Model rawModel;

  /**
   * Repositories used to obtain this project.
   */
  private final IList<Repository> seedRepositories;

  private final MavenCoordinates coordinates;
  private final ISet<VersionDeclaration> versionDeclarations;

  private final Lazy<EffectiveBuild> effectiveBuild = new Lazy<>(this::buildEffective);
  private final Lazy<Model> interimModel = new Lazy<>(this::buildInterim);


  LazyProject(final String pomContent, final IList<Repository> seedRepositories) {
    this.pomContent = pomContent;
    this.seedRepositories = seedRepositories;
    rawModel = call(()->new MavenXpp3Reader().read(new StringReader(pomContent)));
    coordinates = getCoordinates(rawModel);
    versionDeclarations = getVersionDeclarations(rawModel);
  }

  LazyProject(final MavenCoordinates coordinates, final IList<Repository> seedRepositories) {
    this.coordinates = coordinates;
    this.seedRepositories = seedRepositories;
    this.pomContent = new PomFetcher().fetchPomContent(coordinates, seedRepositories);
    this.rawModel = call(()->new MavenXpp3Reader().read(new StringReader(pomContent)));
    verifyEqual(getCoordinates(rawModel), coordinates);
    versionDeclarations = getVersionDeclarations(rawModel);
  }


  /**
   * @param repositories what the resolver used for this build ended up
   *   with - the seed plus whatever this project's own text (and its
   *   parent chain) contributed. Not the same thing as reading
   *   Model.getRepositories() directly, which would only show this one
   *   project's own <repositories> element, not the accumulated result.
   */
  private record EffectiveBuild(Model model, IList<Repository> repositories) {}


  @Override
  public MavenCoordinates coordinates() {
    return coordinates;
  }

  @Override
  public String toString() {
    return coordinates().toString();
  }

  @Override
  public IList<Repository> additionalRepositories() {
    return effectiveBuild.get().repositories();
  }

  @Override
  public Opt<MavenCoordinates> parent() {
    return getParent(rawModel);
  }

  private static Opt<MavenCoordinates> getParent(final Model rawModel) {
    return Opt.ofNullable(rawModel.getParent())
      .map(p->toMavenCoordinates(
        notNull(p.getGroupId()),
        notNull(p.getArtifactId()),
        parse(Opt
          .ofNullable(p.getVersion())
          .orElseGet(()->notYetImplemented(
            "parent version omitted (inferred via relativePath, MNG-624) for "+
            p.getGroupId() + ":" + p.getArtifactId()
          ))
        ),
        "pom"
      ))
    ;
  }

  @Override
  public ISet<Dependency> boms() {
    //Boms are not available in the effective model, because there they are resolved already.
    //We use an interim model where they are still available.
    return Opt
      .ofNullable(interimModel.get().getDependencyManagement()).stream()
      .flatMap(dm->dm.getDependencies().stream())
      .map(d->toDependency(d))
      .filter(d->d.scope().equals(MavenScope.IMPORT))
      .toISet()
    ;
  }

  @Override
  public ISet<Dependency> dependencies() {
    return effectiveBuild.get().model().getDependencies().stream()
      .map(Conversions::toDependency)
      .collect(toISet())
    ;
  }

  @Override
  public ISet<VersionDeclaration> getVersionDeclarations() {
    return versionDeclarations;
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
  private static MavenCoordinates getCoordinates(final Model raw) {
    final Opt<MavenCoordinates> parent = getParent(raw);
    final String groupId = Opt.ofNullable(raw.getGroupId()).orElseGet(()->parent.get().identity().groupId());
    final Version version = Opt
      .ofNullable(raw.getVersion()).map(VersionImpl::parse)
      .orElseGet(()->parent.get().version())
    ;
    return toMavenCoordinates(groupId, raw.getArtifactId(), version, "pom");
  }



  private EffectiveBuild buildEffective() {
    final Instant start = Instant.now();
    final Path tempFile = createTempPomFile();
    try {
      final BridgingModelResolver resolver = new BridgingModelResolver(seedRepositories);
      final Model model = ModelBuilderSketch.buildEffectiveModel(tempFile.toFile(), resolver);
      LOG.info("Built effective model of {}, took {}.", toMavenCoordinates(model), Duration.between(start, Instant.now()));
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

  private static ISet<VersionDeclaration> getVersionDeclarations(final Model raw) {
    return
      Stream.concat(
        ( raw.getDependencies().stream()
          .filter(d->d.getVersion()!=null)
          .map(d->toVersionDeclaration(d, DependencySection.DEPENDENCIES))
        ),
        ( Opt.ofNullable(raw.getDependencyManagement()).stream()
          .flatMap(dm->dm.getDependencies().stream())
          .filter(d->d.getVersion()!=null)
          .map(d->toVersionDeclaration(d, DependencySection.DEPENDENCY_MANAGEMENT))
        )
      )
      .collect(toISet())
    ;
  }

}
