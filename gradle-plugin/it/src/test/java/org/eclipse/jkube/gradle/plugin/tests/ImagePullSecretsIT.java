package org.eclipse.jkube.gradle.plugin.tests;

import net.minidev.json.parser.ParseException;
import org.eclipse.jkube.kit.common.ResourceVerify;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ImagePullSecretsIT {
    @RegisterExtension
    final ITGradleRunnerExtension gradleRunner = new ITGradleRunnerExtension();

    @Test
    void k8sResource_whenRun_generatesK8sManifestsWithImagePullSecrets() throws IOException, ParseException {
        final BuildResult result = gradleRunner.withITProject("image-pull-secrets")
                .withArguments("k8sResource", "--stacktrace")
                .build();

        ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultKubernetesResourceFile(),
                gradleRunner.resolveFile("expected", "kubernetes.yml"));
        assertThat(result).extracting(BuildResult::getOutput).asString()
                .contains("Using resource templates from")
                .contains("Adding a default Deployment")
                .contains("Adding revision history limit to 2")
                .contains("validating");
    }

    @Test
    void ocResource_whenRun_generatesOpenShiftManifestsWithImagePullSecrets() throws IOException, ParseException {
        final BuildResult result = gradleRunner.withITProject("image-pull-secrets")
                .withArguments("ocResource", "--stacktrace")
                .build();

        ResourceVerify.verifyResourceDescriptors(gradleRunner.resolveDefaultOpenShiftResourceFile(),
                gradleRunner.resolveFile("expected", "openshift.yml"));
        assertThat(result).extracting(BuildResult::getOutput).asString()
                .contains("Using resource templates from")
                .contains("Adding a default Deployment")
                .contains("Adding revision history limit to 2")
                .contains("validating");
    }
}
