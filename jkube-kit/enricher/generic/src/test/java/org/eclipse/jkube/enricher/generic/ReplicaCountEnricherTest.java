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

import java.util.Arrays;
import java.util.List;

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigSpec;
import mockit.Expectations;
import mockit.Mocked;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.Before;
import org.junit.Test;

import static java.util.stream.Collectors.*;

import static org.eclipse.jkube.kit.config.resource.PlatformMode.*;
import static org.junit.Assert.*;

public class ReplicaCountEnricherTest {

    private static final int EXPECTED_REPLICAS = 2;

    @Mocked
    private JKubeEnricherContext context;

    private ReplicaCountEnricher enricher;

    // Kubernetes Deployment
    private KubernetesList kubernetesResources;

    // Openshift DeploymentConfig
    private KubernetesList openshiftResources;


    @Before
    public void setupExpectations() {
        // Setup Mock behaviour
        new Expectations() {{
            context.getProperty("jkube.replicas");
            result = String.valueOf(EXPECTED_REPLICAS);
        }};
    }

    @Before
    public void setUpEnricher() {
        enricher = new ReplicaCountEnricher(context);
    }

    @Before
    public void setUpKubernetesResources() {
        Deployment deployment = new Deployment();
        deployment.setSpec(new DeploymentSpec());
        deployment.getSpec().setReplicas(1);
        kubernetesResources = new KubernetesList("v1", Arrays.asList(deployment), null, null);
    }

    @Before
    public void setUpOpenshiftResources() {
        DeploymentConfig deploymentConfig = new DeploymentConfig();
        deploymentConfig.setSpec(new DeploymentConfigSpec());
        deploymentConfig.getSpec().setReplicas(1);
        openshiftResources = new KubernetesList("v1", Arrays.asList(deploymentConfig), null, null);
    }

    /**
     * Tests that replica count is overridden on Kubernetes if -Djkube.replicas is set
     */
    @Test
    public void testKubernetesReplicas() {
        KubernetesListBuilder listBuilder = new KubernetesListBuilder(kubernetesResources);
        enricher.enrich(kubernetes, listBuilder);
        List<Deployment> items = buildItems(listBuilder, Deployment.class);
        assertFalse(items.isEmpty());
        assertTrue(items.stream().allMatch(item -> item.getSpec().getReplicas() == EXPECTED_REPLICAS));
    }

    /**
     * Tests that replica count is overridden on Openshift if -Djkube.replicas is set
     */
    @Test
    public void testOpenshiftReplicas() {
        KubernetesListBuilder listBuilder = new KubernetesListBuilder(openshiftResources);
        enricher.enrich(openshift, listBuilder);
        List<DeploymentConfig> items = buildItems(listBuilder, DeploymentConfig.class);
        assertFalse(items.isEmpty());
        assertTrue(items.stream().allMatch(item -> item.getSpec().getReplicas() == EXPECTED_REPLICAS));
    }

    private <T> List<T> buildItems(KubernetesListBuilder listBuilder, Class<T> type) {
        return listBuilder.buildItems().stream().filter(type::isInstance).map(type::cast).collect(toList());
    }

}