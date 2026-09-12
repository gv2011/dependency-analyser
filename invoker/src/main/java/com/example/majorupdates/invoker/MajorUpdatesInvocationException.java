package com.example.majorupdates.invoker;

/**
 * Thrown when the embedded Maven invocation itself fails (non-zero exit code), or when the
 * plugin's own JSON output file could not be read or parsed afterward. Either way, the full
 * captured Maven output (stdout and stderr) is attached to the message -- since nothing is ever
 * printed live (see {@link MajorUpdatesInvoker#run}'s own javadoc), this is the only place that
 * diagnostic information surfaces on failure.
 */
public final class MajorUpdatesInvocationException extends RuntimeException {

  public MajorUpdatesInvocationException(final String message, final String mavenOutput) {
    super(formatMessage(message, mavenOutput));
  }

  public MajorUpdatesInvocationException(
    final String message, final String mavenOutput, final Throwable cause
  ) {
    super(formatMessage(message, mavenOutput), cause);
  }

  private static String formatMessage(final String message, final String mavenOutput) {
    return message + "\n\n--- Captured Maven output ---\n" + mavenOutput;
  }
}
