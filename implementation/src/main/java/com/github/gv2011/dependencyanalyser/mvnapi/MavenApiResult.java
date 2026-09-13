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

import java.util.List;
import java.util.Optional;

/**
 * Outcome of a {@link MavenApi} invocation. Carries the real exceptions
 * Maven itself collected, unconverted and unswallowed (no exit-code
 * translation, no console logging as a side effect), plus the coordinates
 * of the project that was actually executed against, when available.
 *
 * <p>An empty {@link #exceptions()} list means the build succeeded. An
 * absent {@link #project()} means execution failed before any project
 * could be built at all (e.g. the POM itself could not be read) - not
 * every failure has a project to report.
 */
public record MavenApiResult(List<Throwable> exceptions, Optional<ProjectCoordinates> project) {

  public MavenApiResult {
    exceptions = List.copyOf(exceptions);
  }

  /**
   * groupId/artifactId/version as plain strings, matching Maven's own
   * MavenProject getters directly - not the dependency-analyser-api's own
   * ArtifactIdentity, since this type belongs to MavenApi's own,
   * Maven-flavoured contract, not to our domain model.
   */
  public record ProjectCoordinates(String groupId, String artifactId, String version) {}

}
