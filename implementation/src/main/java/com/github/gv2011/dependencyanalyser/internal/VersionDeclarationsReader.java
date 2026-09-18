package com.github.gv2011.dependencyanalyser.internal;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import com.github.gv2011.dependencyanalyser.api.Dependency;
import com.github.gv2011.dependencyanalyser.api.DependencyAnalyser;
import com.github.gv2011.dependencyanalyser.api.MavenCoordinates;
import com.github.gv2011.dependencyanalyser.api.Project;
import com.github.gv2011.dependencyanalyser.api.Repository;
import com.github.gv2011.dependencyanalyser.api.VersionDeclaration;
import com.github.gv2011.util.icol.ICollections;
import com.github.gv2011.util.icol.ISet;
import com.github.gv2011.util.icol.ISet.Builder;


/**
 * Finds out where the versions of the dependencies of a Maven artifact are declared.
 */
public final class VersionDeclarationsReader {

  private final DependencyAnalyser analyser;

  public VersionDeclarationsReader(final DependencyAnalyser analyser) {
    this.analyser = analyser;
  }


  /**
   * VersionDeclaration together with it's location.
   */
  public record LocatedDeclaration(MavenCoordinates declaringProject, VersionDeclaration declaration) {}


  /**
   * @return the version declarations of the given project, both locally declared and inherited.
   */
  public ISet<LocatedDeclaration> getVersionDeclarations(final Path projectDirectory) {
    final Project project = analyser.getProject(projectDirectory, ICollections.<Repository>listBuilder().build());
    final Builder<LocatedDeclaration> collectedLocations = ICollections.setBuilder();
    final Set<MavenCoordinates> doneProjects = new HashSet<>();
    getVersionDeclarations(project, doneProjects, collectedLocations);
    return collectedLocations.build();
  }

  private void getVersionDeclarations(
    final Project project,
    final Set<MavenCoordinates> doneProjects,
    final Builder<LocatedDeclaration> collectedLocations
  ) {

    if(!doneProjects.contains(project.coordinates())) {
      //prevent infinitive loops if the graph contains cycles:
      doneProjects.add(project.coordinates());
      for(final VersionDeclaration declaration: project.getVersionDeclarations()) {
        collectedLocations.add(new LocatedDeclaration(project.coordinates(), declaration));
      }
      // additionalRepositories() already covers what this project itself
      // declares, on top of what was seeded in - see its own javadoc.
      // Passing it straight on is what keeps repository knowledge
      // flowing forward along this one branch of the walk, without
      // leaking sideways to a sibling.
      for(final Dependency bom: project.boms()) {
        getVersionDeclarations(
          analyser.getProject(bom.coordinates(), project.additionalRepositories()), doneProjects, collectedLocations
        );
      }
      project.parent().ifPresentDo(parentCoordinates ->
        getVersionDeclarations(
          analyser.getProject(parentCoordinates, project.additionalRepositories()), doneProjects, collectedLocations
        )
      );
    }
  }

}
