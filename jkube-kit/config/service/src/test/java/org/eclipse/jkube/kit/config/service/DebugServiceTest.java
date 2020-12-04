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
package org.eclipse.jkube.kit.config.service;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.PodListBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;
import org.eclipse.jkube.kit.common.DebugConstants;
import org.eclipse.jkube.kit.common.KitLogger;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DebugServiceTest {
    @Mocked
    private KubernetesClient kubernetesClient;

    @Mocked
    private ApplyService applyService;

    @Mocked
    private KitLogger logger;

    @Test
    public void testApplyEntities() {
        // Given
        PortForwardService portForwardService = new PortForwardService(kubernetesClient, logger);
        Deployment deployment = getDeployment(getTestLabels());
        ApplyService applyService = new ApplyService(kubernetesClient, logger);
        DebugService debugService = new DebugService(logger, portForwardService, applyService);
        List<EnvVar> debugEnvVars = getDebugEnvVars();
        PodList podList = new PodListBuilder().withItems(getPod(debugEnvVars)).build();
        new Expectations() {{
            applyService.isAlreadyApplied((HasMetadata)any);
            result = true;

            kubernetesClient.pods().inNamespace(anyString).withLabels((Map<String, String>) any).list();
            result = podList;
        }};
        Set<HasMetadata> entities = new HashSet<>();
        entities.add(deployment);

        // When
        PortForwardService.PortForwardThread portForwardTask = debugService.debug(kubernetesClient, "test-ns", "test-file", entities, "5005", false, logger);

        // Then
        assertNotNull(portForwardTask);
        assertEquals("test-app", portForwardTask.getPodName());
        assertEquals("test-ns", portForwardTask.getNamespace());
        new Verifications() {{
            kubernetesClient.pods().inNamespace(anyString).withLabels((Map<String, String>) any).list();
            times = 1;
        }};
    }

    private List<EnvVar> getDebugEnvVars() {
        List<EnvVar> debugEnvVars = new ArrayList<>();
        debugEnvVars.add(new EnvVarBuilder().withName(DebugConstants.ENV_VAR_JAVA_DEBUG).withValue("true").build());
        debugEnvVars.add(new EnvVarBuilder().withName(DebugConstants.ENV_VAR_JAVA_DEBUG_SUSPEND).withValue("false").build());
        debugEnvVars.add(new EnvVarBuilder().withName(DebugConstants.ENV_VAR_JAVA_DEBUG_SESSION).withValue("false").build());
        return debugEnvVars;
    }

    @Test
    public void testGetDebugEnvVarsMap() {
        // Given
        DebugService debugService = createDebugService();

        // When
        Map<String, String> result = debugService.getDebugEnvVarsMap(false);

        // Then
        assertNotNull(result);
        assertFalse(Boolean.parseBoolean(result.get(DebugConstants.ENV_VAR_JAVA_DEBUG_SUSPEND)));
        assertTrue(Boolean.parseBoolean(result.get(DebugConstants.ENV_VAR_JAVA_DEBUG)));
    }

    @Test
    public void testGetLabelSelectorsFromDeployment() {
        // Given
        DebugService debugService = createDebugService();
        HasMetadata deploy = getDeployment(getTestLabels());

        // When
        LabelSelector result = debugService.getLabelSelectorsFromHasMetadata(deploy, kubernetesClient, "test-ns", "test-app", false);

        // Then
        assertNotNull(result);
        assertEquals(getTestLabelSelector(), result);
    }

    @Test
    public void testGetLabelSelectorsFromReplicaSet() {
        // Given
        DebugService debugService = createDebugService();
        HasMetadata rs = getReplicaSet(getTestLabels());

        // When
        LabelSelector result = debugService.getLabelSelectorsFromHasMetadata(rs, kubernetesClient, "test-ns", "test-app", false);

        // Then
        assertNotNull(result);
        assertEquals(getTestLabelSelector(), result);
    }

    @Test
    public void testGetLabelSelectorsFromReplicationController() {
        // Given
        DebugService debugService = createDebugService();
        HasMetadata rc = getReplicationController(getTestLabels());

        // When
        LabelSelector result = debugService.getLabelSelectorsFromHasMetadata(rc, kubernetesClient, "test-ns", "test-app", false);

        // Then
        assertNotNull(result);
        assertNotNull(result.getMatchLabels());
        assertEquals(3, result.getMatchLabels().size());
        assertEquals("jkube", result.getMatchLabels().get("provider"));
        assertEquals("test-app", result.getMatchLabels().get("app"));
        assertEquals("test", result.getMatchLabels().get("group"));
    }

    @Test
    public void testGetLabelSelectorsFromDeploymentConfig() {
        // Given
        DebugService debugService = createDebugService();
        HasMetadata dc = getDeploymentConfig(getTestLabels());

        // When
        LabelSelector result = debugService.getLabelSelectorsFromHasMetadata(dc, kubernetesClient, "test-ns", "test-app", false);

        // Then
        assertNotNull(result);
        assertNotNull(result.getMatchLabels());
        assertEquals(3, result.getMatchLabels().size());
        assertEquals("jkube", result.getMatchLabels().get("provider"));
        assertEquals("test-app", result.getMatchLabels().get("app"));
        assertEquals("test", result.getMatchLabels().get("group"));
    }

    @Test
    public void testPodHasEnvVars() {
        // Given
        List<EnvVar> envVars = new ArrayList<>();
        envVars.add(new EnvVarBuilder().withName("e1").withValue("v1").build());
        envVars.add(new EnvVarBuilder().withName("e2").withValue("v2").build());
        Pod pod = getPod(envVars);

        // When
        boolean result1 = DebugService.podHasEnvVars(pod, Collections.singletonMap("e1", "v1"));
        boolean result2 = DebugService.podHasEnvVars(pod, Collections.singletonMap("D1", "d2"));

        // Then
        assertTrue(result1);
        assertFalse(result2);
    }

    @Test
    public void testPortForward() {
        // Given
        DebugService debugService = createDebugService();

        // When
        PortForwardService.PortForwardThread portForwardTask = debugService.portForward("test-pod", "test-ns", "5005");

        // Then
        assertNotNull(portForwardTask);
        assertEquals("test-pod", portForwardTask.getPodName());
        assertEquals("test-ns", portForwardTask.getNamespace());
    }

    @Test
    public void testPortForwardTermination() throws InterruptedException {
        // Given
        DebugService debugService = createDebugService();
        CountDownLatch isAliveLatch = new CountDownLatch(1);

        // When
        PortForwardService.PortForwardThread portForwardTask = debugService.portForward("test-pod", "test-ns", "5005");
        portForwardTask.start();
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                if (!portForwardTask.isAlive()) {
                    isAliveLatch.countDown();
                }
            }
        }).start();
        portForwardTask.interrupt();

        // Then
        assertNotNull(portForwardTask);
        assertEquals("test-pod", portForwardTask.getPodName());
        assertEquals("test-ns", portForwardTask.getNamespace());
        assertTrue(isAliveLatch.await(500, TimeUnit.MILLISECONDS));
        assertEquals(Thread.State.TERMINATED, portForwardTask.getState());
    }

    @Test
    public void testPortForwardPodWatcherOnEventReceived() {
        // Given
        Pod foundPod = null;
        Map<String, String> debugEnvVars = new HashMap<>();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        DebugService.PortForwardPodWatcher portForwardPodWatcher = new DebugService.PortForwardPodWatcher(logger, debugEnvVars, foundPod, countDownLatch);
        Pod pod = getPod(getDebugEnvVars());

        // When
        portForwardPodWatcher.eventReceived(Watcher.Action.MODIFIED, pod);

        // Then
        assertNotNull(portForwardPodWatcher.getFoundPod());
        assertEquals(0, portForwardPodWatcher.getTerminateLatch().getCount());
    }

    private DebugService createDebugService() {
        PortForwardService portForwardService = new PortForwardService(kubernetesClient, logger);
        return new DebugService(logger, portForwardService, applyService);
    }

    private HasMetadata getReplicaSet(Map<String, String> testLabels) {
        return new ReplicaSetBuilder()
                .withNewMetadata()
                .withName("test-app")
                .addToLabels(testLabels)
                .endMetadata()
                .withNewSpec()
                .withReplicas(1)
                .withSelector(getTestLabelSelector())
                .withTemplate(getPodTemplateSpec(testLabels))
                .endSpec()
                .build();
    }

    private HasMetadata getReplicationController(Map<String, String> testLabels) {
        return new ReplicationControllerBuilder()
                .withNewMetadata().withName("test-app").addToLabels(testLabels).endMetadata()
                .withNewSpec()
                .withReplicas(1)
                .withSelector(getMatchLabels())
                .withTemplate(getPodTemplateSpec(testLabels))
                .endSpec()
                .build();
    }

    private Deployment getDeployment(Map<String, String> labels) {
        return new DeploymentBuilder()
                .withNewMetadata()
                .withName("test-app")
                .addToLabels(labels)
                .endMetadata()
                .withNewSpec()
                .withReplicas(1)
                .withRevisionHistoryLimit(2)
                .withSelector(getTestLabelSelector())
                .withTemplate(getPodTemplateSpec(labels))
                .endSpec()
                .build();
    }

    private HasMetadata getDeploymentConfig(Map<String, String> testLabels) {
        return new DeploymentConfigBuilder()
                .withNewMetadata().withName("test-app").withLabels(testLabels).endMetadata()
                .withNewSpec()
                .withReplicas(1)
                .withSelector(getMatchLabels())
                .withTemplate(getPodTemplateSpec(testLabels))
                .endSpec()
                .build();
    }

    private PodTemplateSpec getPodTemplateSpec(Map<String, String> labels) {
        return new PodTemplateSpecBuilder()
                .withNewMetadata()
                .addToLabels(labels)
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withImage("foo/test-app:0.0.1")
                .withName("test-app")
                .addNewPort().withContainerPort(8080).withName("http").withProtocol("TCP").endPort()
                .endContainer()
                .endSpec()
                .build();
    }

    private LabelSelector getTestLabelSelector() {
        LabelSelectorBuilder labelSelectorBuilder =  new LabelSelectorBuilder();
        getMatchLabels().forEach(labelSelectorBuilder::addToMatchLabels);

        return labelSelectorBuilder.build();
    }

    private Map<String, String> getMatchLabels() {
        Map<String, String> matchLabels = new HashMap<>();
        matchLabels.put("app", "test-app");
        matchLabels.put("provider", "jkube");
        matchLabels.put("group", "test");
        return matchLabels;
    }

    private Map<String, String> getTestLabels() {
        Map<String, String> labels = new HashMap<>();
        labels.put("app", "test-app");
        labels.put("provider", "jkube");
        labels.put("version", "0.0.1");
        labels.put("group", "test");
        return labels;
    }

    private Pod getPod(List<EnvVar> envVarList) {
        return new PodBuilder()
                .withNewMetadata()
                .withName("test-app")
                .withLabels(getTestLabels())
                .endMetadata()
                .withNewSpec()
                .addNewContainer()
                .withImage("foo/bar:0.1.0")
                .withName("test")
                .withEnv(envVarList)
                .endContainer()
                .endSpec()
                .withNewStatus()
                .withPhase("Running")
                .endStatus()
                .build();
    }
}
