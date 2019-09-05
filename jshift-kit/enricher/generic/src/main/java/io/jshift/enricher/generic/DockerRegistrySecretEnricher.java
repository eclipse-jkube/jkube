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

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.jshift.maven.enricher.api.MavenEnricherContext;
import io.jshift.maven.enricher.api.model.Configuration;
import io.jshift.maven.enricher.api.util.SecretConstants;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class DockerRegistrySecretEnricher extends SecretEnricher {
    final private static String ANNOTATION_KEY = "maven.jshift.io/dockerServerId";
    final private static String ENRICHER_NAME = "jshift-docker-registry-secret";


    public DockerRegistrySecretEnricher(MavenEnricherContext buildContext) {
        super(buildContext, ENRICHER_NAME);
    }

    @Override
    protected String getAnnotationKey() {
        return ANNOTATION_KEY;
    }

    @Override
    protected Map<String, String> generateData(String dockerId) {
        final Configuration config = getContext().getConfiguration();
        final Optional<Map<String,Object>> secretConfig = config.getSecretConfiguration(dockerId);
        if (!secretConfig.isPresent()) {
            return null;
        }

        JsonObject params = new JsonObject();
        for (String key : new String[] { "username", "password", "email" }) {
            if  (secretConfig.get().containsKey(key)) {
                params.add(key, new JsonPrimitive(secretConfig.get().get(key).toString()));
            }
        }

        JsonObject ret = new JsonObject();
        ret.add(dockerId, params);
        return Collections.singletonMap(
            SecretConstants.DOCKER_DATA_KEY,
            encode(ret.toString()));
    }
}

