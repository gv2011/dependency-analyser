package com.example.majorupdates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.junit.jupiter.api.Test;

class VersionTest {

    @Test
    void extractsMajorFromSemanticVersion() {
        final Version version = Version.of(new DefaultArtifactVersion("1.2.3"));
        assertEquals(Optional.of(1), version.major());
        assertEquals("1.2.3", version.raw());
    }

    /**
     * Relies on DefaultArtifactVersion's fallback behaviour for non-conforming strings -- flagged
     * as unverified against that class's own source in Version.of's javadoc. Run this test before
     * trusting the heuristic; if it fails, fix Version.of, not this test.
     */
    @Test
    void treatsNonSemanticVersionAsHavingNoMajor() {
        final Version version = Version.of(new DefaultArtifactVersion("not-a-version"));
        assertTrue(version.major().isEmpty());
        assertEquals("not-a-version", version.raw());
    }

    /**
     * Version.parse is new since this test file was last reviewed -- a thin wrapper around
     * Version.of(new DefaultArtifactVersion(...)), added so callers (Dependency's subclasses)
     * don't need to know DefaultArtifactVersion exists. This covers the wiring, not new logic.
     */
    @Test
    void parseDelegatesToOf() {
        final Version parsed = Version.parse("2.0.0");
        assertEquals(Version.of(new DefaultArtifactVersion("2.0.0")), parsed);
    }
}