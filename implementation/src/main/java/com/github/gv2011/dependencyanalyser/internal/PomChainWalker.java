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
 * Walks a leaf project's own version declarations together with those of
 * every project reachable from it. A project is reachable from the leaf
 * if it is the leaf itself, or is the parent of a reachable project, or
 * is a BOM imported by a reachable project. Uses only the
 * DependencyAnalyser API (getProject, and Project's own parent(),
 * boms(), getVersionDeclarations()) - no direct Maven access of its own.
 */
public final class PomChainWalker {

  private final DependencyAnalyser analyser;

  public PomChainWalker(final DependencyAnalyser analyser) {
    this.analyser = analyser;
  }

  /**
   * @param declaration what this project's own text declares
   * @param declaringProject the project whose own text {@code declaration}
   *   came from - not carried by VersionDeclaration itself
   */
  public record LocatedDeclaration(VersionDeclaration declaration, MavenCoordinates declaringProject) {}

  /**
   * Every version declaration found across every project reachable from
   * the leaf (see class javadoc for what "reachable" means), keyed by
   * artifact - first one found wins (the leaf's own, then its parent
   * chain and BOM imports, depth-first).
   */
  public Map<ArtifactIdentity, LocatedDeclaration> declarationsReachableFrom(final Path leafDirectory) {
    final Project leaf = analyser.getProject(leafDirectory);
    final Map<ArtifactIdentity, LocatedDeclaration> result = new HashMap<>();
    walk(leaf, new HashSet<>(), result);
    return result;
  }

  /**
   * @param current the project being visited at this step of the walk
   */
  private void walk(
    final Project current,
    final Set<MavenCoordinates> visited,
    final Map<ArtifactIdentity, LocatedDeclaration> result
  ) {
    if(!visited.add(current.coordinates())) {
      return; // already walked - a BOM (or, wrongly, a parent) reached more than once
    }
    // TODO "first declaration found wins" is only a crude approximation
    // of Maven's real "nearest wins" precedence. Proper reporting should
    // show every declaration found for a given artifact, not just the
    // first, and let the reader judge a real conflict themselves.
    for(final VersionDeclaration declaration: current.getVersionDeclarations()) {
      result.putIfAbsent(declaration.artifact(), new LocatedDeclaration(declaration, current.coordinates()));
    }
    for(final Dependency bom: current.boms()) {
      walk(analyser.getProject(bom.coordinates()), visited, result);
    }
    current.parent().ifPresentDo(parentCoordinates ->
      walk(analyser.getProject(parentCoordinates), visited, result)
    );
  }

}
