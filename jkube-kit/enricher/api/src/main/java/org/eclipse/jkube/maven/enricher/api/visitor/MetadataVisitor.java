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
package org.eclipse.jkube.maven.enricher.api.visitor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.eclipse.jkube.kit.config.resource.MetaDataConfig;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.maven.enricher.api.Kind;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaFluent;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.DaemonSetBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.batch.JobBuilder;
import io.fabric8.kubernetes.api.model.extensions.IngressBuilder;
import io.fabric8.openshift.api.model.BuildBuilder;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.ImageStreamBuilder;

/**
 * Visitor which adds labels and annotations
 *
 * @author roland
 * @since 02/05/16
 */
public abstract class MetadataVisitor<T> extends TypedVisitor<T> {

    private static ThreadLocal<ProcessorConfig> configHolder = new ThreadLocal<>();
    private final Map<String, String> labelsFromConfig;
    private final Map<String, String> annotationFromConfig;

    private MetadataVisitor(ResourceConfig resourceConfig) {
        if (resourceConfig != null) {
            labelsFromConfig = getMapFromConfiguration(resourceConfig.getLabels(), getKind());
            annotationFromConfig = getMapFromConfiguration(resourceConfig.getAnnotations(), getKind());
        } else {
            labelsFromConfig = new HashMap<>();
            annotationFromConfig = new HashMap<>();
        }
    }

    public static void setProcessorConfig(ProcessorConfig config) {
        configHolder.set(config);
    }

    public static void clearProcessorConfig() {
        configHolder.remove();
    }

    public void visit(T item) {
        ObjectMeta metadata = getOrCreateMetadata(item);
        updateLabels(metadata);
        updateAnnotations(metadata);
    }

    private void updateLabels(ObjectMeta metadata) {
        overlayMap(metadata.getLabels(),labelsFromConfig);
    }

    private void updateAnnotations(ObjectMeta metadata) {
        overlayMap(metadata.getAnnotations(),annotationFromConfig);
    }

    private Map<String, String> getMapFromConfiguration(MetaDataConfig config, Kind kind) {
        if (config == null) {
            return new HashMap<>();
        }
        Map<String, String> ret;
        if (kind == Kind.SERVICE) {
            ret = propertiesToMap(config.getService());
        } else if (kind == Kind.DEPLOYMENT || kind == Kind.DEPLOYMENT_CONFIG) {
            ret = propertiesToMap(config.getDeployment());
        } else if (kind == Kind.REPLICATION_CONTROLLER || kind == Kind.REPLICA_SET) {
            ret = propertiesToMap(config.getReplicaSet());
        } else if (kind == Kind.POD_SPEC) {
            ret = propertiesToMap(config.getPod());
        } else if (kind == Kind.INGRESS) {
            ret = propertiesToMap(config.getIngress());
        } else {
            ret = new HashMap<>();
        }
        if (config.getAll() != null) {
            ret.putAll(propertiesToMap(config.getAll()));
        }
        return ret;
    }

    private Map<String, String> propertiesToMap(Properties properties) {
        Map<String, String> propertyMap = new HashMap<>();
        if(properties != null) {
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                propertyMap.put(entry.getKey().toString(), entry.getValue().toString());
            }
        }
        return propertyMap;
    }


    private void overlayMap(Map<String, String> targetMap, Map<String, String> enrichMap) {
        targetMap = ensureMap(targetMap);
        enrichMap = ensureMap(enrichMap);
        for (Map.Entry<String, String> entry : enrichMap.entrySet()) {
            if (!targetMap.containsKey(entry.getKey())) {
                targetMap.put(entry.getKey(), entry.getValue());
            }
        }
    }

    protected abstract Kind getKind();
    protected abstract ObjectMeta getOrCreateMetadata(T item);

    private Map<String, String> ensureMap(Map<String, String> labels) {
        return labels != null ? labels : new HashMap<>();
    }


    // =======================================================================================

    public static class PodTemplateSpecBuilderVisitor extends MetadataVisitor<PodTemplateSpecBuilder> {

        public PodTemplateSpecBuilderVisitor(ResourceConfig resourceConfig) {
            super(resourceConfig);
        }

        @Override
        protected Kind getKind() {
            return Kind.POD_SPEC;
        }

        @Override
        protected ObjectMeta getOrCreateMetadata(PodTemplateSpecBuilder item) {
            return addEmptyLabelsAndAnnotations(item::hasMetadata, item::withNewMetadata, item::editMetadata, item::buildMetadata)
                    .endMetadata().buildMetadata();
        }
    }

    public static class ServiceBuilderVisitor extends MetadataVisitor<ServiceBuilder> {

        public ServiceBuilderVisitor(ResourceConfig resourceConfig) {
            super(resourceConfig);
        }

        @Override
        protected Kind getKind() {
            return Kind.SERVICE;
        }

        @Override
        protected ObjectMeta getOrCreateMetadata(ServiceBuilder item) {
            return addEmptyLabelsAndAnnotations(item::hasMetadata, item::withNewMetadata, item::editMetadata, item::buildMetadata)
                    .endMetadata().buildMetadata();
        }
    }

    public static class ReplicaSet extends MetadataVisitor<ReplicaSetBuilder> {
        public ReplicaSet(ResourceConfig resourceConfig) {
            super(resourceConfig);
        }

        @Override
        protected Kind getKind() {
            return Kind.REPLICA_SET;
        }

        @Override
        protected ObjectMeta getOrCreateMetadata(ReplicaSetBuilder item) {
            return addEmptyLabelsAndAnnotations(item::hasMetadata, item::withNewMetadata, item::editMetadata, item::buildMetadata)
                    .endMetadata().buildMetadata();
        }
    }

    public static class ReplicationControllerBuilderVisitor extends MetadataVisitor<ReplicationControllerBuilder> {
        public ReplicationControllerBuilderVisitor(ResourceConfig resourceConfig) {
            super(resourceConfig);
        }

        @Override
        protected Kind getKind() {
            return Kind.REPLICATION_CONTROLLER;
        }

        @Override
        protected ObjectMeta getOrCreateMetadata(ReplicationControllerBuilder item) {
            return addEmptyLabelsAndAnnotations(item::hasMetadata, item::withNewMetadata, item::editMetadata, item::buildMetadata)
                    .endMetadata().buildMetadata();
        }
    }

    public static class DeploymentBuilderVisitor extends MetadataVisitor<DeploymentBuilder> {
        public DeploymentBuilderVisitor(ResourceConfig resourceConfig) {
            super(resourceConfig);
        }

        @Override
        protected Kind getKind() {
            return Kind.DEPLOYMENT;
        }

        @Override
        protected ObjectMeta getOrCreateMetadata(DeploymentBuilder item) {
            return addEmptyLabelsAndAnnotations(item::hasMetadata, item::withNewMetadata, item::editMetadata, item::buildMetadata)
                    .endMetadata().buildMetadata();
        }
    }

    public static class DeploymentConfigBuilderVisitor extends MetadataVisitor<DeploymentConfigBuilder> {
        public DeploymentConfigBuilderVisitor(ResourceConfig resourceConfig) {
            super(resourceConfig);
        }

        @Override
        protected Kind getKind() {
            return Kind.DEPLOYMENT;
        }

        @Override
        protected ObjectMeta getOrCreateMetadata(DeploymentConfigBuilder item) {
            return addEmptyLabelsAndAnnotations(item::hasMetadata, item::withNewMetadata, item::editMetadata, item::buildMetadata)
                    .endMetadata().buildMetadata();
        }
    }

    public static class DaemonSetBuilderVisitor extends MetadataVisitor<DaemonSetBuilder> {
        public DaemonSetBuilderVisitor(ResourceConfig resourceConfig) {
            super(resourceConfig);
        }

        @Override
        protected Kind getKind() {
            return Kind.DAEMON_SET;
        }

        @Override
        protected ObjectMeta getOrCreateMetadata(DaemonSetBuilder item) {
            return addEmptyLabelsAndAnnotations(item::hasMetadata, item::withNewMetadata, item::editMetadata, item::buildMetadata)
                    .endMetadata().buildMetadata();
        }
    }

    public static class StatefulSetBuilderVisitor extends MetadataVisitor<StatefulSetBuilder> {
        public StatefulSetBuilderVisitor(ResourceConfig resourceConfig) {
            super(resourceConfig);
        }

        @Override
        protected Kind getKind() {
            return Kind.STATEFUL_SET;
        }

        @Override
        protected ObjectMeta getOrCreateMetadata(StatefulSetBuilder item) {
            return addEmptyLabelsAndAnnotations(item::hasMetadata, item::withNewMetadata, item::editMetadata, item::buildMetadata)
                    .endMetadata().buildMetadata();
        }
    }

    public static class JobBuilderVisitor extends MetadataVisitor<JobBuilder> {
        public JobBuilderVisitor(ResourceConfig resourceConfig) {
            super(resourceConfig);
        }

        @Override
        protected Kind getKind() {
            return Kind.JOB;
        }

        @Override
        protected ObjectMeta getOrCreateMetadata(JobBuilder item) {
            return addEmptyLabelsAndAnnotations(item::hasMetadata, item::withNewMetadata, item::editMetadata, item::buildMetadata)
                    .endMetadata().buildMetadata();
        }
    }

    public static class ImageStreamBuilderVisitor extends MetadataVisitor<ImageStreamBuilder> {
        public ImageStreamBuilderVisitor(ResourceConfig resourceConfig) {
            super(resourceConfig);
        }

        @Override
        protected Kind getKind() {
            return Kind.IMAGESTREAM;
        }

        @Override
        protected ObjectMeta getOrCreateMetadata(ImageStreamBuilder item) {
            return addEmptyLabelsAndAnnotations(item::hasMetadata, item::withNewMetadata, item::editMetadata, item::buildMetadata)
                    .endMetadata().buildMetadata();
        }
    }

    public static class BuildConfigBuilderVisitor extends MetadataVisitor<BuildConfigBuilder> {
        public BuildConfigBuilderVisitor(ResourceConfig resourceConfig) {
            super(resourceConfig);
        }

        @Override
        protected Kind getKind() {
            return Kind.BUILD_CONFIG;
        }

        @Override
        protected ObjectMeta getOrCreateMetadata(BuildConfigBuilder item) {
            return addEmptyLabelsAndAnnotations(item::hasMetadata, item::withNewMetadata, item::editMetadata, item::buildMetadata)
                    .endMetadata().buildMetadata();
        }
    }

    public static class BuildBuilderVisitor extends MetadataVisitor<BuildBuilder> {
        public BuildBuilderVisitor(ResourceConfig resourceConfig) {
            super(resourceConfig);
        }

        @Override
        protected Kind getKind() {
            return Kind.BUILD;
        }

        @Override
        protected ObjectMeta getOrCreateMetadata(BuildBuilder item) {
            return addEmptyLabelsAndAnnotations(item::hasMetadata, item::withNewMetadata, item::editMetadata, item::buildMetadata)
                    .endMetadata().buildMetadata();
        }
    }

    public static class IngressBuilderVisitor extends MetadataVisitor<IngressBuilder> {
        public IngressBuilderVisitor(ResourceConfig resourceConfig) {
            super(resourceConfig);
        }

        @Override
        protected Kind getKind() {
            return Kind.BUILD;
        }

        @Override
        protected ObjectMeta getOrCreateMetadata(IngressBuilder item) {
            return addEmptyLabelsAndAnnotations(item::hasMetadata, item::withNewMetadata, item::editMetadata, item::buildMetadata)
                    .endMetadata().buildMetadata();
        }
    }

    private static <T extends ObjectMetaFluent<?>> T addEmptyLabelsAndAnnotations(
        BooleanSupplier hasMetadata, Supplier<T> withNewMetadata, Supplier<T> editMetadata, Supplier<ObjectMeta> buildMetadata) {
        final T ret;
        if (hasMetadata.getAsBoolean()) {
            ret = editMetadata.get();
            if (buildMetadata.get().getLabels() == null) {
                ret.withLabels(Collections.emptyMap());
            }
            if (buildMetadata.get().getAnnotations() == null) {
                ret.withAnnotations(Collections.emptyMap());
            }
        } else {
            ret = withNewMetadata.get();
            ret.withLabels(Collections.emptyMap()).withAnnotations(Collections.emptyMap());
        }
        return ret;
    }
}