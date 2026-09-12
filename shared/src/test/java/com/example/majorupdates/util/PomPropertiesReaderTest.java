package com.example.majorupdates.util;

import static com.example.majorupdates.util.GroupAndArtifact.ARTIFACT_ID;
import static com.example.majorupdates.util.GroupAndArtifact.GROUP_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class PomPropertiesReaderIT {

  @Test
  void readPomProperties() {
    assertTrue(new PomPropertiesReader(GROUP_ID, ARTIFACT_ID).readPomProperties().isPresent());
  }

  @Test
  void readVersion() {
    assertThat(GroupAndArtifact.VERSION, is(Optional.of("0.1.0-SNAPSHOT")));
  }

}