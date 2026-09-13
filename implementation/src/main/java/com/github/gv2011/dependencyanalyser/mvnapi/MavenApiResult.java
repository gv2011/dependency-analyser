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
package com.github.gv2011.dependencyanalyser.mvnapi;

import org.apache.maven.cli.MavenApiImpl;

import com.github.gv2011.util.beans.Bean;
import com.github.gv2011.util.icol.IList;
import com.github.gv2011.util.icol.Opt;

/**
 * Outcome of a {@link MavenApiImpl} invocation. Carries the real exceptions
 * Maven itself collected, unconverted and unswallowed (no exit-code
 * translation, no console logging as a side effect), the coordinates of
 * the project that was actually executed against, when available, and
 * whatever the invoked goal wrote to System.out (see
 * {@link CapturedSystemOut}) - many Maven goals have no other way to
 * report their findings.
 *
 * <p>An empty {@link #exceptions()} list means the build succeeded. An
 * absent {@link #project()} means execution failed before any project
 * could be built at all (e.g. the POM itself could not be read) - not
 * every failure has a project to report.
 */
public interface MavenApiResult extends Bean{

  IList<Throwable> exceptions();

  Opt<MavenCoordinates> project();

  String output();

}
