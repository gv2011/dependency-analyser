package com.github.gv2011.dependencyanalyser.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

import com.github.gv2011.dependencyanalyser.api.Version;
import com.github.gv2011.util.icol.Opt;

/**
 * Whether pom.properties is actually present depends on build phase - see
 * ReactorVersion's own javadoc - so this deliberately does not assert
 * presence or absence either way; a plain {@code mvn test} run (before
 * packaging) legitimately sees {@link Opt#empty()} here, not a failure.
 */
class ReactorVersionTest {

  @Test
  void getDoesNotThrowRegardlessOfAvailability() {
    final Opt<Version> version = ReactorVersion.get();
    if(version.isPresent()) {
      assertThat(version.get().toString(), is(not("")));
    }
  }

}
