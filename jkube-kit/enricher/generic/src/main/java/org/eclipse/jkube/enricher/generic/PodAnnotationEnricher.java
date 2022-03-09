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
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import org.eclipse.jkube.kit.common.util.MapUtil;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.handler.ControllerHandler;
import org.eclipse.jkube.kit.enricher.handler.HandlerHub;

import java.util.List;

/**
 * Enricher which copies the annotation from a Deployment to the annotations of
 * container Pod spec.
 */
public class PodAnnotationEnricher extends BaseEnricher {
    public PodAnnotationEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, "jkube-pod-annotations");
    }

    @Override
    public void enrich(PlatformMode platformMode, KubernetesListBuilder builder) {
        super.enrich(platformMode, builder);

        List<HasMetadata> items = builder.buildItems();
        HandlerHub handlerHub = new HandlerHub(getContext().getGav(), getContext().getProperties());
        for (HasMetadata item : items) {
            ObjectMeta metadata = item.getMetadata();
            ControllerHandler<HasMetadata> controllerHandler = handlerHub.getHandlerFor(item);
            if (controllerHandler != null) {
                PodTemplateSpec template = controllerHandler.getPodTemplate(item);
                if (template != null) {
                    ObjectMeta templateMetadata = template.getMetadata();
                    if (templateMetadata == null) {
                        templateMetadata = new ObjectMeta();
                        template.setMetadata(templateMetadata);
                    }
                    templateMetadata.setAnnotations(MapUtil.mergeMaps(templateMetadata.getAnnotations(), metadata.getAnnotations()));
                }
            }
        }
        builder.withItems(items);
    }
}
