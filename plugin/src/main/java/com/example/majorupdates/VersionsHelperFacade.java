package com.example.majorupdates;

import java.util.Optional;
import java.util.function.Predicate;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;

/**
 * The actual boundary between this plugin's own logic and versions-common. Narrowed twice now:
 * first to return a plain {@link ArtifactVersion} rather than versions-common's own
 * {@code ArtifactVersions} type; then further here -- the filter/major-segment-comparison logic
 * itself moved out of {@code MajorUpdatesReportGenerator} and into
 * {@code MajorUpdatesMojo.VersionsHelperAdapter} (see its own javadoc), so this interface's
 * contract is now "give me the final answer, given an eligibility predicate," not "give me raw
 * candidates for the caller to reduce."
 *
 * <p>Deliberate, not narrowing for its own sake: it keeps the real, already-tested
 * versions-common comparison logic ({@code ArtifactVersions.filter(...)},
 * {@code getNewestUpdateWithinSegment(...)}) in the one place that already legitimately depends
 * on versions-common directly -- the adapter -- rather than reimplementing that comparison
 * ourselves, which the previous, still-narrower signature would have forced.
 */
interface VersionsHelperFacade {

  Optional<ArtifactVersion> lookupLatestMajorUpdate(
    Dependency mavenDependency, Predicate<String> eligible
  );

  Optional<ArtifactVersion> lookupLatestMajorUpdate(Plugin plugin, Predicate<String> eligible);

}
