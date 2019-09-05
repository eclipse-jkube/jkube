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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jayway.jsonpath.matchers.JsonPathMatchers;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.jshift.kit.build.service.docker.ImageConfiguration;
import io.jshift.kit.config.resource.PlatformMode;
import io.jshift.kit.config.resource.ResourceConfig;
import io.jshift.maven.enricher.api.MavenEnricherContext;
import io.jshift.maven.enricher.api.model.Configuration;
import io.jshift.kit.common.util.ResourceUtil;
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
 * @since 14/02/17
 */
public class ImageEnricherTest {

    @Mocked
    private MavenEnricherContext context;

    @Mocked
    ImageConfiguration imageConfiguration;

    private ImageEnricher imageEnricher;

    @Before
    public void prepareMock() {
        // Setup mock behaviour
        new Expectations() {{
            Configuration configuration =
                new Configuration.Builder()
                    .resource(new ResourceConfig.Builder()
                                  .env(Collections.singletonMap("MY_KEY", "MY_VALUE"))
                                  .build())
                .images(Collections.singletonList(imageConfiguration))
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
