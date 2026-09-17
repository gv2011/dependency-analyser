// Confirmed against Maven 3.9.9 source directly (DefaultModelBuilder):
// phase 1 (interpolation) and phase 2 (import merging, via
// importDependencyManagement) are separate steps on the same model
// object. ModelBuildingRequest.setTwoPhaseBuilding(true) stops after
// phase 1 and returns that interim result - interpolated, but import
// entries not yet replaced. Not wired into DependencyAnalyserImpl yet;
// this is still the exploration step, but the model-building mechanics
// themselves are now confirmed, not guessed.

package com.github.gv2011.dependencyanalyser.impl;

import java.io.File;
import java.util.Properties;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.resolution.ModelResolver;

final class ModelBuilderSketch {

  private ModelBuilderSketch(){}

  /**
   * The final, fully effective model: parent-inherited, interpolated,
   * dependencyManagement imports already merged in. Source for
   * coordinates(), parent(), dependencies() - NOT for boms(), since the
   * import entries this method needs are exactly what's gone by the
   * time this returns.
   */
  static Model buildEffectiveModel(final File pomFile, final ModelResolver resolver) {
    return build(pomFile, resolver, false).getEffectiveModel();
  }

  /**
   * The interim model: parent-inherited and interpolated (property
   * placeholders like {@code ${bom.version}} already resolved to real
   * values), but dependencyManagement imports NOT yet merged - a
   * {@code <scope>import</scope>} entry is still present as itself,
   * not replaced by what it points to. Source for boms(): this is the
   * only state where a BOM reference is both identifiable as an import
   * and has a real, usable version.
   *
   * <p>Needs no ModelResolver capable of resolving the imported BOM
   * itself - that resolution is exactly the phase-2 step this skips.
   * A resolver may still be needed for the parent, unless it's found
   * via relativePath.
   */
  static Model buildInterimModel(final File pomFile, final ModelResolver resolver) {
    return build(pomFile, resolver, true).getEffectiveModel();
  }

  private static ModelBuildingResult build(
    final File pomFile, final ModelResolver resolver, final boolean twoPhaseBuilding
  ) {
    final DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
    request.setPomFile(pomFile);
    request.setModelResolver(resolver);
    request.setSystemProperties(new Properties());
    request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
    request.setProcessPlugins(false);
    request.setTwoPhaseBuilding(twoPhaseBuilding);
    try {
      return new DefaultModelBuilderFactory().newInstance().build(request);
    }
    catch(final Exception e) { // narrow once the real exception type is confirmed in use
      throw new RuntimeException("Model building failed for " + pomFile, e);
    }
  }

}
