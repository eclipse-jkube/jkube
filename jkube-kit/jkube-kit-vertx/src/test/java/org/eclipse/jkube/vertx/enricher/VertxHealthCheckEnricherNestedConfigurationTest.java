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
package org.eclipse.jkube.vertx.enricher;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.vertx.enricher.VertxHealthCheckEnricher.VERTX_MAVEN_PLUGIN_ARTIFACT;
import static org.eclipse.jkube.vertx.enricher.VertxHealthCheckEnricher.VERTX_MAVEN_PLUGIN_GROUP;


@RunWith(Parameterized.class)
public class VertxHealthCheckEnricherNestedConfigurationTest {

  @Parameterized.Parameters(name = "{index} with plugin configuration present with artifactId {0}, then should generate health probes")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[] { "kubernetes-maven-plugin" },
        new Object[] { "openshift-maven-plugin" },
        new Object[] { "org.eclipse.jkube.kubernetes.gradle.plugin" },
        new Object[] { "org.eclipse.jkube.openshift.gradle.plugin" },
        new Object[] { "org.eclipse.jkube.gradle.plugin.KubernetesPlugin" },
        new Object[] { "org.eclipse.jkube.gradle.plugin.OpenShiftPlugin" }
    );
  }

  @Parameterized.Parameter
  public String pluginArtifactId;

  @Test
  public void create_whenPluginNestedConfigurationPresent_shouldGenerateProbesAsConfigured() {
    // Given
    VertxHealthCheckEnricher vertxHealthCheckEnricher = new VertxHealthCheckEnricher(createNewEnricherContextWithPlugin(pluginArtifactId));
    KubernetesListBuilder klb = new KubernetesListBuilder();
    klb.addToItems(createNewDeployment());

    // When
    vertxHealthCheckEnricher.create(PlatformMode.kubernetes, klb);

    // Then
    assertThat(klb.buildItems())
        .hasSize(1)
        .asList()
        .first(InstanceOfAssertFactories.type(Deployment.class))
        .extracting(Deployment::getSpec)
        .extracting(DeploymentSpec::getTemplate)
        .extracting(PodTemplateSpec::getSpec)
        .extracting(PodSpec::getContainers)
        .asList()
        .hasSize(1)
        .first(InstanceOfAssertFactories.type(Container.class))
        .hasFieldOrPropertyWithValue("livenessProbe.httpGet.path", "/health/live")
        .hasFieldOrPropertyWithValue("readinessProbe.httpGet.path", "/health/ready");
  }

  private JKubeEnricherContext createNewEnricherContextWithPlugin(String pluginArtifactId) {
    JavaProject javaProject = JavaProject.builder()
        .groupId("org.testing")
        .artifactId("test-project")
        .version("0.0.1")
        .plugin(Plugin.builder()
            .groupId(VERTX_MAVEN_PLUGIN_GROUP)
            .artifactId(VERTX_MAVEN_PLUGIN_ARTIFACT)
            .build())
        .plugin(Plugin.builder()
            .groupId("org.eclipse.jkube.testing")
            .artifactId(pluginArtifactId)
            .version("1.0.0")
            .configuration(createNestedEnricherConfigurationMap())
            .build())
        .build();
    return JKubeEnricherContext.builder()
        .log(new KitLogger.SilentLogger())
        .project(javaProject)
        .build();
  }

  private Map<String, Object> createNestedEnricherConfigurationMap() {
    Map<String, Object> readiness = new HashMap<>();
    readiness.put("path", "/health/ready");
    Map<String, Object> liveness = new HashMap<>();
    liveness.put("path", "/health/live");
    Map<String, Object> jkubeVertxHealthCheck = new HashMap<>();
    jkubeVertxHealthCheck.put("readiness", readiness);
    jkubeVertxHealthCheck.put("liveness", liveness);
    Map<String, Object> enricherConfig = new HashMap<>();
    enricherConfig.put("jkube-healthcheck-vertx", jkubeVertxHealthCheck);
    Map<String, Object> enricher = new HashMap<>();
    enricher.put("config", enricherConfig);
    Map<String, Object> configuration = new HashMap<>();
    configuration.put("enricher", enricher);

    return configuration;
  }

  private Deployment createNewDeployment() {
    return new DeploymentBuilder()
        .withNewMetadata().withName("test-deploy").endMetadata()
        .withNewSpec()
        .withNewTemplate()
        .withNewSpec()
        .addNewContainer()
        .withName("test-container")
        .withImage("test-img:latest")
        .endContainer()
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();
  }
}
