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

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import org.eclipse.jkube.kit.config.resource.GroupArtifactVersion;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test label generation.
 *
 * @author nicola
 */
public class MavenProjectEnricherTest {

    @Mocked
    private JKubeEnricherContext context;

    @Before
    public void setupExpectations() {
        new Expectations() {{
            context.getGav();
            result = new GroupArtifactVersion("groupId", "artifactId", "version");
        }};
    }

    @Test
    public void testGeneratedResources() {
        ProjectLabelEnricher projectEnricher = new ProjectLabelEnricher(context);

        KubernetesListBuilder builder = createListWithDeploymentConfig();
        projectEnricher.enrich(PlatformMode.kubernetes, builder);
        KubernetesList list = builder.build();

        Map<String, String> labels = list.getItems().get(0).getMetadata().getLabels();

        assertNotNull(labels);
        assertEquals("groupId", labels.get("group"));
        assertEquals("artifactId", labels.get("app"));
        assertEquals("version", labels.get("version"));
        assertNull(labels.get("project"));

        builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().build());
        projectEnricher.create(PlatformMode.kubernetes, builder);

        Deployment deployment = (Deployment)builder.buildFirstItem();
        Map<String, String> selectors = deployment.getSpec().getSelector().getMatchLabels();
        assertEquals("groupId", selectors.get("group"));
        assertEquals("artifactId", selectors.get("app"));
        assertNull(selectors.get("version"));
        assertNull(selectors.get("project"));
    }
    

    @Test
    public void testCustomAppName() {
    	
    	// Setup
    	final String CUSTOM_APP_NAME =  "my-custom-app-name";
    	final Properties properties = new Properties();
        properties.setProperty("jkube.enricher.jkube-project-label.customAppName", CUSTOM_APP_NAME);
        // @formatter:off
        new Expectations() {{
            context.getProperties(); result = properties;
        }};
        // @formatter:on
    	
    	ProjectLabelEnricher projectEnricher = new ProjectLabelEnricher(context);

        KubernetesListBuilder builder = createListWithDeploymentConfig();
        projectEnricher.enrich(PlatformMode.kubernetes, builder);
        KubernetesList list = builder.build();
        
        Map<String, String> labels = list.getItems().get(0).getMetadata().getLabels();

        assertNotNull(labels);
        assertEquals("groupId", labels.get("group"));
        assertEquals(CUSTOM_APP_NAME, labels.get("app"));
        assertEquals("version", labels.get("version"));
        assertNull(labels.get("project"));

        builder = new KubernetesListBuilder().withItems(new DeploymentBuilder().build());
        projectEnricher.create(PlatformMode.kubernetes, builder);

        Deployment deployment = (Deployment)builder.buildFirstItem();
        Map<String, String> selectors = deployment.getSpec().getSelector().getMatchLabels();
        assertEquals("groupId", selectors.get("group"));
        assertEquals(CUSTOM_APP_NAME, selectors.get("app"));
        assertNull(selectors.get("version"));
        assertNull(selectors.get("project"));
        
    }

    @Test
    public void testOldStyleGeneratedResources() {

        final Properties properties = new Properties();
        properties.setProperty("jkube.enricher.jkube-project-label.useProjectLabel", "true");
        // @formatter:off
        new Expectations() {{
            context.getProperties(); result = properties;
        }};
        // @formatter:on

        ProjectLabelEnricher projectEnricher = new ProjectLabelEnricher(context);

        KubernetesListBuilder builder = createListWithDeploymentConfig();
        projectEnricher.enrich(PlatformMode.kubernetes, builder);
        KubernetesList list = builder.build();

        Map<String, String> labels = list.getItems().get(0).getMetadata().getLabels();

        assertNotNull(labels);
        assertEquals("groupId", labels.get("group"));
        assertEquals("artifactId", labels.get("project"));
        assertEquals("version", labels.get("version"));
        assertNull(labels.get("app"));

        builder = new KubernetesListBuilder().withItems(new DeploymentConfigBuilder().build());
        projectEnricher.create(PlatformMode.kubernetes, builder);

        DeploymentConfig deploymentConfig = (DeploymentConfig)builder.buildFirstItem();
        Map<String, String> selectors = deploymentConfig.getSpec().getSelector();
        assertEquals("groupId", selectors.get("group"));
        assertEquals("artifactId", selectors.get("project"));
        assertNull(selectors.get("version"));
        assertNull(selectors.get("app"));
    }

    private KubernetesListBuilder createListWithDeploymentConfig() {
        return new KubernetesListBuilder().addToItems(new DeploymentConfigBuilder()
            .withNewMetadata().endMetadata()
            .withNewSpec().endSpec()
            .build());
    }

}
