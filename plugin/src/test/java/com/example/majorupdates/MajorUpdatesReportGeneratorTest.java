package com.example.majorupdates;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.function.Predicate;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.junit.jupiter.api.Test;

import com.example.majorupdates.Dependency.DefaultDependency;
import com.example.majorupdates.Dependency.DependencyKind;

/**
 * Tests {@link MajorUpdatesReportGenerator}'s own remaining logic -- just the "don't look
 * anything up if no version is declared" gate now.
 *
 * <p>This file used to also cover the major-version comparison itself (excluding milestones,
 * picking the newest within-segment update), via a mock {@link VersionsHelperFacade} handing
 * back raw candidates for real code in this class to reduce. That comparison logic has since
 * moved into {@code MajorUpdatesMojo.VersionsHelperAdapter} (see {@link VersionsHelperFacade}'s
 * own javadoc for why). Testing it here now would only prove "does the mock return what it's
 * told to return" -- the word "milestone" would survive only in a test's name, not in anything
 * it actually exercised -- so those three tests were removed rather than kept as hollowed-out
 * plumbing checks. The same real coverage, against the same real versions-common logic, now
 * lives in {@code VersionsHelperAdapterTest} instead, relocated to where that logic actually
 * runs.
 */
class MajorUpdatesReportGeneratorTest {

  @Test
  void returnsEmptyWhenDependencyHasNoVersion() {
    final Dependency dependency = defaultDependency(Optional.empty());
    final VersionsHelperFacade neverCalled = new VersionsHelperFacade() {
      @Override
      public Optional<ArtifactVersion> lookupLatestMajorUpdate(
        final org.apache.maven.model.Dependency d, final Predicate<String> eligible
      ) {
        throw new AssertionError("lookup must not run when the version is absent");
      }

      @Override
      public Optional<ArtifactVersion> lookupLatestMajorUpdate(
        final org.apache.maven.model.Plugin p, final Predicate<String> eligible
      ) {
        throw new AssertionError("lookup must not run when the version is absent");
      }
    };

    final Optional<Version> result =
      new MajorUpdatesReportGenerator(neverCalled)
      .getLatestMajorUpdate(dependency)
    ;

    assertTrue(result.isEmpty());
  }

  private static Dependency defaultDependency(final Optional<String> version) {
    final org.apache.maven.model.Dependency mavenDependency =
      new org.apache.maven.model.Dependency()
    ;
    mavenDependency.setGroupId("org.assertj");
    mavenDependency.setArtifactId("assertj-core");
    version.ifPresent(mavenDependency::setVersion);
    return new DefaultDependency(DependencyKind.DEPENDENCY, mavenDependency);
  }
}
