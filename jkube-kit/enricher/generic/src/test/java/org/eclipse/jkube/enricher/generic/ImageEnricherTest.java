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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jayway.jsonpath.matchers.JsonPathMatchers;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import mockit.Expectations;
import mockit.Mocked;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author nicola
 */
public class ImageEnricherTest {

    @Mocked
    private JKubeEnricherContext context;

    @Mocked
    ImageConfiguration imageConfiguration;

    private ImageEnricher imageEnricher;

    @Before
    public void prepareMock() {
        // Setup mock behaviour
        new Expectations() {{
            Configuration configuration = Configuration.builder()
                .resource(ResourceConfig.builder()
                    .env(Collections.singletonMap("MY_KEY", "MY_VALUE"))
                    .build())
                .image(imageConfiguration)
                .build();
            context.getConfiguration(); result = configuration;

            imageConfiguration.getName(); result = "busybox";
            imageConfiguration.getAlias(); result = "busybox";
        }};

        imageEnricher = new ImageEnricher(context);
    }

    @Test
    public void checkEnrichDeployment() throws Exception {
        KubernetesListBuilder builder = new KubernetesListBuilder()
                .addNewDeploymentItem()
                .endDeploymentItem();

        imageEnricher.create(PlatformMode.kubernetes, builder);
        assertCorrectlyGeneratedResources(builder.build(), "Deployment");
    }

    @Test
    public void checkEnrichReplicaSet() throws Exception {
        KubernetesListBuilder builder = new KubernetesListBuilder()
                .addNewReplicaSetItem()
                .endReplicaSetItem();

        imageEnricher.create(PlatformMode.kubernetes, builder);
        assertCorrectlyGeneratedResources(builder.build(), "ReplicaSet");
    }

    @Test
    public void checkEnrichReplicationController() throws Exception {
        KubernetesListBuilder builder = new KubernetesListBuilder()
                .addNewReplicationControllerItem()
                .endReplicationControllerItem();

        imageEnricher.create(PlatformMode.kubernetes, builder);
        assertCorrectlyGeneratedResources(builder.build(), "ReplicationController");
    }

    @Test
    public void checkEnrichDaemonSet() throws Exception {
        KubernetesListBuilder builder = new KubernetesListBuilder()
                .addNewDaemonSetItem()
                .endDaemonSetItem();

        imageEnricher.create(PlatformMode.kubernetes, builder);
        assertCorrectlyGeneratedResources(builder.build(), "DaemonSet");
    }

    @Test
    public void checkEnrichStatefulSet() throws Exception {
        KubernetesListBuilder builder = new KubernetesListBuilder()
                .addNewStatefulSetItem()
                .endStatefulSetItem();

        imageEnricher.create(PlatformMode.kubernetes, builder);
        assertCorrectlyGeneratedResources(builder.build(), "StatefulSet");
    }

    @Test
    public void checkEnrichDeploymentConfig() throws Exception {
        KubernetesListBuilder builder = new KubernetesListBuilder()
                .addNewDeploymentConfigItem()
                .endDeploymentConfigItem();

        imageEnricher.create(PlatformMode.kubernetes, builder);
        assertCorrectlyGeneratedResources(builder.build(), "DeploymentConfig");
    }

    private void assertCorrectlyGeneratedResources(KubernetesList list, String kind) throws JsonProcessingException {
        assertEquals(list.getItems().size(),1);

        String json = ResourceUtil.toJson(list.getItems().get(0));
        assertThat(json, JsonPathMatchers.isJson());
        assertThat(json, JsonPathMatchers.hasJsonPath("$.kind", Matchers.equalTo(kind)));

        assertThat(json, JsonPathMatchers.hasJsonPath("$.spec.template.spec.containers[0].env[0].name", Matchers.equalTo("MY_KEY")));
        assertThat(json, JsonPathMatchers.hasJsonPath("$.spec.template.spec.containers[0].env[0].value", Matchers.equalTo("MY_VALUE")));
    }
}
