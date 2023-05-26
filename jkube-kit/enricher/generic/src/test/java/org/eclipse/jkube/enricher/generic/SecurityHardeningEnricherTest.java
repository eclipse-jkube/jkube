/**
 * Copyright (c) 2019 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at:
 *
 *     https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.jkube.enricher.generic;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1.CronJobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.CronJobSpec;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import io.fabric8.kubernetes.api.model.batch.v1.JobTemplateSpec;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigSpec;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ThrowingConsumer;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SecurityHardeningEnricherTest {

  private JKubeEnricherContext context;
  private KubernetesListBuilder kubernetesListBuilder;

  @BeforeEach
  void setUp() {
    context = JKubeEnricherContext.builder()
      .log(spy(new KitLogger.SilentLogger()))
      .project(JavaProject.builder().build())
      .build();
    kubernetesListBuilder = new KubernetesListBuilder()
      .addToItems(new PodBuilder().withNewMetadata().withName("pod").endMetadata()
        .withNewSpec().addNewContainer().endContainer().endSpec().build())
      .addToItems(new PodBuilder().withNewMetadata().withName("multiple-containers").endMetadata()
        .withNewSpec().addNewContainer().endContainer().addNewContainer().endContainer().endSpec().build())
      .addToItems(new PodBuilder().withNewMetadata().withName("existing-security-settings").endMetadata().withNewSpec()
        .withAutomountServiceAccountToken()
        .addNewContainer().withNewSecurityContext()
        .withPrivileged().withAllowPrivilegeEscalation().withRunAsUser(0L).withRunAsNonRoot(false)
        .endSecurityContext().endContainer().endSpec().build())
      .addToItems(new DeploymentBuilder().withNewMetadata().withName("deployment").endMetadata()
        .withNewSpec().withNewTemplate().withNewSpec().addNewContainer().endContainer().endSpec().endTemplate().endSpec().build())
      .addToItems(new DeploymentConfigBuilder().withNewMetadata().withName("deployment-config").endMetadata()
        .withNewSpec().withNewTemplate().withNewSpec().addNewContainer().endContainer().endSpec().endTemplate().endSpec().build())
      .addToItems(new JobBuilder().withNewMetadata().withName("job").endMetadata()
        .withNewSpec().withNewTemplate().withNewSpec().addNewContainer().endContainer().endSpec().endTemplate().endSpec().build())
      .addToItems(new CronJobBuilder().withNewMetadata().withName("cron-job").endMetadata()
        .withNewSpec().withNewJobTemplate().withNewSpec().withNewTemplate().withNewSpec()
        .addNewContainer().endContainer().endSpec().endTemplate().endSpec().endJobTemplate().endSpec().build())
      .addToItems(new PodBuilder().withNewMetadata().withName("latest-image").endMetadata()
        .withNewSpec().addNewContainer().withName("latest-image").withImage("image:latest").endContainer().endSpec().build());
  }

  @Test
  void enforcesSecurityOnPods() {
    new SecurityHardeningEnricher(context).enrich(PlatformMode.kubernetes, kubernetesListBuilder);
    assertThat(kubernetesListBuilder.buildItems())
      .element(0, InstanceOfAssertFactories.type(Pod.class))
      .hasFieldOrPropertyWithValue("metadata.name", "pod")
      .extracting(Pod::getSpec)
      .satisfies(POD_SPEC_SECURITY)
      .extracting(PodSpec::getContainers, InstanceOfAssertFactories.list(Container.class))
      .singleElement()
      .satisfies(CONTAINER_SECURITY);
  }

  @Test
  void enforcesSecurityOnMultipleContainers() {
    new SecurityHardeningEnricher(context).enrich(PlatformMode.kubernetes, kubernetesListBuilder);
    assertThat(kubernetesListBuilder.buildItems())
      .element(1, InstanceOfAssertFactories.type(Pod.class))
      .hasFieldOrPropertyWithValue("metadata.name", "multiple-containers")
      .extracting(Pod::getSpec)
      .satisfies(POD_SPEC_SECURITY)
      .extracting(PodSpec::getContainers, InstanceOfAssertFactories.list(Container.class))
      .hasSize(2)
      .allSatisfy(CONTAINER_SECURITY);
  }

  @Test
  void overwritesExistingSecurityContext() {
    new SecurityHardeningEnricher(context).enrich(PlatformMode.kubernetes, kubernetesListBuilder);
    assertThat(kubernetesListBuilder.buildItems())
      .element(2, InstanceOfAssertFactories.type(Pod.class))
      .hasFieldOrPropertyWithValue("metadata.name", "existing-security-settings")
      .extracting(Pod::getSpec)
      .satisfies(POD_SPEC_SECURITY)
      .extracting(PodSpec::getContainers, InstanceOfAssertFactories.list(Container.class))
      .singleElement()
      .satisfies(CONTAINER_SECURITY);
  }

  @Test
  void enforcesSecurityOnDeployments() {
    new SecurityHardeningEnricher(context).enrich(PlatformMode.kubernetes, kubernetesListBuilder);
    assertThat(kubernetesListBuilder.buildItems())
      .element(3, InstanceOfAssertFactories.type(Deployment.class))
      .hasFieldOrPropertyWithValue("metadata.name", "deployment")
      .extracting(Deployment::getSpec).extracting(DeploymentSpec::getTemplate).extracting(PodTemplateSpec::getSpec)
      .satisfies(POD_SPEC_SECURITY)
      .extracting(PodSpec::getContainers, InstanceOfAssertFactories.list(Container.class))
      .singleElement()
      .satisfies(CONTAINER_SECURITY);
  }

  @Test
  void enforcesSecurityOnDeploymentConfigs() {
    new SecurityHardeningEnricher(context).enrich(PlatformMode.kubernetes, kubernetesListBuilder);
    assertThat(kubernetesListBuilder.buildItems())
      .element(4, InstanceOfAssertFactories.type(DeploymentConfig.class))
      .hasFieldOrPropertyWithValue("metadata.name", "deployment-config")
      .extracting(DeploymentConfig::getSpec).extracting(DeploymentConfigSpec::getTemplate).extracting(PodTemplateSpec::getSpec)
      .satisfies(POD_SPEC_SECURITY)
      .extracting(PodSpec::getContainers, InstanceOfAssertFactories.list(Container.class))
      .singleElement()
      .satisfies(CONTAINER_SECURITY);
  }

  @Test
  void enforcesSecurityOnJobs() {
    new SecurityHardeningEnricher(context).enrich(PlatformMode.kubernetes, kubernetesListBuilder);
    assertThat(kubernetesListBuilder.buildItems())
      .element(5, InstanceOfAssertFactories.type(Job.class))
      .hasFieldOrPropertyWithValue("metadata.name", "job")
      .extracting(Job::getSpec).extracting(JobSpec::getTemplate).extracting(PodTemplateSpec::getSpec)
      .satisfies(POD_SPEC_SECURITY)
      .extracting(PodSpec::getContainers, InstanceOfAssertFactories.list(Container.class))
      .singleElement()
      .satisfies(CONTAINER_SECURITY);
  }

  @Test
  void enforcesSecurityOnCronJobs() {
    new SecurityHardeningEnricher(context).enrich(PlatformMode.kubernetes, kubernetesListBuilder);
    assertThat(kubernetesListBuilder.buildItems())
      .element(6, InstanceOfAssertFactories.type(CronJob.class))
      .hasFieldOrPropertyWithValue("metadata.name", "cron-job")
      .extracting(CronJob::getSpec).extracting(CronJobSpec::getJobTemplate).extracting(JobTemplateSpec::getSpec)
      .extracting(JobSpec::getTemplate).extracting(PodTemplateSpec::getSpec)
      .satisfies(POD_SPEC_SECURITY)
      .extracting(PodSpec::getContainers, InstanceOfAssertFactories.list(Container.class))
      .singleElement()
      .satisfies(CONTAINER_SECURITY);
  }

  @Test
  void warnsAboutImageWithLatestTag() {
    new SecurityHardeningEnricher(context).enrich(PlatformMode.kubernetes, kubernetesListBuilder);
    verify(context.getLog()).warn(
      "Container %s has an image with tag 'latest', it's recommended to use a fixed tag or a digest instead",
      "latest-image"
    );
  }

  @Test
  void ignoresWarningAboutImageWithLatestTagForSnapshotProject() {
    context = context.toBuilder().project(context.getProject().toBuilder().version("1.33.7-SNAPSHOT").build()).build();
    new SecurityHardeningEnricher(context).enrich(PlatformMode.kubernetes, kubernetesListBuilder);
    verify(context.getLog(), times(0)).warn(
      eq("Container %s has an image with tag 'latest', it's recommended to use a fixed tag or a digest instead"),
      anyString()
    );
  }

  private static final ThrowingConsumer<PodSpec> POD_SPEC_SECURITY = podSpec ->
    assertThat(podSpec)
      .hasFieldOrPropertyWithValue("automountServiceAccountToken", false);
  private static final ThrowingConsumer<Container> CONTAINER_SECURITY = container ->
    assertThat(container)
      .hasFieldOrPropertyWithValue("securityContext.privileged", false)
      .hasFieldOrPropertyWithValue("securityContext.allowPrivilegeEscalation", false)
      .hasFieldOrPropertyWithValue("securityContext.runAsUser", 10000L)
      .hasFieldOrPropertyWithValue("securityContext.runAsNonRoot", true)
      .hasFieldOrPropertyWithValue("securityContext.seccompProfile.type", "RuntimeDefault")
      .extracting("securityContext.capabilities.drop").asList()
      .containsExactlyInAnyOrder("NET_RAW", "ALL");
}
