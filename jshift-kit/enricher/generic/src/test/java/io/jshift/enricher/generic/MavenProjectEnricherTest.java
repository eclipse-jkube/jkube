/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jshift.enricher.generic;

import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.jshift.kit.config.resource.GroupArtifactVersion;
import io.jshift.kit.config.resource.PlatformMode;
import io.jshift.maven.enricher.api.MavenEnricherContext;
import io.jshift.maven.enricher.api.model.Configuration;
import mockit.Expectations;
import mockit.Mocked;
import org.apache.maven.project.MavenProject;
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
    private MavenEnricherContext context;

    @Mocked
    private MavenProject mavenProject;

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
    public void testOldStyleGeneratedResources() {

        final Properties properties = new Properties();
        properties.setProperty("jshift.enricher.jshift-project-label.useProjectLabel", "true");
        new Expectations() {{
            context.getConfiguration();
            result = new Configuration.Builder().properties(properties).build();
        }};

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
        return new KubernetesListBuilder()
                .addNewDeploymentConfigItem()
                .withNewMetadata().endMetadata()
                .withNewSpec().endSpec()
                .endDeploymentConfigItem();
    }

}
