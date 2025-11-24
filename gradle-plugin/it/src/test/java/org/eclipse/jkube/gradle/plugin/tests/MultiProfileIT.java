package org.eclipse.jkube.gradle.plugin.tests;

import java.io.IOException;

import org.eclipse.jkube.kit.common.ResourceVerify;

import net.minidev.json.parser.ParseException;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class MultiProfileIT {
  @RegisterExtension
  private final ITGradleRunnerExtension gradleRunner = new ITGradleRunnerExtension();

  @Test
  void ocResource_whenRunMutliFragments_generatesManifests() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("multi-env-same-kind")
        .withArguments("build", "ocResource")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftResourceFile(),
        gradleRunner.resolveFile("expected", "openshift.yml"));
  }

  @Test
  void ocResource_whenRunMutliFragmentsProfileOverridden_generatesManifests() throws IOException, ParseException {
    // When
    final BuildResult result = gradleRunner.withITProject("multi-env-same-kind-profile-overridden")
        .withArguments("build", "ocResource")
        .build();
    // Then
    ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftResourceFile(),
        gradleRunner.resolveFile("expected", "openshift.yml"));
  }
}
