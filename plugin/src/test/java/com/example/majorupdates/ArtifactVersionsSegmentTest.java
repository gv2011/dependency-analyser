package com.example.majorupdates;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.codehaus.mojo.versions.api.AbstractVersionDetails;
import org.codehaus.mojo.versions.api.ArtifactVersions;
import org.codehaus.mojo.versions.api.Segment;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

/**
 * Exercises versions-common's own segment-comparison logic ({@link ArtifactVersions} plus
 * {@code getNewestUpdateWithinSegment}, inherited from {@code AbstractVersionDetails}) directly
 * -- no {@code MavenSession}, no {@code RepositorySystem}, no plugin lifecycle, no
 * {@link MajorUpdatesMojo} involved at all. Confirms this piece is genuinely testable in
 * isolation, and directly resolves README.md gap 2 ("exact signature is unconfirmed") -- this
 * test either compiles and passes against the real signature, or fails loudly if the assumed
 * signature was wrong.
 *
 * <p><b>Partially unverified:</b> {@link ArtifactVersions}'s constructor is confirmed public
 * from its own pasted source. Constructing the minimal {@link Artifact} via
 * {@link DefaultArtifact}/{@link DefaultArtifactHandler} is not verified against pasted source
 * the same way -- it's recalled from general knowledge of core, long-stable {@code maven-artifact}
 * API, not a {@code versions-common} internal, so confidence is reasonably high, but a failure
 * here should be read as "check DefaultArtifact's real constructor," not as evidence against the
 * broader claim that this logic is testable without Maven plumbing.
 */
class ArtifactVersionsSegmentTest {

  @Test
  void milestoneIsConsideredMajor() {
    final ImmutableList<ArtifactVersion> versions = Stream.of("3.27.7", "3.28.0", "4.0.0-M1")
      .map(DefaultArtifactVersion::new)
      .collect(ImmutableList.toImmutableList())
    ;

    final Artifact artifact = new DefaultArtifact(
      "org.assertj", "assertj-core", versions.get(0).toString(),
      "compile", "jar", "", new DefaultArtifactHandler("jar")
    );

    final Optional<ArtifactVersion> result = Optional.ofNullable(
      new ArtifactVersions(artifact, versions)
      .getNewestUpdateWithinSegment(Optional.of(Segment.MAJOR), false)
    );

    assertThat(result, is(Optional.of(versions.get(2))));
  }

    @Test
    void returnsNullWhenNoMajorUpdateExists() {
        final Artifact artifact = new DefaultArtifact(
                "org.assertj", "assertj-core", "3.27.7", "compile", "jar", "", new DefaultArtifactHandler("jar"));

        final List<ArtifactVersion> candidates = List.of(
                new DefaultArtifactVersion("3.27.7"),
                new DefaultArtifactVersion("3.28.0"));

        final ArtifactVersions versions = new ArtifactVersions(artifact, candidates);

        final ArtifactVersion result = versions.getNewestUpdateWithinSegment(Optional.of(Segment.MAJOR), false);

        assertNull(result);
    }

    /**
     * The only test in this file exercising a {@link VersionRange}-constructed {@link Artifact}
     * -- via {@code VersionRange.createFromVersionSpec(version)} and {@link DefaultArtifact}'s
     * 8-argument, VersionRange-taking constructor -- rather than the plain-string constructor
     * every other test here uses. Confirmed from
     * {@code org.codehaus.mojo.versions.utils.ArtifactFactory.createArtifact(Dependency)}'s own
     * pasted source to be how real production code actually builds its {@link Artifact}, so this
     * is the one test confirming the comparison logic still behaves correctly against that real
     * construction path, not just the simpler stand-in every other test relies on.
     *
     * <p>Originally written during an investigation into an empty-CSV report, alongside a
     * sibling test using the plain-string constructor -- both passed, meaning neither
     * construction method was the actual cause (a stale build was). That sibling was since
     * removed as redundant with {@link #milestoneIsConsideredMajor}/
     * {@link #returnsNullWhenNoMajorUpdateExists} once its original diagnostic purpose was moot;
     * this one earns its keep on a different, still-real basis: coverage of the VersionRange
     * construction path itself.
     */
    @Test
    void reproducesRealCandidateSetForSlugifyLikeCase()
            throws InvalidVersionSpecificationException {
        final Artifact artifact = new DefaultArtifact(
                "com.github.slugify", "slugify",
                VersionRange.createFromVersionSpec("3.0.7"),
                "compile", "jar", "", new DefaultArtifactHandler("jar"), false);

        final List<ArtifactVersion> candidates = List.of(
                new DefaultArtifactVersion("4.0.0"),
                new DefaultArtifactVersion("4.0.1"));

        final ArtifactVersions versions = new ArtifactVersions(artifact, candidates);

        final ArtifactVersion result = versions
                .filter(v -> !AbstractVersionDetails.isPreReleaseVersion(v))
                .getNewestUpdateWithinSegment(Optional.of(Segment.MAJOR), false);

        assertEquals("4.0.1", result == null ? null : result.toString());
    }
}