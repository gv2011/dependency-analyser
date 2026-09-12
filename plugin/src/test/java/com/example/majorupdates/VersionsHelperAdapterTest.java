package com.example.majorupdates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.codehaus.mojo.versions.api.AbstractVersionDetails;
import org.codehaus.mojo.versions.api.ArtifactVersions;
import org.junit.jupiter.api.Test;

/**
 * Tests {@code MajorUpdatesMojo.VersionsHelperAdapter.findNewestMajorUpdate} directly -- the
 * real major-version comparison logic ({@code ArtifactVersions.filter(...)},
 * {@code getNewestUpdateWithinSegment(...)}), now that it lives in the adapter rather than
 * {@link MajorUpdatesReportGenerator}. Same pattern {@link ArtifactVersionsSegmentTest} already
 * established (a hand-built {@link ArtifactVersions}, no {@code VersionsHelper} needed at all --
 * that method never depended on a real lookup, only on its outcome).
 *
 * <p>These three tests are the direct relocation of what {@code MajorUpdatesReportGeneratorTest}
 * used to cover before the move; see that file's own javadoc for why they couldn't stay there.
 */
class VersionsHelperAdapterTest {

  @Test
  void returnsEmptyWhenNoMajorUpdateExists() {
    final Optional<ArtifactVersion> result = findNewestMajorUpdate(
      "3.27.7", "3.27.7", "3.28.0"
    );

    assertTrue(result.isEmpty());
  }

  @Test
  void returnsLatestMajorVersionWhenOneExists() {
    final Optional<ArtifactVersion> result = findNewestMajorUpdate(
      "3.27.7", "3.27.7", "3.28.0", "4.0.0"
    );

    assertEquals(Optional.of(new DefaultArtifactVersion("4.0.0")), result);
  }

  /**
   * Companion to {@link ArtifactVersionsSegmentTest#milestoneIsConsideredMajor}, which proves
   * the opposite: versions-common's own comparison logic, unfiltered, treats a milestone as a
   * legitimate major update. This proves our filtering actually removes it at this level.
   */
  @Test
  void excludesMilestoneVersions() {
    final Optional<ArtifactVersion> result = findNewestMajorUpdate(
      "3.27.7", "3.27.7", "3.28.0", "4.0.0-M1"
    );

    assertTrue(result.isEmpty());
  }

  /**
   * ICU4J uses a two-part {@code MAJOR.MINOR} scheme with no third component -- checks that
   * doesn't confuse the major-segment comparison.
   */
  @Test
  void icu4jMajorUpdateIsDetected() {
    final Optional<ArtifactVersion> result = findNewestMajorUpdate(
      "77.1", "77.1", "78.3"
    );

    assertEquals(Optional.of(new DefaultArtifactVersion("78.3")), result);
  }

  private static Optional<ArtifactVersion> findNewestMajorUpdate(
    final String currentVersion, final String... candidateVersions
  ) {
    final Artifact artifact = new DefaultArtifact(
      "org.assertj", "assertj-core", currentVersion,
      "compile", "jar", "", new DefaultArtifactHandler("jar")
    );
    final ArtifactVersions updates = new ArtifactVersions(
      artifact,
      ( List.of(candidateVersions).stream()
        .<ArtifactVersion>map(DefaultArtifactVersion::new)
        .toList()
      )
    );

    return MajorUpdatesMojo.VersionsHelperAdapter.findNewestMajorUpdate(
      updates, v -> !AbstractVersionDetails.isPreReleaseVersion(v)
    );
  }
}