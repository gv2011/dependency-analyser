package com.example.majorupdates.invoker;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.maven.cli.MavenCli;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.majorupdates.transport.MajorUpdate;
import com.example.majorupdates.transport.UpdateResponse;
import com.example.majorupdates.util.GroupAndArtifact;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;


public final class MajorUpdatesInvoker {

  private static final Logger LOGGER = LoggerFactory.getLogger(MajorUpdatesInvoker.class);

  /**
   * @param projectDirectory the target project's directory -- the plugin's goal runs as if
   *                          invoked from here, independent of this JVM's own working directory.
   *                          Never written to; see this class's own javadoc.
   * @return the parsed updates, plus the full captured Maven output
   * @throws IllegalStateException if this module's own version is unavailable (see
   *         {@link #OWN_VERSION}), meaning there is no valid coordinate to invoke the plugin by
   * @throws MajorUpdatesInvocationException if the invocation itself fails, or its output can't
   *         be parsed afterward -- NOT for a plugin-version mismatch, which only logs a warning;
   *         see this class's own javadoc
   */
  public InvocationResult invoke(final Path projectDirectory) {
    final String goal = "com.example:major-updates-maven-plugin:" + GroupAndArtifact.VERSION.get() + ":major-updates";

    final ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();
    final ByteArrayOutputStream capturedErr = new ByteArrayOutputStream();

    // The one setup step confirmed (via a working example) to matter for MavenCli; see this
    // class's own javadoc for what's still unverified beyond it.
    System.setProperty("maven.multiModuleProjectDirectory", projectDirectory.toString());

    final int exitCode;
    try {
      exitCode = new MavenCli().doMain(
        new String[] { "-q", goal, "-DoutputFormat=json" },
        projectDirectory.toString(),
        new PrintStream(capturedOut, true, StandardCharsets.UTF_8),
        new PrintStream(capturedErr, true, StandardCharsets.UTF_8)
      );
    } catch (final RuntimeException e) {
      // doMain(CliRequest) normally catches its own failures and returns a non-zero exit code
      // instead of throwing (see the exitCode != 0 branch below) -- but relying on that alone
      // would mean a genuinely thrown exception bypasses our own captured-output wrapping
      // entirely, defeating the point of always attaching diagnostic output. Caught broadly
      // here specifically as a safety net against that, not because a specific exception type
      // is expected.
      throw new MajorUpdatesInvocationException(
        "MavenCli.doMain itself threw, rather than returning a non-zero exit code",
        formatCapturedOutput(
          capturedOut.toString(StandardCharsets.UTF_8), capturedErr.toString(StandardCharsets.UTF_8)
        ),
        e
      );
    }

    final String stdout = capturedOut.toString(StandardCharsets.UTF_8);
    final String stderr = capturedErr.toString(StandardCharsets.UTF_8);
    final String mavenOutput = formatCapturedOutput(stdout, stderr);

    if (exitCode != 0) {
      throw new MajorUpdatesInvocationException(
        goal + " exited with code " + exitCode, mavenOutput
      );
    }

    final ImmutableList<MajorUpdate> updates =
      parseAndVerifyResults(stdout, mavenOutput, GroupAndArtifact.VERSION.get())
    ;
    return new InvocationResult(updates, mavenOutput);
  }

  private static ImmutableList<MajorUpdate> parseAndVerifyResults(
    final String stdout, final String mavenOutput, final String expectedVersion
  ) {
    final UpdateResponse results;
    try {
      results = new Gson().fromJson(stdout, UpdateResponse.class);
    } catch (final JsonSyntaxException e) {
      throw new MajorUpdatesInvocationException(
        "Could not parse the plugin's own JSON report from captured stdout", mavenOutput, e
      );
    }

    final Optional<String> actualVersion = Optional.ofNullable(results.pluginVersion());
    if (!actualVersion.equals(Optional.of(expectedVersion))) {
      // A signal worth surfacing, not a reason to discard otherwise-usable results -- see this
      // class's own javadoc for why this warns rather than throws.
      LOGGER.warn(
        "Plugin version mismatch: expected {} (this invoker's own version) but the plugin that "
        + "actually ran reported {}. This may indicate a different or stale plugin build was "
        + "resolved.",
        expectedVersion, actualVersion.orElse("<none>")
      );
    }

    return ImmutableList.copyOf(results.updates());
  }

  private static String formatCapturedOutput(final String stdout, final String stderr) {
    return "--- stdout ---\n" + stdout + "\n--- stderr ---\n" + stderr;
  }

}