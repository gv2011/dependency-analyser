/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.cli;

import static com.github.gv2011.util.BeanUtils.beanBuilder;
import static com.github.gv2011.util.icol.ICollections.xStream;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulationException;
import org.apache.maven.execution.MavenExecutionResult;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;

import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;
import com.github.gv2011.dependencyanalyser.impl.Conversions;
import com.github.gv2011.dependencyanalyser.mvnapi.MavenApi;
import com.github.gv2011.dependencyanalyser.mvnapi.MavenApiResult;
import com.github.gv2011.util.icol.ICollections;
import com.github.gv2011.util.icol.Opt;

/**
 * A thin subclass of MavenCliBase (a visibility-relaxed copy of the real
 * MavenCli). Provides new entry points instead of overriding
 * doMain(...)/execute(...) - Java does not allow overriding to change a
 * return type from int to an object type, so genuinely new methods
 * (run/executeRequest below) are the only option.
 *
 * Never calls the inherited doMain(...)/execute(...); those still contain
 * the original System.out/exit-code behaviour, simply unused here. The
 * inherited entry points are overridden/hidden below to throw immediately,
 * so an accidental call surfaces as a clear failure rather than silently
 * running the old behaviour.
 */
public class MavenApiImpl extends MavenCliBase implements MavenApi{

  public MavenApiImpl() {
    super();
  }

  /**
   * Runs Maven against the given working directory. Never reads or writes
   * System.out/System.err, never converts what happened into a bare exit
   * code - see MavenApiResult.
   */
  @Override
  public MavenApiResult doMain(final String[] args, final String workingDirectory) {
    final Set<String> realms;
    if (classWorld != null) {
      realms = new HashSet<>();
      for (final ClassRealm realm : classWorld.getRealms()) {
        realms.add(realm.getId());
      }
    } else {
      realms = Collections.emptySet();
    }

    try {
      final CliRequest cliRequest = new CliRequest(args, classWorld);
      cliRequest.workingDirectory = workingDirectory;
      return run(cliRequest);
    } finally {
      if (classWorld != null) {
        for (final ClassRealm realm : new ArrayList<>(classWorld.getRealms())) {
          final String realmId = realm.getId();
          if (!realms.contains(realmId)) {
            try {
              classWorld.disposeRealm(realmId);
            } catch (final NoSuchRealmException ignored) {
              // can't happen
            }
          }
        }
      }
    }
  }

  /**
   * Replicates the inherited doMain(CliRequest)'s pipeline (same setup
   * steps, same order) and execute()'s real work, but never calls either -
   * both still return a bare int and would run the old behaviour if
   * invoked. No console logging, no exit-code translation: the real
   * exceptions and the project's coordinates go straight into
   * MavenApiResult.
   *
   * <p>Scope note (unchanged from the previous refactor): informativeCommands
   * and encryption below are inherited as-is from MavenCliBase and can
   * still throw ExitException / print via System.out/System.err internally
   * (--version, --help, --encrypt-password) - dead paths for the fixed,
   * well-formed arguments this project always passes, not rewritten here.
   */
  private MavenApiResult run(final CliRequest cliRequest) {
    PlexusContainer localContainer = null;
    try {
      initialize(cliRequest);
      cli(cliRequest);
      properties(cliRequest);
      logging(cliRequest);
      informativeCommands(cliRequest);
      version(cliRequest);
      localContainer = container(cliRequest);
      commands(cliRequest);
      configure(cliRequest);
      toolchains(cliRequest);
      populateRequest(cliRequest);
      encryption(cliRequest);
      return executeRequest(cliRequest);
    } catch (final Exception e) {
      return beanBuilder(MavenApiResult.class)
        .set(MavenApiResult::exceptions).to(ICollections.listOf((Throwable)e))
        .build()
      ;
    } finally {
      if (localContainer != null) {
        localContainer.dispose();
      }
    }
  }

  /**
   * Replicates what the inherited (private, unused here) execute(CliRequest)
   * used to do, minus the console error-summary printing and the
   * REACTOR_FAIL_NEVER special case.
   */
  private MavenApiResult executeRequest(final CliRequest cliRequest) throws MavenExecutionRequestPopulationException {
    final MavenExecutionRequest request = executionRequestPopulator.populateDefaults(cliRequest.request);

    eventSpyDispatcher.onEvent(request);

    final MavenExecutionResult result = maven.execute(request);

    eventSpyDispatcher.onEvent(result);

    eventSpyDispatcher.close();

    final Opt<MavenCoordinates> project = xStream(result.getTopologicallySortedProjects())
      .tryFindFirst()
      .map(Conversions::toMavenCoordinates)
    ;
    return beanBuilder(MavenApiResult.class)
      .set(MavenApiResult::exceptions).to(ICollections.listFrom(result.getExceptions()))
      .set(MavenApiResult::project).to(project)
      .build()
    ;
  }

  // Everything below hides/overrides an inherited entry point that would
  // otherwise silently run MavenCliBase's original System.out/exit-code
  // behaviour. Each throws immediately instead of running that behaviour,
  // so an accidental call fails loudly rather than doing the wrong thing
  // quietly - this is what catches a wrong assumption about "this path is
  // never used".

  @Override
  public int doMain(final CliRequest cliRequest) {
    throw new UnsupportedOperationException(
      "Not supported on MavenApi - use doMain(String[], String), which returns MavenApiResult."
    );
  }

  @Override
  public int doMain(final String[] args, final String workingDirectory, final PrintStream stdout, final PrintStream stderr) {
    throw new UnsupportedOperationException(
      "Not supported on MavenApi - use doMain(String[], String), which returns MavenApiResult."
    );
  }

  public static void main(final String[] args) {
    throw new UnsupportedOperationException("Not supported on MavenApi.");
  }

  public static int main(final String[] args, final ClassWorld classWorld) {
    throw new UnsupportedOperationException("Not supported on MavenApi.");
  }

  public static int doMain(final String[] args, final ClassWorld classWorld) {
    throw new UnsupportedOperationException("Not supported on MavenApi.");
  }

}
