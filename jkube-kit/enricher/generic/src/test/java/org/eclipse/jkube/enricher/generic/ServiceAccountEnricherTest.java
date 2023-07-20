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
package org.eclipse.jkube.enricher.generic;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.ServiceAccountConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ServiceAccountEnricherTest {
    private JKubeEnricherContext context;

    @BeforeEach
    void setUp() {
        context = JKubeEnricherContext.builder()
          .project(JavaProject.builder()
            .properties(new Properties())
            .build())
          .resources(ResourceConfig.builder().build())
          .build();
    }

    @Test
    void create_withServiceAccountInResourceConfig_shouldCreateServiceAccount() {
        // Given
        givenServiceAccountConfiguredInResourceConfiguration();

        // When
        final KubernetesListBuilder builder = new KubernetesListBuilder();

        // Then
        enrichAndAssert(builder, "ribbon");
    }

    @Test
    void create_whenServiceAccountConfiguredInDeployment_thenServiceAccountCreated() {
        // Given
        Deployment deploymentFragment = createNewDeploymentFragmentWithServiceAccountConfigured("sa1").build();

        // When
        final KubernetesListBuilder builder = new KubernetesListBuilder().withItems(deploymentFragment);

        // Then
        enrichAndAssert(builder, "sa1");
    }

    @Test
    void create_whenServiceAccountNameConfiguredInDeployment_thenServiceAccountCreated() {
        // Given
        Deployment deploymentFragment = createNewDeploymentFragmentWithServiceAccountNameConfigured("sa1").build();

        // When
        final KubernetesListBuilder builder = new KubernetesListBuilder().withItems(deploymentFragment);

        // Then
        enrichAndAssert(builder, "sa1");
    }

    @Test
    void create_withAlreadyExistingServiceAccount_shouldNotCreateServiceAccount() {
        // Given
        final KubernetesListBuilder builder = new KubernetesListBuilder()
            .withItems(createNewDeploymentFragmentWithServiceAccountNameConfigured("ribbon").build(),
                new ServiceAccountBuilder().withNewMetadata().withName("ribbon").endMetadata().build());

        // When
        new ServiceAccountEnricher(context).create(PlatformMode.kubernetes, builder);

        // Then
        assertThat(builder.buildItems())
            .hasSize(2);
    }

    @Test
    void create_withSkipCreateEnabledAndPluginConfiguration_shouldNotCreateServiceAccount() {
        // Given
        context.getProperties().put("jkube.enricher.jkube-serviceaccount.skipCreate", "true");
        final KubernetesListBuilder builder = new KubernetesListBuilder();
        builder.addToItems(createNewDeploymentFragment().build());
        givenServiceAccountConfiguredInResourceConfiguration();

        // When
        new ServiceAccountEnricher(context).create(PlatformMode.kubernetes, builder);

        // Then
        assertThat(builder.buildItems())
            .singleElement(InstanceOfAssertFactories.type(Deployment.class))
            .hasFieldOrPropertyWithValue("spec.template.spec.serviceAccountName", "ribbon");
    }

    @Test
    void create_withSkipCreateEnabledAndFragment_shouldNotCreateServiceAccount() {
        // Given
        context.getProperties().put("jkube.enricher.jkube-serviceaccount.skipCreate", "true");
        final KubernetesListBuilder builder = new KubernetesListBuilder();
        builder.addToItems(createNewDeploymentFragmentWithServiceAccountConfigured("already-exist"));

        // When
        new ServiceAccountEnricher(context).create(PlatformMode.kubernetes, builder);

        // Then
        assertThat(builder.buildItems())
            .hasSize(1)
            .hasOnlyElementsOfType(Deployment.class);
    }

    @ParameterizedTest(name = "resources serviceAccount={0}, deployment, then generated deployment {2} is {3}")
    @MethodSource("resourceConfigTestData")
    void create_whenServiceAccountResourceConfigProvided_thenServiceAccountSetInGeneratedController(String resourceConfigServiceAccount, DeploymentBuilder deploymentFragment, String serviceAccountField, String serviceAccountName) {
        // Given
        context.getProperties().put("jkube.enricher.jkube-serviceaccount.skipCreate", "true");
        context = context.toBuilder()
            .resources(ResourceConfig.builder().serviceAccount(resourceConfigServiceAccount).build())
            .project(JavaProject.builder().groupId("org.example").artifactId("cheese").version("0.0.1").build())
            .build();
        final KubernetesListBuilder builder = new KubernetesListBuilder();
        builder.addToItems(deploymentFragment);

        // When
        new ServiceAccountEnricher(context).create(PlatformMode.kubernetes, builder);

        // Then
        assertControllerHasServiceAccountFieldSet(builder, serviceAccountField, serviceAccountName);
    }

    private static Stream<Arguments> resourceConfigTestData() {
        return Stream.of(
            arguments("", createNewDeploymentFragment(), "serviceAccountName", null),
            arguments("sa-from-config", createNewDeploymentFragment(), "serviceAccountName", "sa-from-config"),
            arguments("sa-from-config", createNewDeploymentFragmentWithServiceAccountConfigured("sa-from-fragment"), "serviceAccount", "sa-from-fragment"),
            arguments("sa-from-config", createNewDeploymentFragmentWithServiceAccountNameConfigured("sa-from-fragment"), "serviceAccountName","sa-from-fragment")
        );
    }

    private void enrichAndAssert(KubernetesListBuilder builder, String expectedServiceAccountName) {
        final ServiceAccountEnricher saEnricher = new ServiceAccountEnricher(context);
        saEnricher.create(PlatformMode.kubernetes, builder);

        final ServiceAccount serviceAccount = (ServiceAccount) builder.buildLastItem();
        assertThat(serviceAccount).isNotNull()
            .hasFieldOrPropertyWithValue("metadata.name", expectedServiceAccountName);
    }

    private void givenServiceAccountConfiguredInResourceConfiguration() {
        context = context.toBuilder()
          .resources(ResourceConfig.builder()
            .serviceAccount(ServiceAccountConfig.builder().name("ribbon").deploymentRef("cheese").build())
            .build())
          .build();
    }

    private static DeploymentBuilder createNewDeploymentFragment() {
        return new DeploymentBuilder()
            .withNewMetadata().withName("cheese").endMetadata()
            .withNewSpec()
            .withNewTemplate()
            .withNewSpec()
            .addNewContainer().withImage("cheese-image").endContainer()
            .endSpec()
            .endTemplate()
            .endSpec();
    }

    private static DeploymentBuilder createNewDeploymentFragmentWithServiceAccountConfigured(String serviceAccount) {
        return createNewDeploymentFragment().editSpec()
            .editTemplate()
            .editSpec()
            .withServiceAccount(serviceAccount)
            .endSpec()
            .endTemplate()
            .endSpec();
    }

    private static DeploymentBuilder createNewDeploymentFragmentWithServiceAccountNameConfigured(String serviceAccountName) {
        return createNewDeploymentFragment().editSpec()
            .editTemplate()
            .editSpec()
            .withServiceAccountName(serviceAccountName)
            .endSpec()
            .endTemplate()
            .endSpec();
    }

    private void assertControllerHasServiceAccountFieldSet(KubernetesListBuilder builder, String serviceAccountField, String serviceAccountName) {
        Optional<HasMetadata> hasMetadataOptional = builder.buildItems()
            .stream()
            .filter(h -> h.getKind().equalsIgnoreCase("Deployment"))
            .findAny();
        assertThat(hasMetadataOptional)
            .isPresent()
            .get(InstanceOfAssertFactories.type(Deployment.class))
            .extracting(Deployment::getSpec)
            .extracting(DeploymentSpec::getTemplate)
            .extracting(PodTemplateSpec::getSpec)
            .hasFieldOrPropertyWithValue(serviceAccountField, serviceAccountName);
    }
}
