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
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigSpec;
import org.eclipse.jkube.kit.common.util.MapUtil;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.maven.enricher.api.BaseEnricher;
import org.eclipse.jkube.maven.enricher.api.JKubeEnricherContext;

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

        List<HasMetadata> items = builder.getItems();
        for (HasMetadata item : items) {
            if (platformMode == PlatformMode.kubernetes && item instanceof Deployment) {
                Deployment deployment = (Deployment) item;
                ObjectMeta metadata = deployment.getMetadata();
                DeploymentSpec spec = deployment.getSpec();
                if (metadata != null && spec != null) {
                    PodTemplateSpec template = spec.getTemplate();
                    if (template != null) {
                        ObjectMeta templateMetadata = template.getMetadata();
                        if (templateMetadata == null) {
                            templateMetadata = new ObjectMeta();
                            template.setMetadata(templateMetadata);
                        }
                        templateMetadata.setAnnotations(MapUtil.mergeMaps(templateMetadata.getAnnotations(), metadata.getAnnotations()));
                    }
                }
            } else if (platformMode == PlatformMode.openshift && item instanceof DeploymentConfig) {
                DeploymentConfig deploymentConfig = (DeploymentConfig)item;
                ObjectMeta metadata = deploymentConfig.getMetadata();
                DeploymentConfigSpec spec = deploymentConfig.getSpec();
                if (metadata != null && spec != null) {
                    PodTemplateSpec templateSpec = spec.getTemplate();
                    if(templateSpec != null) {
                        ObjectMeta templateMetadata = templateSpec.getMetadata();
                        if(templateMetadata == null) {
                            templateMetadata = new ObjectMeta();
                            templateSpec.setMetadata(templateMetadata);
                        }
                        templateMetadata.setAnnotations(MapUtil.mergeMaps(templateMetadata.getAnnotations(), metadata.getAnnotations()));
                    }
                }
            }
        }
        builder.withItems(items);
    }
}
