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
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.apps.DaemonSetBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

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
        givenResourceConfigWithEnvVar("MY_KEY", "MY_VALUE");
        // @formatter:off
        new Expectations() {{
            imageConfiguration.getName(); result = "busybox";
            imageConfiguration.getAlias(); result = "busybox";
        }};
        // @formatter:on

        imageEnricher = new ImageEnricher(context);
    }

    @Test
    public void checkEnrichDeployment() throws Exception {
        KubernetesListBuilder builder = new KubernetesListBuilder().addToItems(new DeploymentBuilder().build());

        imageEnricher.create(PlatformMode.kubernetes, builder);
        assertCorrectlyGeneratedResources(builder.build(), "Deployment", "MY_KEY", "MY_VALUE");
    }

    @Test
    public void checkEnrichReplicaSet() throws Exception {
        KubernetesListBuilder builder = new KubernetesListBuilder().addToItems(new ReplicaSetBuilder().build());

        imageEnricher.create(PlatformMode.kubernetes, builder);
        assertCorrectlyGeneratedResources(builder.build(), "ReplicaSet", "MY_KEY", "MY_VALUE");
    }

    @Test
    public void checkEnrichReplicationController() throws Exception {
        KubernetesListBuilder builder = new KubernetesListBuilder()
                .addNewReplicationControllerItem()
                .endReplicationControllerItem();

        imageEnricher.create(PlatformMode.kubernetes, builder);
        assertCorrectlyGeneratedResources(builder.build(), "ReplicationController", "MY_KEY", "MY_VALUE");
    }

    @Test
    public void checkEnrichDaemonSet() throws Exception {
        KubernetesListBuilder builder = new KubernetesListBuilder().addToItems(new DaemonSetBuilder().build());

        imageEnricher.create(PlatformMode.kubernetes, builder);
        assertCorrectlyGeneratedResources(builder.build(), "DaemonSet", "MY_KEY", "MY_VALUE");
    }

    @Test
    public void checkEnrichStatefulSet() throws Exception {
        KubernetesListBuilder builder = new KubernetesListBuilder().addToItems(new StatefulSetBuilder().build());

        imageEnricher.create(PlatformMode.kubernetes, builder);
        assertCorrectlyGeneratedResources(builder.build(), "StatefulSet", "MY_KEY", "MY_VALUE");
    }

    @Test
    public void checkEnrichDeploymentConfig() throws Exception {
        KubernetesListBuilder builder = new KubernetesListBuilder().addToItems(new DeploymentConfigBuilder().build());

        imageEnricher.create(PlatformMode.kubernetes, builder);
        assertCorrectlyGeneratedResources(builder.build(), "DeploymentConfig", "MY_KEY", "MY_VALUE");
    }

    @Test
    public void create_whenEnvironmentVariableAbsent_thenAddsEnvironmentVariable() throws JsonProcessingException {
        // Given
        KubernetesListBuilder builder = new KubernetesListBuilder().addToItems(new DeploymentBuilder().build());

        // When
        imageEnricher.create(PlatformMode.kubernetes, builder);

        // Then
        assertCorrectlyGeneratedResources(builder.build(), "Deployment", "MY_KEY", "MY_VALUE");
    }

    @Test
    public void create_whenEnvironmentVariablePresentWithDifferentValue_thenOldValueIsPreserved() throws JsonProcessingException {
        // Given
        givenResourceConfigWithEnvVar("key", "valueNew");
        KubernetesListBuilder builder = new KubernetesListBuilder().addToItems(new DeploymentBuilder()
            .withNewSpec()
            .withNewTemplate()
            .withNewSpec()
            .addNewContainer()
            .addNewEnv().withName("key").withValue("valueOld").endEnv()
            .endContainer()
            .endSpec()
            .endTemplate()
            .endSpec()
          .build());

        // When
        imageEnricher.create(PlatformMode.kubernetes, builder);

        // Then
        assertCorrectlyGeneratedResources(builder.build(), "Deployment", "key", "valueOld");
    }

    private void assertCorrectlyGeneratedResources(KubernetesList list, String kind, String expectedKey, String expectedValue) throws JsonProcessingException {
        assertThat(list.getItems())
                .hasSize(1)
                .first()
                .hasFieldOrPropertyWithValue("kind", kind);

        assertThat(list.getItems())
                .extracting("spec.template.spec.containers")
                .first()
                .asList()
                .extracting("env")
                .first()
                .asList()
                .first()
                .hasFieldOrPropertyWithValue("name", expectedKey)
                .hasFieldOrPropertyWithValue("value", expectedValue);
    }

    private void givenResourceConfigWithEnvVar(String name, String value) {
        // @formatter:off
        new Expectations() {{
            Configuration configuration = Configuration.builder()
              .resource(ResourceConfig.builder()
                .env(Collections.singletonMap(name, value))
                .build())
              .image(imageConfiguration)
              .build();
            context.getConfiguration(); result = configuration;
        }};
        // @formatter:on
    }
}
