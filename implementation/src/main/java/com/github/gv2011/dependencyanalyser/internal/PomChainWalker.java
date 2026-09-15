package com.github.gv2011.dependencyanalyser.internal;

import static com.github.gv2011.util.BeanUtils.beanBuilder;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import com.github.gv2011.dependencyanalyser.api.ArtifactIdentity;
import com.github.gv2011.dependencyanalyser.api.DependencyAnalyser;
import com.github.gv2011.dependencyanalyser.api.DependencyDeclaration;
import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;
import com.github.gv2011.dependencyanalyser.api.PomDependencyDeclarations;
import com.github.gv2011.dependencyanalyser.api.Version;
import com.github.gv2011.util.icol.ICollections;
import com.github.gv2011.util.icol.IList;

/**
 * Walks a leaf project's own dependency declarations together with its
 * whole reachable parent/BOM-import chain, and reports, for every
 * artifact a version is declared for anywhere in that chain, the first
 * path found to it and whether the pom at the end of that path counts
 * as "one's own" per the groupId predicate given at construction.
 */
public final class PomChainWalker {

  private final DependencyAnalyser analyser;
  private final Predicate<String> ownGroupId;

  /**
   * @param ownGroupId decides, given a pom's own stated groupId, whether
   *   that pom counts as "one's own" - the leaf project itself always
   *   does, regardless of what this predicate says about its groupId
   */
  public PomChainWalker(final DependencyAnalyser analyser, final Predicate<String> ownGroupId) {
    this.analyser = analyser;
    this.ownGroupId = ownGroupId;
  }

  public Map<ArtifactIdentity, DeclarationSource> declarationsReachableFrom(final Path leafDirectory) {
    final Map<ArtifactIdentity, DeclarationSource> result = new HashMap<>();
    final PomDependencyDeclarations leaf = analyser.pomDependencyDeclarations(leafDirectory);
    walk(leaf, ICollections.<PomStep>listBuilder().build(), true, result);
    return result;
  }

  /**
   * @param path how this declaration's pom was reached from the leaf;
   *   empty means the leaf itself
   * @param own whether the declaring pom (path's last step, or the leaf
   *   if path is empty) counts as "one's own"
   */
  public record DeclarationSource(IList<PomStep> path, Version version, boolean own) {}

  // TODO no cycle protection: a parent or BOM chain that (incorrectly)
  // refers back to itself would recurse until it overflows the stack.
  private void walk(
    final PomDependencyDeclarations pom,
    final IList<PomStep> path,
    final boolean own,
    final Map<ArtifactIdentity, DeclarationSource> result
  ) {
    for(final DependencyDeclaration declaration: pom.dependencyDeclarations()) {
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
        final PomDependencyDeclarations bom = analyser.pomDependencyDeclarations(bomCoordinates);
        walk(bom, extend(path, PomRelation.BOM_IMPORT, bomCoordinates), isOwn(bom), result);
      }
    }
    pom.parent().ifPresentDo(parentCoordinates -> {
      final PomDependencyDeclarations parentPom = analyser.pomDependencyDeclarations(parentCoordinates);
      walk(parentPom, extend(path, PomRelation.PARENT, parentCoordinates), isOwn(parentPom), result);
    });
  }

  private boolean isOwn(final PomDependencyDeclarations pom) {
    return pom.groupId().map(ownGroupId::test).orElse(false);
  }

  private static IList<PomStep> extend(
    final IList<PomStep> path, final PomRelation relation, final MavenCoordinates coordinates
  ) {
    return path.addElement(
      beanBuilder(PomStep.class)
        .set(PomStep::relation).to(relation)
        .set(PomStep::coordinates).to(coordinates)
        .build()
    );
  }

}
