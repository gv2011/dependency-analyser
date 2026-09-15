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
 * purpose. No CLI parsing, no configuration. This class exists to
 * exercise pomDependencyDeclarations end to end against a real project,
 * not to be the real tool.
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
   * Recursively walks one pom's own declarations together with its BOM
   * imports and its parent, so {@code result} ends up with one entry per
   * artifact found across the whole chain.
   *
   * @param pom the pom currently being walked
   * @param label where {@code pom} came from in the walk (e.g.
   *   {@code "<leaf>"}, {@code "parent groupId:artifactId:version"},
   *   {@code "BOM groupId:artifactId:version"}) - recorded in each
   *   DeclarationSource so the final report can say where a version was
   *   declared, not just that it was
   * @param own whether {@code pom} counts as "one's own", per
   *   {@link #isOwn}
   * @param result accumulates one entry per artifact found so far
   */
  private static void walk(
    final DependencyAnalyser analyser,
    final PomDependencyDeclarations pom,
    final String label,
    final boolean own,
    final Map<ArtifactIdentity, DeclarationSource> result
  ) {
    // TODO no cycle protection: a parent or BOM chain that (incorrectly)
    // refers back to itself would recurse until it overflows the stack.
    // Acceptable for this throwaway driver; not for the real tool.
    for(final DependencyDeclaration declaration: pom.dependencyDeclarations()) {
      // TODO "first declaration found wins" is only a crude approximation
      // of Maven's real "nearest wins" precedence. Proper reporting
      // should show every declaration found for a given artifact, not
      // just the first, and let the reader judge a real conflict
      // themselves.
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
