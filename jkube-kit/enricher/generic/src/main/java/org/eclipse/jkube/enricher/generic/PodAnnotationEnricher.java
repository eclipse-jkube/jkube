/*
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
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import org.eclipse.jkube.kit.common.util.MapUtil;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.EnricherContext;
import org.eclipse.jkube.kit.enricher.handler.ControllerHandler;

import java.util.List;

/**
 * Enricher which copies the annotation from a Deployment to the annotations of
 * container Pod spec.
 */
public class PodAnnotationEnricher extends BaseEnricher {
    public PodAnnotationEnricher(EnricherContext buildContext) {
        super(buildContext, "jkube-pod-annotations");
    }

    @Override
    public void enrich(PlatformMode platformMode, KubernetesListBuilder builder) {
        super.enrich(platformMode, builder);
        final List<HasMetadata> items = builder.buildItems();
        for (HasMetadata item : items) {
            final ControllerHandler<HasMetadata> controllerHandler = getContext().getHandlerHub().getHandlerFor(item);
            if (controllerHandler != null) {
                final PodTemplateSpec template = controllerHandler.getPodTemplate(item);
                if (template != null) {
                    if (template.getMetadata() == null) {
                        template.setMetadata(new ObjectMeta());
                    }
                    final ObjectMeta templateMetadata = template.getMetadata();
                    templateMetadata.setAnnotations(MapUtil.mergeMaps(templateMetadata.getAnnotations(), item.getMetadata().getAnnotations()));
                }
            }
        }
        builder.withItems(items);
    }
}
