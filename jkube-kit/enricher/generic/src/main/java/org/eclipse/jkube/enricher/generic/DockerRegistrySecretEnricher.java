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

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.eclipse.jkube.maven.enricher.api.JkubeEnricherContext;
import org.eclipse.jkube.maven.enricher.api.model.Configuration;
import org.eclipse.jkube.maven.enricher.api.util.SecretConstants;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class DockerRegistrySecretEnricher extends SecretEnricher {
    final private static String ANNOTATION_KEY = "maven.jkube.io/dockerServerId";
    final private static String ENRICHER_NAME = "jkube-docker-registry-secret";


    public DockerRegistrySecretEnricher(JkubeEnricherContext buildContext) {
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

