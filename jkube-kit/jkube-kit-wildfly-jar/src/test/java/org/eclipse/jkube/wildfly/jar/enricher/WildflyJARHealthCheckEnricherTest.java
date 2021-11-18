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
package org.eclipse.jkube.wildfly.jar.enricher;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarSource;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.client.utils.Serialization;
import mockit.Expectations;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import mockit.Mocked;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.Plugin;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class WildflyJARHealthCheckEnricherTest {

    @Mocked
    protected JKubeEnricherContext context;

    private void setupExpectations(JavaProject project, Map<String, Object> bootableJarconfig, Map<String, Map<String, Object>> jkubeConfig) {
        Plugin plugin =
                Plugin.builder().artifactId(WildflyJARHealthCheckEnricher.BOOTABLE_JAR_ARTIFACT_ID).
                        groupId(WildflyJARHealthCheckEnricher.BOOTABLE_JAR_GROUP_ID).configuration(bootableJarconfig).build();
        List<Plugin> lst = new ArrayList<>();
        lst.add(plugin);
        ProcessorConfig c = new ProcessorConfig(null, null, jkubeConfig);
        new Expectations() {{
            project.getPlugins();
            result = lst;
            context.getProject();
            result = project;
            Configuration.ConfigurationBuilder configBuilder = Configuration.builder();
            configBuilder.processorConfig(c);
            context.getConfiguration();
            result = configBuilder.build();
        }};
    }

    private void setupExpectations(Map<String, Map<String, Object>> jkubeConfig) {
        ProcessorConfig c = new ProcessorConfig(null, null, jkubeConfig);
        new Expectations() {{
            Configuration.ConfigurationBuilder configBuilder = Configuration.builder();
            configBuilder.processorConfig(c);
            context.getConfiguration();
            result = configBuilder.build();
        }};
    }

    @Test
    public void testDefaultConfiguration(@Mocked final JavaProject project) {
        setupExpectations(project, Collections.emptyMap(), Collections.emptyMap());
        WildflyJARHealthCheckEnricher enricher = new WildflyJARHealthCheckEnricher(context);
        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testCloudConfiguration(@Mocked final JavaProject project) {
        Map<String, Object> config = new HashMap<>();
        config.put("cloud", null);
        setupExpectations(project, config, Collections.emptyMap());
        WildflyJARHealthCheckEnricher enricher = new WildflyJARHealthCheckEnricher(context);
        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertEquals(9990, probe.getHttpGet().getPort().getIntVal().intValue());
        assertEquals("/health/live", probe.getHttpGet().getPath());
        assertEquals("HTTP", probe.getHttpGet().getScheme());
        probe = enricher.getReadinessProbe();
        assertEquals(9990, probe.getHttpGet().getPort().getIntVal().intValue());
        assertEquals("/health/ready", probe.getHttpGet().getPath());
        assertEquals("HTTP", probe.getHttpGet().getScheme());
        assertNotNull(probe);
    }

    @Test
    public void testWithCustomConfigurationComingFromConf(@Mocked final JavaProject project) {
        Map<String, Object> jarConfig = new HashMap<>();
        jarConfig.put("cloud", null);
        final Map<String, Map<String, Object>> config = createFakeConfig("{\"readinessPath\":\"/foo/ready\","
                + "\"livenessPath\":\"/foo/live\","
                + "\"port\":\"1234\","
                + "\"scheme\":\"https\","
                +"\"livenessInitialDelay\":\"99\","
                +"\"readinessInitialDelay\":\"77\","
                +"\"failureThreshold\":\"17\","
                +"\"successThreshold\":\"27\""
                + "}");
        setupExpectations(project, jarConfig, config);

        final WildflyJARHealthCheckEnricher enricher = new WildflyJARHealthCheckEnricher(context);
        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertNull(probe.getHttpGet().getHost());
        assertEquals("HTTPS", probe.getHttpGet().getScheme());
        assertEquals(1234, probe.getHttpGet().getPort().getIntVal().intValue());
        assertEquals("/foo/live", probe.getHttpGet().getPath());
        assertEquals(99, probe.getInitialDelaySeconds().intValue());
        assertEquals(27, probe.getSuccessThreshold().intValue());
        assertEquals(17, probe.getFailureThreshold().intValue());
        probe = enricher.getReadinessProbe();
        assertNotNull(probe);
        assertEquals("HTTPS", probe.getHttpGet().getScheme());
        assertNull(probe.getHttpGet().getHost());
        assertEquals(1234, probe.getHttpGet().getPort().getIntVal().intValue());
        assertEquals("/foo/ready", probe.getHttpGet().getPath());
        assertEquals(77, probe.getInitialDelaySeconds().intValue());
        assertEquals(27, probe.getSuccessThreshold().intValue());
        assertEquals(17, probe.getFailureThreshold().intValue());
    }

    @Test
    public void testDisableHealth(@Mocked final JavaProject project) {
        Map<String, Object> jarConfig = new HashMap<>();
        jarConfig.put("cloud", null);
        final Map<String, Map<String, Object>> config = createFakeConfig("{"
                + "\"port\":\"-1\""
                + "}");
        setupExpectations(project, jarConfig, config);

        final WildflyJARHealthCheckEnricher enricher = new WildflyJARHealthCheckEnricher(context);
        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void testEnforceTrueHealth() {
        final Map<String, Map<String, Object>> config = createFakeConfig("{"
                + "\"enforceProbes\":\"true\""
                + "}");
        setupExpectations(config);

        final WildflyJARHealthCheckEnricher enricher = new WildflyJARHealthCheckEnricher(context);
        Probe probe = enricher.getLivenessProbe();
        assertNotNull(probe);
        assertEquals(9990, probe.getHttpGet().getPort().getIntVal().intValue());
        assertEquals("/health/live", probe.getHttpGet().getPath());
        assertEquals("HTTP", probe.getHttpGet().getScheme());
        probe = enricher.getReadinessProbe();
        assertEquals(9990, probe.getHttpGet().getPort().getIntVal().intValue());
        assertEquals("/health/ready", probe.getHttpGet().getPath());
        assertEquals("HTTP", probe.getHttpGet().getScheme());
        assertNotNull(probe);
    }

    @Test
    public void testEnforceFalseHealth() {
        final Map<String, Map<String, Object>> config = createFakeConfig("{"
                + "\"enforceProbes\":\"false\""
                + "}");
        setupExpectations(config);

        final WildflyJARHealthCheckEnricher enricher = new WildflyJARHealthCheckEnricher(context);
        Probe probe = enricher.getLivenessProbe();
        assertNull(probe);
        probe = enricher.getReadinessProbe();
        assertNull(probe);
    }

    @Test
    public void configureWildFlyJarHealthPort() {
        final WildflyJARHealthCheckEnricher enricher = new WildflyJARHealthCheckEnricher(context);
        final int port = enricher.getPort();
        Assert.assertEquals(9990, port);
        Assert.assertEquals("/health/live", enricher.getLivenessPath());
        Assert.assertEquals("/health/ready", enricher.getReadinessPath());
    }

    @Test
    public void kubernetesHostName() {
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
        final WildflyJARHealthCheckEnricher enricher = new WildflyJARHealthCheckEnricher(context);
        enricher.create(PlatformMode.kubernetes, list);
        final List<ContainerBuilder> containerBuilders = new LinkedList<>();
        list.accept(new TypedVisitor<ContainerBuilder>() {
            @Override
            public void visit(ContainerBuilder containerBuilder) {
                containerBuilders.add(containerBuilder);
            }
        });

        Assert.assertEquals(1, containerBuilders.size());
        ContainerBuilder cb = containerBuilders.get(0);
        List<EnvVar> env = cb.build().getEnv();
        Assert.assertEquals(1, env.size());
        EnvVar hostName = env.get(0);
        Assert.assertEquals("HOSTNAME", hostName.getName());
        Assert.assertNotNull(hostName.getValueFrom());
        EnvVarSource src = hostName.getValueFrom();
        Assert.assertEquals("metadata.name", src.getFieldRef().getFieldPath());
    }

    private Map<String, Map<String, Object>> createFakeConfig(String config) {
        try {
            Map<String, Object> healthCheckJarMap = Serialization.jsonMapper().readValue(config, Map.class);
            Map<String, Map<String, Object>> enricherConfigMap = new HashMap<>();
            enricherConfigMap.put("jkube-healthcheck-wildfly-jar", healthCheckJarMap);

            Map<String, Object> enricherMap = new HashMap<>();
            enricherMap.put("config", enricherConfigMap);

            Map<String, Object> pluginConfigurationMap = new HashMap<>();
            pluginConfigurationMap.put("enricher", enricherMap);

            return enricherConfigMap;
        } catch (JsonProcessingException jsonProcessingException) {
            jsonProcessingException.printStackTrace();
        }
        return null;
    }

}
