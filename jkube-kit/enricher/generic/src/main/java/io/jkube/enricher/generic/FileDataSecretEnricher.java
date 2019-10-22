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
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.jkube.kit.common.util.Base64Util;
import io.jkube.kit.config.resource.PlatformMode;
import io.jkube.maven.enricher.api.BaseEnricher;
import io.jkube.maven.enricher.api.MavenEnricherContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class FileDataSecretEnricher extends BaseEnricher {

    protected static final String PREFIX_ANNOTATION = "maven.jkube.io/secret/";

    public FileDataSecretEnricher(MavenEnricherContext buildContext) {
        super(buildContext, "jkube-secret-file");
    }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
        addAnnotations(builder);
    }

    private void addAnnotations(KubernetesListBuilder builder) {
        builder.accept(new TypedVisitor<SecretBuilder>() {

            @Override
            public void visit(SecretBuilder element) {
                final Map<String, String> annotations = element.buildMetadata().getAnnotations();
                try {
                    final Map<String, String> secretAnnotations = createSecretFromAnnotations(annotations);
                    element.addToData(secretAnnotations);
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        });
    }

    private Map<String, String> createSecretFromAnnotations(final Map<String, String> annotations) throws IOException {
        final Set<Map.Entry<String, String>> entries = annotations.entrySet();
        final Map<String, String> secretFileLocations = new HashMap<>();

        for(Iterator<Map.Entry<String, String>> it = entries.iterator(); it.hasNext(); ) {
            Map.Entry<String, String> entry = it.next();
            final String key = entry.getKey();

            if(key.startsWith(PREFIX_ANNOTATION)) {
                byte[] bytes = readContent(entry.getValue());
                secretFileLocations.put(getOutput(key), Base64Util.encodeToString(bytes));
                it.remove();
            }
        }

        return secretFileLocations;
    }

    private byte[] readContent(String location) throws IOException {
        return Files.readAllBytes(Paths.get(location));
    }

    private String getOutput(String key) {
        return key.substring(PREFIX_ANNOTATION.length());
    }
}
