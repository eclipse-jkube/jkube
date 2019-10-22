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
package io.jkube.enricher.generic;

import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.jkube.kit.config.resource.PlatformMode;
import io.jkube.kit.config.resource.ResourceConfig;
import io.jkube.kit.config.resource.ServiceAccountConfig;
import io.jkube.maven.enricher.api.MavenEnricherContext;
import io.jkube.maven.enricher.api.model.Configuration;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class ServiceAccountEnricherTest {
    @Mocked
    private MavenEnricherContext context;

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
