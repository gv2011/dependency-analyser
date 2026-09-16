package com.github.gv2011.dependencyanalyser.internal;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.github.gv2011.dependencyanalyser.api.ArtifactIdentity;
import com.github.gv2011.dependencyanalyser.api.Dependency;
import com.github.gv2011.dependencyanalyser.api.DependencyAnalyser;
import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;
import com.github.gv2011.dependencyanalyser.api.Project;
import com.github.gv2011.dependencyanalyser.api.VersionDeclaration;

/**
 * Walks a leaf project's own version declarations together with its
 * whole reachable parent/BOM-import chain. Uses only the
 * DependencyAnalyser API (getProject, and Project's own parent(),
 * boms(), getVersionDeclarations()) - no direct Maven access of its own.
 */
public final class PomChainWalker {

  private final DependencyAnalyser analyser;

  public PomChainWalker(final DependencyAnalyser analyser) {
    this.analyser = analyser;
  }

  /**
   * Every version declaration reachable from the leaf, keyed by
   * artifact - first one found wins (the leaf's own, then its parent
   * chain and BOM imports, depth-first).
   */
  public Map<ArtifactIdentity, VersionDeclaration> declarationsReachableFrom(final Path leafDirectory) {
    final Project leaf = analyser.getProject(leafDirectory);
    final Map<ArtifactIdentity, VersionDeclaration> result = new HashMap<>();
    walk(leaf, new HashSet<>(), result);
    return result;
  }

  private void walk(
    final Project project,
    final Set<MavenCoordinates> visited,
    final Map<ArtifactIdentity, VersionDeclaration> result
  ) {
    if(!visited.add(project.coordinates())) {
      return; // already walked - a BOM (or, wrongly, a parent) reached more than once
    }
    // TODO "first declaration found wins" is only a crude approximation
    // of Maven's real "nearest wins" precedence. Proper reporting should
    // show every declaration found for a given artifact, not just the
    // first, and let the reader judge a real conflict themselves.
    for(final VersionDeclaration declaration: project.getVersionDeclarations()) {
      result.putIfAbsent(declaration.artifact(), declaration);
    }
    for(final Dependency bom: project.boms()) {
      walk(analyser.getProject(bom.coordinates()), visited, result);
    }
    project.parent().ifPresentDo(parentCoordinates ->
      walk(analyser.getProject(parentCoordinates), visited, result)
    );
  }

}
