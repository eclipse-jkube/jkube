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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import org.eclipse.jkube.kit.config.image.ImageName;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.builder.Visitable;
import io.fabric8.kubernetes.api.builder.VisitableBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.DaemonSetBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.openshift.api.model.ImageChangeTrigger;
import io.fabric8.openshift.api.model.ImageChangeTriggerBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * This adds a `image.openshift.io/triggers` tag to all kubernetes resources in order to make them run on OpenShift when using ImageStreams.
 *
 * @author nicola
 */
public class TriggersAnnotationEnricher extends BaseEnricher {

    private static final String TRIGGERS_ANNOTATION = "image.openshift.io/triggers";

    @AllArgsConstructor
    private enum Config implements Configs.Config {

        /**
         * Comma-separated list of container names that should be enriched (default all that apply)
         */
        CONTAINERS("containers");

        @Getter
        protected String key;
    }


    public TriggersAnnotationEnricher(JKubeEnricherContext buildContext) {
        super(buildContext, "jkube-triggers-annotation");
    }

    @Override
    public void enrich(PlatformMode platformMode, KubernetesListBuilder builder) {

        builder.accept(new TypedVisitor<StatefulSetBuilder>() {
            @Override
            public void visit(StatefulSetBuilder o) {
                StatefulSet s = o.build();
                if (canWriteTriggers(s)) {
                    o.withMetadata(getMetaEnrichedWithTriggers(s.getMetadata(), o));
                }
            }
        });

        builder.accept(new TypedVisitor<ReplicaSetBuilder>() {
            @Override
            public void visit(ReplicaSetBuilder o) {
                ReplicaSet s = o.build();
                if (canWriteTriggers(s)) {
                    o.withMetadata(getMetaEnrichedWithTriggers(s.getMetadata(), o));
                }
            }
        });

        builder.accept(new TypedVisitor<DaemonSetBuilder>() {
            @Override
            public void visit(DaemonSetBuilder o) {
                DaemonSet s = o.build();
                if (canWriteTriggers(s)) {
                    o.withMetadata(getMetaEnrichedWithTriggers(s.getMetadata(), o));
                }
            }
        });

    }

    protected ObjectMeta getMetaEnrichedWithTriggers(ObjectMeta meta, VisitableBuilder<?, ?> o) {
        ObjectMetaBuilder metaBuilder;
        if (meta != null) {
            metaBuilder = new ObjectMetaBuilder(meta);
        } else {
            metaBuilder = new ObjectMetaBuilder();
        }

        return metaBuilder
                .addToAnnotations(TRIGGERS_ANNOTATION, createAnnotation(o))
                .build();
    }

    protected boolean canWriteTriggers(HasMetadata res) {
        return res.getMetadata() == null ||
                res.getMetadata().getAnnotations() == null ||
                !res.getMetadata().getAnnotations().containsKey(TRIGGERS_ANNOTATION);
    }

    protected String createAnnotation(Visitable<?> builder) {
        final List<ImageChangeTrigger> triggerList = new ArrayList<>();
        builder.accept(new TypedVisitor<ContainerBuilder>() {
            @Override
            public void visit(ContainerBuilder cb) {
                Container container = cb.build();
                String containerName = container.getName();
                String containerImage = container.getImage();
                ImageName image = new ImageName(containerImage);
                if (isContainerAllowed(containerName) && image.getRegistry() == null && image.getUser() == null) {
                    // Imagestreams used as trigger are in the same namespace
                    String tag = image.getTag() != null ? image.getTag() : "latest";

                    ImageChangeTrigger trigger = new ImageChangeTriggerBuilder()
                            .withNewFrom()
                            .withKind("ImageStreamTag")
                            .withName(image.getSimpleName() + ":" + tag)
                            .endFrom()
                            .build();

                    trigger.setAdditionalProperty("fieldPath", "spec.template.spec.containers[?(@.name==\"" + containerName + "\")].image");
                    triggerList.add(trigger);
                }
            }
        });

        try {
            return ResourceUtil.toJson(triggerList);
        } catch (JsonProcessingException e) {
            getLog().error("Error while creating ImageStreamTag triggers for Kubernetes resources: %s", e);
            return "[]";
        }
    }

    protected boolean isContainerAllowed(String containerName) {
        String namesStr = this.getConfig(Config.CONTAINERS);
        Set<String> allowedNames = new HashSet<>();
        if (namesStr != null) {
            for (String name : namesStr.split(",")) {
                allowedNames.add(name.trim());
            }
        }

        return allowedNames.isEmpty() || allowedNames.contains(containerName);
    }

}
