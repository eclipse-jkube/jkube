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

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import java.util.List;
import java.util.Map;

import static org.eclipse.jkube.kit.enricher.api.util.Constants.RESOURCE_SOURCE_URL_ANNOTATION;


/**
 * Removes any build time annotations on resources
 */
public class RemoveBuildAnnotationsEnricher extends BaseEnricher {

    public RemoveBuildAnnotationsEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, "jkube-remove-build-annotations");
    }

    @Override
    public void enrich(PlatformMode platformMode, KubernetesListBuilder builder) {
        List<HasMetadata> items = builder.buildItems();

        for (HasMetadata item : items) {
            removeBuildAnnotations(item);
        }
    }

    private void removeBuildAnnotations(HasMetadata item) {
        if (item != null) {
            ObjectMeta metadata = item.getMetadata();
            if (metadata != null) {
                Map<String, String> annotations = metadata.getAnnotations();
                if (annotations != null) {
                    annotations.remove(RESOURCE_SOURCE_URL_ANNOTATION);
                }
            }
        }
    }
}
