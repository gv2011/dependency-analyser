// SKETCH — not verified against actual Maven 3.9.9 source/compiled. Needs
// checking before trusting it. Written under tight context budget as a
// starting point for the next session, not a finished implementation.

package com.github.gv2011.dependencyanalyser.impl;

import java.io.File;
import java.util.Properties;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.resolution.ModelResolver;

/**
 * PLAN:
 * - getCoordinates()/parent()/dependencies(): use ModelBuildingResult's
 *   final effective Model (result.getEffectiveModel()) - straightforward,
 *   same as help:effective-pom conceptually, but as a real object, no
 *   XML round-trip.
 * - boms(): the effective Model has ALREADY had import entries replaced
 *   (DefaultModelBuildingRequest runs the full pipeline, which includes
 *   DependencyManagementImporter - same erasure as help:effective-pom).
 *   NOT YET SOLVED: need either
 *     (a) ModelBuildingResult.getRawModel(String modelId) per ancestor -
 *         check whether THAT is pre-import-merge but already
 *         interpolated (needs checking - this is the main open
 *         question), or
 *     (b) call org.apache.maven.model.interpolation.ModelInterpolator
 *         directly on the raw model, supplying properties accumulated
 *         from the (already-resolved) parent chain, bypassing full
 *         ModelBuilder for this one step.
 *   (a) is less code if it works; (b) is more certain to give the right
 *   semantics but needs finding the right interpolator class/wiring for
 *   this Maven version.
 *
 * ModelResolver: needs a real implementation to let the builder fetch
 * parent/import POMs from the local/remote repo - likely
 * DefaultModelResolver or something wired through this project's
 * existing embedded-Maven session (MavenApiImpl / MavenCliBase already
 * has a working RepositorySystem set up - reuse its container/session
 * rather than building a new one from scratch).
 */
final class ModelBuilderSketch {

  private ModelBuilderSketch(){}

  static ModelBuildingResult buildEffectiveModel(final File pomFile, final ModelResolver resolver) {
    final ModelBuilder builder = new DefaultModelBuilderFactory().newInstance();
    final ModelBuildingRequest request = new DefaultModelBuildingRequest();
    request.setPomFile(pomFile);
    request.setModelResolver(resolver);
    // Needed for property interpolation involving system properties:
    request.setSystemProperties(new Properties());
    // Likely also needed - check actual defaults/requirements:
    // request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
    // request.setProcessPlugins(false); // we don't need build/plugin info

    try {
      final ModelBuildingResult result = builder.build(request);
      // result.getEffectiveModel() -> fully merged Model (parent(),
      //   coordinates(), dependencies() all derivable from this)
      // result.getRawModel(String modelId) -> CHECK: is this
      //   interpolated? Does it still show <scope>import</scope>
      //   entries in dependencyManagement? This is the key thing to
      //   verify next session.
      return result;
    }
    catch(final Exception e) { // narrow this once the real exception type is confirmed
      throw new RuntimeException("Model building failed for " + pomFile, e);
    }
  }

}
