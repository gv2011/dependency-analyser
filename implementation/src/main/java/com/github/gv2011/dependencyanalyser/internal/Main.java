package com.github.gv2011.dependencyanalyser.internal;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.github.gv2011.dependencyanalyser.api.ArtifactIdentity;
import com.github.gv2011.dependencyanalyser.api.Classpath;
import com.github.gv2011.dependencyanalyser.api.Dependency;
import com.github.gv2011.dependencyanalyser.api.DependencyAnalyser;
import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;
import com.github.gv2011.dependencyanalyser.internal.PomChainWalker.DeclarationSource;
import com.github.gv2011.util.icol.ISet;

/**
 * Quick, throwaway command-line entry point for {@link PomChainWalker}:
 * parses one argument (a leaf project directory), runs the walk, and
 * prints one line per resolved dependency. The actual logic lives in
 * PomChainWalker/PomStep/PomRelation - this class is deliberately where
 * the "quick and dirty" stays: argument parsing and println formatting
 * only.
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
    final PomChainWalker walker =
      new PomChainWalker(analyser, groupId -> groupId.contains(OWN_GROUP_ID_SUBSTRING))
    ;
    final Map<ArtifactIdentity, DeclarationSource> declared =
      walker.declarationsReachableFrom(leafDirectory)
    ;

    for(final Dependency dependency: resolved) {
      final String groupId = dependency.coordinates().identity().groupId();
      final String artifactId = dependency.coordinates().identity().artifactId();
      final DeclarationSource source = declared.get(dependency.coordinates().identity());
      final String status =
        source==null ? "NOT DECLARED (pure transitive resolution)"
        : (source.own() ? "declared by OWN pom: " : "declared by THIRD-PARTY pom: ") + describe(source)
      ;
      System.out.println(
        groupId + ":" + artifactId + ":" + dependency.coordinates().version() + " -> " + status
      );
    }
  }

  private static String describe(final DeclarationSource source) {
    final StringBuilder location = new StringBuilder();
    if(source.path().isEmpty()) {
      location.append("<leaf>");
    }
    else {
      boolean first = true;
      for(final PomStep step: source.path()) {
        if(!first) {
          location.append(" -> ");
        }
        location.append(step.relation()).append(' ').append(format(step.coordinates()));
        first = false;
      }
    }
    return location.append(" (").append(source.version()).append(')').toString();
  }

  private static String format(final MavenCoordinates coordinates) {
    return coordinates.identity().groupId() + ":" + coordinates.identity().artifactId()
      + ":" + coordinates.version();
  }

}
