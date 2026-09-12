package com.example.majorupdates;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.mojo.versions.api.ArtifactVersions;
import org.codehaus.mojo.versions.api.DefaultVersionsHelper;
import org.codehaus.mojo.versions.api.PomHelper;
import org.codehaus.mojo.versions.api.Segment;
import org.codehaus.mojo.versions.api.VersionRetrievalException;
import org.codehaus.mojo.versions.api.VersionsHelper;
import org.codehaus.mojo.versions.rule.RuleService;
import org.codehaus.mojo.versions.rule.RulesServiceBuilder;
import org.codehaus.mojo.versions.utils.ArtifactFactory;
import org.codehaus.mojo.versions.utils.VersionsExpressionEvaluator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.eclipse.aether.RepositorySystem;

import com.google.common.collect.ImmutableList;

/**
 * Framework-facing shell. Its only job is to absorb Maven/Plexus/Guice's injection and
 * parameter-binding quirks -- documented, sometimes badly, in README.md -- so that
 * {@link MajorUpdatesReportGenerator}, the actual logic, never has to know they exist.
 *
 * <p>Milestone/pre-release filtering does NOT happen here. It used to, via a {@code rulesUri}
 * parameter feeding {@code RuleService}'s own filtering -- removed, since that mechanism was
 * mostly unused overhead (only ever exercised {@code withIgnoredVersions}) and the actual
 * comparison logic it fed into is business logic, not framework plumbing. It now lives in
 * {@code VersionsHelperAdapter} below, via the real {@code ArtifactVersions.filter(...)} and
 * {@code getNewestUpdateWithinSegment(...)} -- moved there, out of
 * {@link MajorUpdatesReportGenerator}, specifically so that logic keeps using the real,
 * already-tested versions-common comparison rather than a reimplementation of it; see
 * {@link VersionsHelperFacade}'s own javadoc for the full reasoning.
 * {@code RuleService} is still built here -- {@link DefaultVersionsHelper.Builder} requires a
 * non-null instance -- but does no filtering work of its own anymore.
 *
 * <p>Deliberately NOT held to this project's usual rules. Field-driven {@code @Parameter}
 * binding forces plain {@code @Nullable} fields here instead of {@code Optional} (confirmed: an
 * {@code Optional}-typed backing field breaks Plexus's configuration converter for any
 * non-expression parameter -- see the prior discussion for the exact failure).
 * {@code VersionsHelper}, {@code RuleService}, and {@code PomHelper} are built manually via their
 * own builders because they are not, despite appearances, injectable Sisu components -- each
 * failure was confirmed individually via a runtime {@code ProvisionException} before switching
 * to manual construction, not assumed up front. If a rule needs bending to deal with any of
 * this, it gets bent here, and only here.
 */
@Mojo(name = "major-updates", aggregator = true, requiresDependencyResolution = ResolutionScope.NONE)
public class MajorUpdatesMojo extends AbstractMojo {

  public static final String GROUP_ID = "com.example";
  public static final String ARTIFACT_ID = "major-updates-maven-plugin";

  private final MavenSession mavenSession;
  private final RepositorySystem repositorySystem;
  private final ArtifactFactory artifactFactory;

  private @Nullable List<MavenProject> reactorProjects;
  private @Nullable File outputFile;
  private @Nullable String outputFormat;
  private @Nullable MojoExecution mojoExecution;

  @Inject
  public MajorUpdatesMojo(
    final MavenSession mavenSession, final RepositorySystem repositorySystem, final ArtifactFactory artifactFactory
  ) {
    this.mavenSession = mavenSession;
    this.repositorySystem = repositorySystem;
    this.artifactFactory = artifactFactory;
  }

  @Parameter(defaultValue = "${reactorProjects}", readonly = true)
  public void setReactorProjects(final List<MavenProject> reactorProjects) {
    this.reactorProjects = reactorProjects;
  }

  /**
   * Left unset by default, deliberately: if not given, the report is written directly to
   * {@link System#out} instead (see {@link #execute}) -- not to any default file path. This
   * matters beyond convenience: {@code major-updates-invoker} runs this plugin against arbitrary
   * target projects and must never write anything into them, so it deliberately never sets this
   * parameter. An explicit value given here is still used exactly as given, writing to that file.
   */
  @Parameter(property = "outputFile")
  public void setOutputFile(final File outputFile) {
    this.outputFile = outputFile;
  }

  /**
   * {@code csv} or {@code json}, case-insensitive; converted to {@link OutputFormat} in
   * {@link #execute}.
   */
  @Parameter(property = "outputFormat", defaultValue = "csv")
  public void setOutputFormat(final String outputFormat) {
    this.outputFormat = outputFormat;
  }

  @Parameter(defaultValue = "${mojoExecution}", readonly = true)
  public void setMojoExecution(final MojoExecution mojoExecution) {
    this.mojoExecution = mojoExecution;
  }

  @Override
  public void execute() throws MojoExecutionException {
    // No withIgnoredVersions/withRulesUri here anymore: this RuleService does no filtering
    // of its own. It exists only because DefaultVersionsHelper.Builder requires a non-null
    // instance -- confirmed from DefaultVersionsHelper's own constructor.
    final RuleService ruleService = new RulesServiceBuilder()
      .withMavenSession(mavenSession)
      .withLog(getLog())
      .build()
    ;
    final ExpressionEvaluator expressionEvaluator = new VersionsExpressionEvaluator(mavenSession, mojoExecution);
    final PomHelper pomHelper = new PomHelper(artifactFactory, expressionEvaluator);
    final VersionsHelperFacade versionsHelper = new VersionsHelperAdapter(
      new DefaultVersionsHelper.Builder()
      .withLog(getLog())
      .withMavenSession(mavenSession)
      .withRepositorySystem(repositorySystem)
      .withRuleService(ruleService)
      .withArtifactFactory(artifactFactory)
      .withPomHelper(pomHelper)
      .build()
    );
    final OutputFormat format = OutputFormat.valueOf(outputFormat.toUpperCase(Locale.ROOT));
    final ImmutableList<MavenProject> projects = ImmutableList.copyOf(reactorProjects);
    final MajorUpdatesReportGenerator generator = new MajorUpdatesReportGenerator(versionsHelper);
    if (outputFile != null) {
      generator.generate(projects, outputFile.toPath(), format);
    } else {
      generator.generateToConsole(projects, format);
    }
  }


  static final class VersionsHelperAdapter implements VersionsHelperFacade{

    private final VersionsHelper versionsHelper;

    VersionsHelperAdapter(final VersionsHelper versionsHelper) {
      this.versionsHelper = versionsHelper;
    }

    @Override
    public Optional<ArtifactVersion> lookupLatestMajorUpdate(
      final Dependency mavenDependency, final Predicate<String> eligible
    ) {
      try {
        final ArtifactVersions updates =
          versionsHelper.lookupDependencyUpdates(mavenDependency, false, true, false)
        ;
        return findNewestMajorUpdate(updates, eligible);
      } catch (final VersionRetrievalException e) {
        throw new RuntimeException("Lookup failed for "+mavenDependency+".", e);
      }
    }

    @Override
    public Optional<ArtifactVersion> lookupLatestMajorUpdate(
      final Plugin plugin, final Predicate<String> eligible
    ) {
      try {
        final ArtifactVersions updates = versionsHelper.lookupPluginUpdates(plugin, false);
        return findNewestMajorUpdate(updates, eligible);
      } catch (final VersionRetrievalException e) {
        throw new RuntimeException("Lookup failed for "+plugin+".", e);
      }
    }

    /**
     * The real versions-common comparison logic, extracted as its own package-private static
     * method specifically so it can be unit-tested directly against a hand-built
     * {@code ArtifactVersions} -- see {@code VersionsHelperAdapterTest} -- without needing a
     * real {@link VersionsHelper}, which has never been mocked or tested anywhere in this
     * project, before or after this extraction; it never needed to be, since this method is the
     * only part that actually depends on the outcome of a lookup, not on how that lookup itself
     * happens.
     */
    static Optional<ArtifactVersion> findNewestMajorUpdate(
      final ArtifactVersions updates, final Predicate<String> eligible
    ) {
      return Optional.ofNullable(
        updates.filter(eligible::test)
          .getNewestUpdateWithinSegment(Optional.of(Segment.MAJOR), false)
      );
    }
  }
}