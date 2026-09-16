package com.github.gv2011.dependencyanalyser.internal;

import static com.github.gv2011.util.BeanUtils.beanBuilder;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import com.github.gv2011.dependencyanalyser.api.ArtifactIdentity;
import com.github.gv2011.dependencyanalyser.api.DependencyAnalyser;
import com.github.gv2011.dependencyanalyser.api.DependencyDeclaration;
import com.github.gv2011.dependencyanalyser.api.DirectlyDeclaredDependencies;
import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;
import com.github.gv2011.dependencyanalyser.api.Version;
import com.github.gv2011.util.icol.ICollections;
import com.github.gv2011.util.icol.IList;

/**
 * Walks a leaf project's own dependency declarations together with its
 * whole reachable parent/BOM-import chain, and reports, for every
 * artifact a version is declared for anywhere in that chain, the first
 * path found to it and whether the project at the end of that path
 * counts as "one's own" per the groupId predicate given at construction.
 */
public final class PomChainWalker {

  private final DependencyAnalyser analyser;
  private final Predicate<String> ownGroupId;

  /**
   * @param ownGroupId decides, given a project's own groupId, whether
   *   that project counts as "one's own" - the leaf project itself
   *   always does, regardless of what this predicate says about its
   *   groupId
   */
  public PomChainWalker(final DependencyAnalyser analyser, final Predicate<String> ownGroupId) {
    this.analyser = analyser;
    this.ownGroupId = ownGroupId;
  }

  public Map<ArtifactIdentity, DeclarationSource> declarationsReachableFrom(final Path leafDirectory) {
    final Map<ArtifactIdentity, DeclarationSource> result = new HashMap<>();
    final DirectlyDeclaredDependencies leaf = analyser.directlyDeclaredDependencies(leafDirectory);
    walk(leaf, ICollections.<PomStep>listBuilder().build(), result);
    return result;
  }

  /**
   * @param path how this declaration's project was reached from the
   *   leaf; empty means the leaf itself
   * @param own whether the declaring project (path's last step, or the
   *   leaf if path is empty) counts as "one's own"
   */
  public record DeclarationSource(IList<PomStep> path, Version version, boolean own) {}

  // TODO no cycle protection: a parent or BOM chain that (incorrectly)
  // refers back to itself would recurse until it overflows the stack.
  private void walk(
    final DirectlyDeclaredDependencies project,
    final IList<PomStep> path,
    final Map<ArtifactIdentity, DeclarationSource> result
  ) {
    // An empty path means project is the leaf itself, which counts as
    // "one's own" unconditionally, not via isOwn(project).
    final boolean own = path.isEmpty() || isOwn(project);
    for(final DependencyDeclaration declaration: project.dependencyDeclarations()) {
      // TODO "first declaration found wins" is only a crude approximation
      // of Maven's real "nearest wins" precedence. Proper reporting
      // should show every declaration found for a given artifact, not
      // just the first, and let the reader judge a real conflict
      // themselves.
      result.putIfAbsent(
        declaration.coordinates().identity(),
        new DeclarationSource(path, declaration.coordinates().version(), own)
      );
      if(declaration.isBomImport()) {
        final MavenCoordinates bomCoordinates = declaration.coordinates();
        final DirectlyDeclaredDependencies bom = analyser.directlyDeclaredDependencies(bomCoordinates);
        walk(bom, extend(path, PomRelation.BOM_IMPORT, bomCoordinates), result);
      }
    }
    project.parent().ifPresentDo(parentCoordinates -> {
      final DirectlyDeclaredDependencies parentProject =
        analyser.directlyDeclaredDependencies(parentCoordinates)
      ;
      walk(parentProject, extend(path, PomRelation.PARENT, parentCoordinates), result);
    });
  }

  private boolean isOwn(final DirectlyDeclaredDependencies project) {
    return ownGroupId.test(project.mavenCoordinates().identity().groupId());
  }

  private static IList<PomStep> extend(
    final IList<PomStep> path, final PomRelation relation, final MavenCoordinates projectCoordinates
  ) {
    return path.addElement(
      beanBuilder(PomStep.class)
        .set(PomStep::relation).to(relation)
        .set(PomStep::projectCoordinates).to(projectCoordinates)
        .build()
    );
  }

}
