package com.example.majorupdates.cli;

import java.nio.file.Path;

import com.example.majorupdates.invoker.InvocationResult;
import com.example.majorupdates.invoker.MajorUpdatesInvoker;
import com.example.majorupdates.transport.MajorUpdate;

/**
 * Thin CLI wrapper around {@link MajorUpdatesInvoker} -- library usage (calling
 * {@link MajorUpdatesInvoker} directly) is the primary intended use of this module; this class
 * exists for convenience, not as the main interface. Deliberately NOT held to this project's
 * usual rules -- a process entry point, boundary code, not business logic. Real {@link System#out}
 * usage here is deliberate and appropriate, unlike inside {@link MajorUpdatesInvoker} itself: a
 * CLI invocation is explicitly meant to print, whereas a library caller decides that for itself.
 */
public final class Main {

  private Main() {
  }

  public static void main(final String[] args) {
    final Path projectDir = Path.of(args.length==0?".":args[0]);
    final InvocationResult result = new MajorUpdatesInvoker().invoke(projectDir);
    System.out.println(result.mavenOutput());
    for (final MajorUpdate update : result.updates()) {
      System.out.println(
        update.module() + "," + update.kind() + "," + update.groupId() + ","
        + update.artifactId() + "," + update.currentVersion() + "," + update.latestVersion()
      );
    }
  }
}
