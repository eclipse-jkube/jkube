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

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.jkube.kit.common.util.Base64Util;
import io.jkube.kit.config.resource.PlatformMode;
import io.jkube.kit.config.resource.ResourceConfig;
import io.jkube.kit.config.resource.SecretConfig;
import io.jkube.maven.enricher.api.BaseEnricher;
import io.jkube.maven.enricher.api.MavenEnricherContext;
import io.jkube.maven.enricher.api.util.SecretConstants;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SecretEnricher extends BaseEnricher {

    public SecretEnricher(MavenEnricherContext buildContext, String name) {
        super(buildContext, name);
    }

    protected String encode(String raw) {
        return Base64Util.encodeToString(raw);
    }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
        // update builder
        // use a selector to choose all secret builder in kubernetes list builders.
        // try to find the target annotations
        // use the annotation value to generate data
        // add generated data to this builder
        builder.accept(new TypedVisitor<SecretBuilder>() {
            @Override
            public void visit(SecretBuilder secretBuilder) {
                Map<String, String> annotation = secretBuilder.buildMetadata().getAnnotations();
                if (!annotation.containsKey(getAnnotationKey())) {
                    return;
                }
                String dockerId = annotation.get(getAnnotationKey());
                Map<String, String> data = generateData(dockerId);
                if (data == null) {
                    return;
                }
                // remove the annotation key
                annotation.remove(getAnnotationKey());
                secretBuilder.addToData(data);
            }
        });

        addSecretsFromXmlConfiguration(builder);
    }

    private void addSecretsFromXmlConfiguration(KubernetesListBuilder builder) {
        log.verbose("Adding secrets resources from plugin configuration");
        List<SecretConfig> secrets = getSecretsFromXmlConfig();
        Map<String, Integer> secretToIndexMap = new HashMap<>();
        if (secrets == null || secrets.isEmpty()) {
            return;
        }

        for(Integer index = 0; index < builder.buildItems().size(); index++) {
            if(builder.buildItems().get(index) instanceof Secret) {
                secretToIndexMap.put(builder.buildItems().get(index).getMetadata().getName(), index);
            }
        }

        for (int i = 0; i < secrets.size(); i++) {
            SecretConfig secretConfig = secrets.get(i);
            if (StringUtils.isBlank(secretConfig.getName())) {
                log.warn("Secret name is empty. You should provide a proper name for the secret");
                continue;
            }

            Map<String, String> data = new HashMap<>();
            String type = "";
            ObjectMeta metadata = new ObjectMetaBuilder()
                    .withNamespace(secretConfig.getNamespace())
                    .withName(secretConfig.getName())
                    .build();

            // docker-registry
            if (secretConfig.getDockerServerId() != null) {
                MavenEnricherContext mavenContext = ((MavenEnricherContext)getContext());
                String dockerSecret = (mavenContext).getDockerJsonConfigString(mavenContext.getSettings(), secretConfig.getDockerServerId());
                if (StringUtils.isBlank(dockerSecret)) {
                    log.warn("Docker secret with id "
                            + secretConfig.getDockerServerId()
                            + " cannot be found in maven settings");
                    continue;
                }
                data.put(SecretConstants.DOCKER_DATA_KEY, Base64Util.encodeToString(dockerSecret));
                type = SecretConstants.DOCKER_CONFIG_TYPE;
            }
            // TODO: generic secret (not supported for now)

            if (StringUtils.isBlank(type) || data.isEmpty()) {
                log.warn("No data can be found for docker secret with id " + secretConfig.getDockerServerId());
                continue;
            }

            Secret secret = new SecretBuilder().withData(data).withMetadata(metadata).withType(type).build();
            if(!secretToIndexMap.containsKey(secretConfig.getName())) {
                builder.addToSecretItems(i, secret);
            }
        }
    }

    private List<SecretConfig> getSecretsFromXmlConfig() {
        ResourceConfig resourceConfig = getConfiguration().getResource().orElse(null);
        if(resourceConfig != null && resourceConfig.getSecrets() != null) {
            return resourceConfig.getSecrets();
        }
        return null;
    }

    protected abstract String getAnnotationKey();

    protected abstract Map<String, String> generateData(String key);
}
