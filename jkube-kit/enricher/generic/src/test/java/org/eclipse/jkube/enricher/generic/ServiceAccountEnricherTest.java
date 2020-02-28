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
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.ServiceAccountConfig;
import org.eclipse.jkube.maven.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.maven.enricher.api.model.Configuration;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class ServiceAccountEnricherTest {
    @Mocked
    private JKubeEnricherContext context;

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Test
    public void testServiceAccountCreationFromConfig() {
        new Expectations() {{
            context.getConfiguration();
            result = new Configuration.Builder()
                    .resource(new ResourceConfig.Builder()
                            .withServiceAccounts(Collections.singletonList(new ServiceAccountConfig("ribbon"))).build())
                    .build();
        }};
        final KubernetesListBuilder builder = new KubernetesListBuilder();

        enrichAndAssert(builder);
    }

    @Test
    public void testServiceAccountCreationFromFragment() {
        final KubernetesListBuilder builder = new KubernetesListBuilder()
                .withItems(new DeploymentBuilder().withNewMetadata().withName("cheese").endMetadata()
                        .withNewSpec().withNewTemplate().withNewSpec()
                        .addNewContainer().withImage("cheese-image").endContainer()
                        .withServiceAccount("ribbon")
                        .endSpec().endTemplate().endSpec().build());

        enrichAndAssert(builder);
    }

    private void enrichAndAssert(KubernetesListBuilder builder) {
        final ServiceAccountEnricher saEnricher = new ServiceAccountEnricher(context);
        saEnricher.create(PlatformMode.kubernetes, builder);

        final ServiceAccount serviceAccount = (ServiceAccount) builder.buildLastItem();
        assertThat(serviceAccount).isNotNull();
        assertThat(serviceAccount.getMetadata().getName()).isEqualTo("ribbon");
    }
}
