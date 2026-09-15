package com.github.gv2011.dependencyanalyser.internal;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.github.gv2011.dependencyanalyser.api.ArtifactIdentity;
import com.github.gv2011.dependencyanalyser.api.Classpath;
import com.github.gv2011.dependencyanalyser.api.Dependency;
import com.github.gv2011.dependencyanalyser.api.DependencyAnalyser;
import com.github.gv2011.dependencyanalyser.api.DependencyDeclaration;
import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;
import com.github.gv2011.dependencyanalyser.api.PomDependencyDeclarations;
import com.github.gv2011.util.icol.ISet;

/**
 * Quick, throwaway driver connecting getDependencies (resolved) with
 * pomDependencyDeclarations (declared): for each of a leaf project's
 * resolved dependencies, is its version declared anywhere in the leaf's
 * own parent/BOM chain - and if so, by a pom this run considers "one's
 * own", or by a third party?
 *
 * <p>Package name is the signal: this is not part of the public API
 * surface. "Own" is decided by the crudest possible check - a hardcoded
 * substring match on groupId, see {@link #OWN_GROUP_ID_SUBSTRING} - on
 * purpose. No CLI parsing, no configuration, no cycle protection on the
 * parent/BOM walk. This class exists to exercise pomDependencyDeclarations
 * end to end against a real project, not to be the real tool.
 */
public final class Main {

  /**
   * Placeholder - replace with whatever substring actually identifies
   * "your own" groupIds for the project this is pointed at.
   */
  private static final String OWN_GROUP_ID_SUBSTRING = "xyz";

  private Main(){}

  public static void main(final String[] args) {
    if(args.length!=1) {
      System.err.println("Usage: Main <leaf-project-directory>");
      System.exit(1);
      return;
    }
    final Path leafDirectory = Paths.get(args[0]);
    final DependencyAnalyser analyser = DependencyAnalyser.instance();

    final ISet<Dependency> resolved = analyser.getDependencies(leafDirectory, Classpath.MAIN);
    final Map<ArtifactIdentity, DeclarationSource> declared = new HashMap<>();
    walk(analyser, analyser.pomDependencyDeclarations(leafDirectory), "<leaf>", true, declared);

    for(final Dependency dependency: resolved) {
      final String groupId = dependency.coordinates().identity().groupId();
      final String artifactId = dependency.coordinates().identity().artifactId();
      final DeclarationSource source = declared.get(dependency.coordinates().identity());
      final String status =
        source==null ? "NOT DECLARED (pure transitive resolution)"
        : source.own() ? "declared by OWN pom: " + source.declaringPom()
        : "declared by THIRD-PARTY pom: " + source.declaringPom()
      ;
      System.out.println(
        groupId + ":" + artifactId + ":" + dependency.coordinates().version() + " -> " + status
      );
    }
  }

  private record DeclarationSource(String declaringPom, boolean own) {}

  /**
   * No cycle protection: a parent or BOM chain that (incorrectly) refers
   * back to itself would recurse until it overflows the stack. Acceptable
   * for a throwaway driver; not for the real tool this becomes later.
   */
  private static void walk(
    final DependencyAnalyser analyser,
    final PomDependencyDeclarations pom,
    final String label,
    final boolean own,
    final Map<ArtifactIdentity, DeclarationSource> result
  ) {
    for(final DependencyDeclaration declaration: pom.dependencyDeclarations()) {
      // First declaration found wins: this pom's own management/dependencies
      // are walked before its parent, and a parent before its own BOM
      // imports are followed - close to Maven's real "nearest wins"
      // precedence, though not a faithful reproduction of it.
      result.putIfAbsent(
        declaration.coordinates().identity(),
        new DeclarationSource(label + " (" + declaration.coordinates().version() + ")", own)
      );
      if(declaration.isBomImport()) {
        final MavenCoordinates bomCoordinates = declaration.coordinates();
        final PomDependencyDeclarations bom = analyser.pomDependencyDeclarations(bomCoordinates);
        walk(analyser, bom, "BOM " + format(bomCoordinates), isOwn(bom), result);
      }
    }
    pom.parent().ifPresentDo(parentCoordinates -> {
      final PomDependencyDeclarations parentPom = analyser.pomDependencyDeclarations(parentCoordinates);
      walk(analyser, parentPom, "parent " + format(parentCoordinates), isOwn(parentPom), result);
    });
  }

  private static boolean isOwn(final PomDependencyDeclarations pom) {
    return pom.groupId().map(g -> g.contains(OWN_GROUP_ID_SUBSTRING)).orElse(false);
  }

  private static String format(final MavenCoordinates coordinates) {
    return coordinates.identity().groupId() + ":" + coordinates.identity().artifactId()
      + ":" + coordinates.version();
  }

}
