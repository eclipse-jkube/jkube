/*
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
package org.eclipse.jkube.wildfly.jar.enricher;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerFluent;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.common.Dependency;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.common.util.Serialization;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


class WildflyJARHealthCheckEnricherTest {

    protected JKubeEnricherContext context;

    private JavaProject project;

    @BeforeEach
    public void setUp() {
       project = JavaProject.builder().build();
       context = JKubeEnricherContext.builder()
               .project(project)
               .build();
    }
    private void setupExpectations(Map<String, Object> bootableJarConfig, Map<String, Map<String, Object>> jkubeConfig) {
      Plugin plugin = Plugin.builder().artifactId("wildfly-jar-maven-plugin").groupId("org.wildfly.plugins")
          .configuration(bootableJarConfig).build();
        List<Plugin> lst = new ArrayList<>();
        lst.add(plugin);
        ProcessorConfig c = new ProcessorConfig(null, null, jkubeConfig);
        Configuration.ConfigurationBuilder configBuilder = Configuration.builder();
        configBuilder.processorConfig(c);
        context = context.toBuilder()
                .project(context.getProject().toBuilder()
                .plugins(lst).build())
                .processorConfig(c)
                .build();
    }

    private void setupExpectations(Map<String, Map<String, Object>> jkubeConfig) {
        ProcessorConfig c = new ProcessorConfig(null, null, jkubeConfig);
        Configuration.ConfigurationBuilder configBuilder = Configuration.builder();
        configBuilder.processorConfig(c);
        context = context.toBuilder()
                .processorConfig(c)
                .build();
    }

    @Test
    @DisplayName("with default configuration, should not add probes")
    void defaultConfiguration_shouldNotAddProbes() {
        setupExpectations(Collections.emptyMap(), Collections.emptyMap());
        WildflyJARHealthCheckEnricher enricher = new WildflyJARHealthCheckEnricher(context);
        assertNoProbesAdded(enricher);
    }

    @Test
    @DisplayName("cloud configuration with wildfly jar version before 25.0.0, should not add startup probe")
    void cloudConfiguration_withWildflyJarBefore25_0shouldNotAdd_startupProbe() {
        wildFlyJarDependencyWithVersion("24.1.1.Final");

        Map<String, Object> config = new HashMap<>();
        config.put("cloud", null);
        setupExpectations(config, Collections.emptyMap());
        WildflyJARHealthCheckEnricher enricher = new WildflyJARHealthCheckEnricher(context);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertHttpGet(livenessProbe, "HTTP","/health/live", 9990);

        Probe readinessProbe = enricher.getReadinessProbe();
        assertHttpGet(readinessProbe, "HTTP","/health/ready", 9990);

        Probe startupProbe = enricher.getStartupProbe();
        assertThat(startupProbe).isNull();
    }

    @Test
    @DisplayName("cloud configuration with wildfly jar version after 25.0, should not add startup probe")
    void cloudConfiguration_withWildflyJarAfter25_0shouldAdd_startupProbe() {
        wildFlyJarDependencyWithVersion("26.1.1.Final");

        Map<String, Object> config = new HashMap<>();
        config.put("cloud", null);
        setupExpectations(config, Collections.emptyMap());
        WildflyJARHealthCheckEnricher enricher = new WildflyJARHealthCheckEnricher(context);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertHttpGet(livenessProbe, "HTTP", "/health/live", 9990);

        Probe readinessProbe = enricher.getReadinessProbe();
        assertHttpGet(readinessProbe, "HTTP", "/health/ready", 9990);

        Probe startupProbe = enricher.getStartupProbe();
        assertHttpGet(startupProbe, "HTTP", "/health/started", 9990);
    }

    @Test
    @DisplayName("custom configuration with wildfly jar version before 25.0, should not add startup probe")
    void withCustomConfigurationComingFromConf_withWildflyJarBefore25_0shouldNotAdd_startupProbe() {
        wildFlyJarDependencyWithVersion("24.1.1.Final");

        Map<String, Object> jarConfig = new HashMap<>();
        jarConfig.put("cloud", null);
        Map<String, Map<String, Object>> config = createFakeConfig(
                 "{\"readinessPath\":\"/foo/ready\","
                + "\"livenessPath\":\"/foo/live\","
                + "\"startupPath\":\"/foo/started\","
                + "\"port\":\"8080\","
                + "\"scheme\":\"https\","
                + "\"livenessInitialDelay\":\"99\","
                + "\"readinessInitialDelay\":\"77\","
                + "\"startupInitialDelay\":\"57\","
                + "\"failureThreshold\":\"27\","
                + "\"successThreshold\":\"10\","
                + "\"periodSeconds\":\"15\""
                + "}");
        setupExpectations(jarConfig, config);

        WildflyJARHealthCheckEnricher enricher = new WildflyJARHealthCheckEnricher(context);
        Probe livenessProbe = enricher.getLivenessProbe();
        assertProbeAdded(livenessProbe, "/foo/live", 8080, 99, 27, 10, 15);

        Probe readinessProbe = enricher.getReadinessProbe();
        assertProbeAdded(readinessProbe, "/foo/ready", 8080, 77, 27, 10, 15);

        Probe startupProbe = enricher.getStartupProbe();
        assertThat(startupProbe).isNull();
    }

    @Test
    @DisplayName("custom configuration with wildfly jar version after 25.0, should add startup probe")
    void withCustomConfigurationComingFromConf_withWildflyJarAfter25_0shouldAdd_startupProbe() {
        wildFlyJarDependencyWithVersion("26.1.1.Final");
        Map<String, Object> jarConfig = new HashMap<>();
        jarConfig.put("cloud", null);
        Map<String, Map<String, Object>> config = createFakeConfig(
                 "{\"readinessPath\":\"/foo/ready\","
                + "\"livenessPath\":\"/foo/live\","
                + "\"startupPath\":\"/foo/started\","
                + "\"port\":\"1234\","
                + "\"scheme\":\"https\","
                + "\"livenessInitialDelay\":\"99\","
                + "\"readinessInitialDelay\":\"77\","
                + "\"startupInitialDelay\":\"10\","
                + "\"failureThreshold\":\"3\","
                + "\"successThreshold\":\"1\","
                + "\"periodSeconds\":\"10\""
                + "}");
        setupExpectations(jarConfig, config);

        WildflyJARHealthCheckEnricher enricher = new WildflyJARHealthCheckEnricher(context);
        Probe livenessProbe = enricher.getLivenessProbe();
        assertProbeAdded(livenessProbe, "/foo/live", 1234, 99, 3, 1, 10);

        Probe readinessProbe = enricher.getReadinessProbe();
        assertProbeAdded(readinessProbe, "/foo/ready", 1234, 77, 3, 1, 10);

        Probe startupProbe = enricher.getStartupProbe();
        assertProbeAdded(startupProbe, "/foo/started", 1234, 10, 3, 1, 10);
    }

    @Test
    @DisplayName("with negative port, should disable health checks")
    void withNegativePort_shouldDisableHealth() {
        Map<String, Object> jarConfig = new HashMap<>();
        jarConfig.put("cloud", null);
        Map<String, Map<String, Object>> config = createFakeConfig("{"
                + "\"port\":\"-1\""
                + "}");
        setupExpectations(jarConfig, config);

        WildflyJARHealthCheckEnricher enricher = new WildflyJARHealthCheckEnricher(context);
        assertNoProbesAdded(enricher);
    }

    @Test
    @DisplayName("when true health is enforced with wildfly jar version before 25.0, should not add startup probe")
    void enforceTrueHealth_withWildflyJarBefore25_0shouldNotAdd_startupProbe() {
        wildFlyJarDependencyWithVersion("24.1.1.Final");
        Map<String, Map<String, Object>> config = createFakeConfig("{"
                + "\"enforceProbes\":\"true\""
                + "}");
        setupExpectations(config);

        WildflyJARHealthCheckEnricher enricher = new WildflyJARHealthCheckEnricher(context);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertHttpGet(livenessProbe, "HTTP", "/health/live",9990);

        Probe readinessProbe = enricher.getReadinessProbe();
        assertHttpGet(readinessProbe, "HTTP", "/health/ready", 9990);

        Probe startupProbe = enricher.getStartupProbe();
        assertThat(startupProbe).isNull();
    }

    @Test
    @DisplayName("when true health is enforced with wildfly jar version after 25.0, should add startup probe")
    void enforceTrueHealth_withWildflyJarAfter25_0shouldAddStartupProbe() {
        wildFlyJarDependencyWithVersion("26.1.1.Final");
        Map<String, Map<String, Object>> config = createFakeConfig("{"
                + "\"enforceProbes\":\"true\""
                + "}");
        setupExpectations(config);

        WildflyJARHealthCheckEnricher enricher = new WildflyJARHealthCheckEnricher(context);

        Probe livenessProbe = enricher.getLivenessProbe();
        assertHttpGet(livenessProbe, "HTTP","/health/live", 9990);

        Probe readinessProbe = enricher.getReadinessProbe();
        assertHttpGet(readinessProbe, "HTTP", "/health/ready", 9990);

        Probe startupProbe = enricher.getStartupProbe();
        assertHttpGet(startupProbe, "HTTP", "/health/started", 9990);
    }

    @Test
    @DisplayName("with false health enforced, should not add probes")
    void enforceFalseHealth_shouldNotAddProbes() {
        Map<String, Map<String, Object>> config = createFakeConfig("{"
                + "\"enforceProbes\":\"false\""
                + "}");
        setupExpectations(config);

        WildflyJARHealthCheckEnricher enricher = new WildflyJARHealthCheckEnricher(context);
        assertNoProbesAdded(enricher);
    }

    @Test
    void kubernetesHostName() {
        KubernetesListBuilder list = new KubernetesListBuilder().addToItems(new DeploymentBuilder()
            .withNewSpec()
            .withNewTemplate()
            .withNewSpec()
            .addNewContainer()
            .withName("app")
            .withImage("app:latest")
            .endContainer()
            .endSpec()
            .endTemplate()
            .endSpec()
            .build());
        WildflyJARHealthCheckEnricher enricher = new WildflyJARHealthCheckEnricher(context);
        enricher.create(PlatformMode.kubernetes, list);
        final List<ContainerBuilder> containerBuilders = new LinkedList<>();
        list.accept(new TypedVisitor<ContainerBuilder>() {
            @Override
            public void visit(ContainerBuilder containerBuilder) {
                containerBuilders.add(containerBuilder);
            }
        });

        assertThat(containerBuilders).singleElement()
            .extracting(ContainerFluent::buildEnv).asInstanceOf(InstanceOfAssertFactories.list(EnvVar.class))
            .singleElement()
            .hasFieldOrPropertyWithValue("name", "HOSTNAME")
            .extracting("valueFrom.fieldRef.fieldPath").isNotNull()
            .isEqualTo("metadata.name");
    }

    private void wildFlyJarDependencyWithVersion(String wildflyJarVersion) {
        context = context.toBuilder()
                .project(context.getProject().toBuilder()
                        .dependency(Dependency.builder()
                                .groupId("org.wildfly.plugins")
                                .artifactId("wildfly-jar-maven-plugin")
                                .version(wildflyJarVersion)
                                .build())
                        .build())
                .build();


    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> createFakeConfig(String config) {
        Map<String, Object> healthCheckJarMap = Serialization.unmarshal(config, Map.class);
        Map<String, Map<String, Object>> enricherConfigMap = new HashMap<>();
        enricherConfigMap.put("jkube-healthcheck-wildfly-jar", healthCheckJarMap);
        return enricherConfigMap;
    }

    private void assertHttpGet(Probe probe, String scheme, String path, int port) {
      assertThat(probe).isNotNull()
          .extracting(Probe::getHttpGet)
          .hasFieldOrPropertyWithValue("host", null)
          .hasFieldOrPropertyWithValue("scheme", scheme)
          .hasFieldOrPropertyWithValue("path", path)
          .hasFieldOrPropertyWithValue("port.intVal", port);
    }

    private void assertProbeAdded(Probe probe, String path, int port,
                                  int initialDelay, int failureThreshold, int successThreshold, int periodSeconds) {
      assertHttpGet(probe, "HTTPS", path, port);
      assertThat(probe).isNotNull()
          .returns(initialDelay, Probe::getInitialDelaySeconds)
          .returns(failureThreshold, Probe::getFailureThreshold)
          .returns(successThreshold, Probe::getSuccessThreshold)
          .returns(periodSeconds, Probe::getPeriodSeconds);
    }

    private void assertNoProbesAdded(WildflyJARHealthCheckEnricher enricher) {
      assertThat(enricher)
          .returns(null, WildflyJARHealthCheckEnricher::getLivenessProbe)
          .returns(null, WildflyJARHealthCheckEnricher::getReadinessProbe)
          .returns(null, WildflyJARHealthCheckEnricher::getStartupProbe);
    }
}
