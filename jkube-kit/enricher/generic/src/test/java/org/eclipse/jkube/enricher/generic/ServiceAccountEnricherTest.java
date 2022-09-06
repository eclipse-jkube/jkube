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

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.ServiceAccountConfig;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServiceAccountEnricherTest {
    private JKubeEnricherContext context;
    @Before
    public void setUp() {
        context = mock(JKubeEnricherContext.class,RETURNS_DEEP_STUBS);
    }
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Test
    public void create_withServiceAccountInResourceConfig_shouldCreateServiceAccount() {
        // Given
        givenServiceAccountConfiguredInResourceConfiguration();

        // When
        final KubernetesListBuilder builder = new KubernetesListBuilder();

        // Then
        enrichAndAssert(builder, "ribbon");
    }

    @Test
    public void create_whenServiceAccountConfiguredInDeployment_thenServiceAccountCreated() {
        // Given
        Deployment deploymentFragment = createNewDeploymentFragmentWithServiceAccountConfigured("sa1").build();

        // When
        final KubernetesListBuilder builder = new KubernetesListBuilder().withItems(deploymentFragment);

        // Then
        enrichAndAssert(builder, "sa1");
    }

    @Test
    public void create_whenServiceAccountNameConfiguredInDeployment_thenServiceAccountCreated() {
        // Given
        Deployment deploymentFragment = createNewDeploymentFragmentWithServiceAccountNameConfigured("sa1").build();

        // When
        final KubernetesListBuilder builder = new KubernetesListBuilder().withItems(deploymentFragment);

        // Then
        enrichAndAssert(builder, "sa1");
    }

    @Test
    public void create_withAlreadyExistingServiceAccount_shouldNotCreateServiceAccount() {
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
    public void create_withSkipCreateEnabledAndPluginConfiguration_shouldNotCreateServiceAccount() {
        // Given
        Properties properties = new Properties();
        properties.put("jkube.enricher.jkube-serviceaccount.skipCreate", "true");
        final KubernetesListBuilder builder = new KubernetesListBuilder();
        builder.addToItems(createNewDeploymentFragment().build());
        givenServiceAccountConfiguredInResourceConfiguration();
        givenProperties(properties);

        // When
        new ServiceAccountEnricher(context).create(PlatformMode.kubernetes, builder);

        // Then
        assertThat(builder.buildItems())
            .hasSize(1)
            .first(InstanceOfAssertFactories.type(Deployment.class))
            .hasFieldOrPropertyWithValue("spec.template.spec.serviceAccountName", "ribbon");
    }

    @Test
    public void create_withSkipCreateEnabledAndFragment_shouldNotCreateServiceAccount() {
        // Given
        Properties properties = new Properties();
        properties.put("jkube.enricher.jkube-serviceaccount.skipCreate", "true");
        final KubernetesListBuilder builder = new KubernetesListBuilder();
        builder.addToItems(createNewDeploymentFragmentWithServiceAccountConfigured("already-exist"));
        givenProperties(properties);

        // When
        new ServiceAccountEnricher(context).create(PlatformMode.kubernetes, builder);

        // Then
        assertThat(builder.buildItems())
            .hasSize(1)
            .hasOnlyElementsOfType(Deployment.class);
    }

    private void enrichAndAssert(KubernetesListBuilder builder, String expectedServiceAccountName) {
        final ServiceAccountEnricher saEnricher = new ServiceAccountEnricher(context);
        saEnricher.create(PlatformMode.kubernetes, builder);

        final ServiceAccount serviceAccount = (ServiceAccount) builder.buildLastItem();
        assertThat(serviceAccount).isNotNull();
        assertThat(serviceAccount.getMetadata().getName()).isEqualTo(expectedServiceAccountName);
    }

    private void givenServiceAccountConfiguredInResourceConfiguration() {
        when(context.getConfiguration()).thenReturn(Configuration.builder()
                .resource(ResourceConfig.builder()
                        .serviceAccount(ServiceAccountConfig.builder().name("ribbon").deploymentRef("cheese").build()).build())
                .build());
    }

    private void givenProperties(Properties properties) {
        when(context.getProperties()).thenReturn(properties);
    }

    private DeploymentBuilder createNewDeploymentFragment() {
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

    private DeploymentBuilder createNewDeploymentFragmentWithServiceAccountConfigured(String serviceAccount) {
        return createNewDeploymentFragment().editSpec()
            .editTemplate()
            .editSpec()
            .withServiceAccount(serviceAccount)
            .endSpec()
            .endTemplate()
            .endSpec();
    }

    private DeploymentBuilder createNewDeploymentFragmentWithServiceAccountNameConfigured(String serviceAccountName) {
        return createNewDeploymentFragment().editSpec()
            .editTemplate()
            .editSpec()
            .withServiceAccountName(serviceAccountName)
            .endSpec()
            .endTemplate()
            .endSpec();
    }
}
