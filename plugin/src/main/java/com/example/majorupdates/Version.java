package com.example.majorupdates;

import java.util.Optional;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

/**
 * A dependency or plugin version, independent of Maven's {@link ArtifactVersion}.
 *
 * <p>This project's central concern is detecting major-version updates. {@link ArtifactVersion}
 * assumes a major.minor.incremental-qualifier shape and silently falls back to treating the whole
 * string as an opaque qualifier when it doesn't fit that shape -- which means "major version 0"
 * and "not a semantic version at all" become indistinguishable once you're only holding an
 * {@link ArtifactVersion}. Keeping our own type here, even as a thin wrapper today, means that
 * distinction -- and any future non-semver scheme -- can be handled properly later without
 * touching every call site that consumes a {@link Version}.
 *
 * @param major the parsed major segment, or empty when this version doesn't have a usable one
 *              (including genuinely non-semantic version strings)
 * @param raw   the original version string, always preserved even when {@code major} is empty
 */
record Version(Optional<Integer> major, String raw) {

    /**
     * The only place in the codebase that should know {@link ArtifactVersion} exists --
     * everywhere else should depend on {@link Version} instead.
     *
     * <p><b>Heuristic, not a guarantee:</b> {@link ArtifactVersion} doesn't expose whether it
     * actually parsed a major.minor.incremental structure or fell back to treating the entire
     * string as a qualifier. We infer "fell back" when the qualifier equals the raw string and
     * every numeric component is zero. This can misclassify a genuine, deliberate "0.0.0" as
     * non-semantic -- a real limitation, and exactly the kind of edge case this type exists to
     * eventually fix with real parsing instead of inferring it from the outside.
     *
     * <p><b>Unverified:</b> this reasoning about {@link org.apache.maven.artifact.versioning
     * .DefaultArtifactVersion}'s fallback behaviour for non-conforming strings hasn't been
     * confirmed against that class's own parsing source -- see README.md.
     */
    static Version of(final ArtifactVersion artifactVersion) {
        final String raw = artifactVersion.toString();
        final boolean looksNonSemantic = raw.equals(artifactVersion.getQualifier())
                && artifactVersion.getMajorVersion() == 0
                && artifactVersion.getMinorVersion() == 0
                && artifactVersion.getIncrementalVersion() == 0
                && artifactVersion.getBuildNumber() == 0;
        final Optional<Integer> major = looksNonSemantic
                ? Optional.empty()
                : Optional.of(artifactVersion.getMajorVersion());
        return new Version(major, raw);
    }

    static Version parse(final String version) {
      return Version.of(new DefaultArtifactVersion(version));
    }


    @Override
    public String toString() {
        return raw;
    }
}
