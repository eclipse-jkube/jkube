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
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;
import org.eclipse.jkube.kit.enricher.api.util.SecretConstants;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class DockerRegistrySecretEnricher extends SecretEnricher {
    /**
     * @deprecated Use <code>jkube.eclipse.org/dockerServerId</code> instead.
     */
    @Deprecated
    private static final String ANNOTATION_KEY = "maven.jkube.io/dockerServerId";
    private static final String DOCKER_SERVER_ID_ANNOTATION_KEY = INTERNAL_ANNOTATION_PREFIX + "/dockerServerId";
    private static final String ENRICHER_NAME = "jkube-docker-registry-secret";


    public DockerRegistrySecretEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, ENRICHER_NAME);
    }

    @Override
    protected String getAnnotationKey() {
        return DOCKER_SERVER_ID_ANNOTATION_KEY;
    }

    @Override
    protected String getDeprecatedAnnotationKey() {
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

