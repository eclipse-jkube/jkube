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
package org.eclipse.jkube.kit.enricher.api.visitor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.jkube.kit.config.resource.MetaDataConfig;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.builder.VisitableBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaFluent;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecFluent;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerFluent;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccountFluent;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceFluent;
import io.fabric8.kubernetes.api.model.apps.DaemonSetBuilder;
import io.fabric8.kubernetes.api.model.apps.DaemonSetFluent;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentFluent;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetFluent;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetFluent;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobFluent;
import io.fabric8.kubernetes.api.model.extensions.IngressBuilder;
import io.fabric8.kubernetes.api.model.extensions.IngressFluent;
import io.fabric8.openshift.api.model.BuildBuilder;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.fabric8.openshift.api.model.BuildConfigFluent;
import io.fabric8.openshift.api.model.BuildFluent;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigFluent;
import io.fabric8.openshift.api.model.ImageStreamBuilder;
import io.fabric8.openshift.api.model.ImageStreamFluent;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.api.model.RouteFluent;
import lombok.AllArgsConstructor;

import static org.eclipse.jkube.kit.common.util.PropertiesUtil.toMap;

/**
 * Visitor which adds labels and annotations
 */
@AllArgsConstructor
public class MetadataVisitor<T extends VisitableBuilder> extends TypedVisitor<T> {

  private final Class<T> clazz;
  private final Supplier<Properties> annotationSupplier;
  private final Supplier<Properties> labelSupplier;
  private final Function<T, ObjectMetaFluent<?>> objectMeta;
  private final Function<ObjectMetaFluent<?>, Runnable> endMetadata;

  public MetadataVisitor(
      Class<T> clazz,
      Supplier<Properties> annotationSupplier,
      Supplier<Properties> labelSupplier,
      Function<T, ObjectMetaFluent<?>> objectMeta) {
    this(clazz, annotationSupplier, labelSupplier, objectMeta, null);
  }

  @Override
  public Class<T> getType() {
    return clazz;
  }

  @Override
  public void visit(T item) {
    final ObjectMetaFluent<?> omf = objectMeta.apply(item);
    omf.withAnnotations(overlayMap(annotationSupplier.get(), omf.getAnnotations()))
        .withLabels(overlayMap(labelSupplier.get(), omf.getLabels()));
    Optional.ofNullable(endMetadata).map(em -> em.apply(omf)).ifPresent(Runnable::run);
  }

  private Map<String, String> overlayMap(Properties properties, Map<String, String> originalMap) {
    final Map<String, String> ret = new HashMap<>(Optional.ofNullable(originalMap).orElse(Collections.emptyMap()));
    for (Map.Entry<String, String> entry : toMap(properties).entrySet()) {
      ret.putIfAbsent(entry.getKey(), appendTrailingNewLineIfMultiline(entry.getValue()));
    }
    return ret;
  }

  private static MetaDataConfig getAnnotations(ResourceConfig resourceConfig) {
    return Optional.ofNullable(resourceConfig).map(ResourceConfig::getAnnotations).orElse(new MetaDataConfig());
  }

  private static MetaDataConfig getLabels(ResourceConfig resourceConfig) {
    return Optional.ofNullable(resourceConfig).map(ResourceConfig::getLabels).orElse(new MetaDataConfig());
  }

  private String appendTrailingNewLineIfMultiline(String value) {
    if (value.contains(System.lineSeparator()) && !value.endsWith(System.lineSeparator())) {
      return value + System.lineSeparator();
    }
    return value;
  }

  // =======================================================================================

  public static MetadataVisitor<ObjectMetaBuilder> metadata(ResourceConfig resourceConfig) {
    return new MetadataVisitor<>(
        ObjectMetaBuilder.class,
        getAnnotations(resourceConfig)::getAll, getLabels(resourceConfig)::getAll,
        omb -> omb);
  }

  public static MetadataVisitor<DeploymentBuilder> deployment(ResourceConfig resourceConfig) {
    return new MetadataVisitor<>(
        DeploymentBuilder.class,
        getAnnotations(resourceConfig)::getDeployment, getLabels(resourceConfig)::getDeployment,
        DeploymentFluent::editOrNewMetadata, omf -> ((DeploymentFluent<?>.MetadataNested<?>) omf)::endMetadata);
  }

  public static MetadataVisitor<io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder> extensionsDeployment(
      ResourceConfig resourceConfig) {
    return new MetadataVisitor<>(
        io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder.class,
        getAnnotations(resourceConfig)::getDeployment, getLabels(resourceConfig)::getDeployment,
        io.fabric8.kubernetes.api.model.extensions.DeploymentFluent::editOrNewMetadata,
        omf -> ((io.fabric8.kubernetes.api.model.extensions.DeploymentFluent<?>.MetadataNested<?>) omf)::endMetadata);
  }

  public static MetadataVisitor<DeploymentConfigBuilder> deploymentConfig(ResourceConfig resourceConfig) {
    return new MetadataVisitor<>(
        DeploymentConfigBuilder.class,
        getAnnotations(resourceConfig)::getDeployment, getLabels(resourceConfig)::getDeployment,
        DeploymentConfigFluent::editOrNewMetadata, omf -> ((DeploymentConfigFluent<?>.MetadataNested<?>) omf)::endMetadata);
  }

  public static MetadataVisitor<ReplicaSetBuilder> replicaSet(ResourceConfig resourceConfig) {
    return new MetadataVisitor<>(
        ReplicaSetBuilder.class,
        getAnnotations(resourceConfig)::getReplicaSet, getLabels(resourceConfig)::getReplicaSet,
        ReplicaSetFluent::editOrNewMetadata, omf -> ((ReplicaSetFluent<?>.MetadataNested<?>) omf)::endMetadata);
  }

  public static MetadataVisitor<ReplicationControllerBuilder> replicationController(ResourceConfig resourceConfig) {
    return new MetadataVisitor<>(
        ReplicationControllerBuilder.class,
        getAnnotations(resourceConfig)::getReplicaSet, getLabels(resourceConfig)::getReplicaSet,
        ReplicationControllerFluent::editOrNewMetadata,
        omf -> ((ReplicationControllerFluent<?>.MetadataNested<?>) omf)::endMetadata);
  }

  public static MetadataVisitor<ServiceBuilder> service(ResourceConfig resourceConfig) {
    return new MetadataVisitor<>(
        ServiceBuilder.class,
        getAnnotations(resourceConfig)::getService, getLabels(resourceConfig)::getService,
        ServiceFluent::editOrNewMetadata, omf -> ((ServiceFluent<?>.MetadataNested<?>) omf)::endMetadata);
  }

  public static MetadataVisitor<PodTemplateSpecBuilder> podTemplateSpec(ResourceConfig resourceConfig) {
    return new MetadataVisitor<>(
        PodTemplateSpecBuilder.class,
        getAnnotations(resourceConfig)::getPod, getLabels(resourceConfig)::getPod,
        PodTemplateSpecFluent::editOrNewMetadata, omf -> ((PodTemplateSpecFluent<?>.MetadataNested<?>) omf)::endMetadata);
  }

  public static MetadataVisitor<DaemonSetBuilder> daemonSet(ResourceConfig resourceConfig) {
    return new MetadataVisitor<>(
        DaemonSetBuilder.class,
        getAnnotations(resourceConfig)::getAll, getLabels(resourceConfig)::getAll,
        DaemonSetFluent::editOrNewMetadata, omf -> ((DaemonSetFluent<?>.MetadataNested<?>) omf)::endMetadata);
  }

  public static MetadataVisitor<StatefulSetBuilder> statefulSet(ResourceConfig resourceConfig) {
    return new MetadataVisitor<>(
        StatefulSetBuilder.class,
        getAnnotations(resourceConfig)::getAll, getLabels(resourceConfig)::getAll,
        StatefulSetFluent::editOrNewMetadata, omf -> ((StatefulSetFluent<?>.MetadataNested<?>) omf)::endMetadata);
  }

  public static MetadataVisitor<JobBuilder> job(ResourceConfig resourceConfig) {
    return new MetadataVisitor<>(
        JobBuilder.class,
        getAnnotations(resourceConfig)::getAll, getLabels(resourceConfig)::getAll,
        JobFluent::editOrNewMetadata, omf -> ((JobFluent<?>.MetadataNested<?>) omf)::endMetadata);
  }

  public static MetadataVisitor<ImageStreamBuilder> imageStream(ResourceConfig resourceConfig) {
    return new MetadataVisitor<>(
        ImageStreamBuilder.class,
        getAnnotations(resourceConfig)::getAll, getLabels(resourceConfig)::getAll,
        ImageStreamFluent::editOrNewMetadata, omf -> ((ImageStreamFluent<?>.MetadataNested<?>) omf)::endMetadata);
  }

  public static MetadataVisitor<BuildConfigBuilder> buildConfig(ResourceConfig resourceConfig) {
    return new MetadataVisitor<>(
        BuildConfigBuilder.class,
        getAnnotations(resourceConfig)::getAll, getLabels(resourceConfig)::getAll,
        BuildConfigFluent::editOrNewMetadata, omf -> ((BuildConfigFluent<?>.MetadataNested<?>) omf)::endMetadata);
  }

  public static MetadataVisitor<BuildBuilder> build(ResourceConfig resourceConfig) {
    return new MetadataVisitor<>(
        BuildBuilder.class,
        getAnnotations(resourceConfig)::getAll, getLabels(resourceConfig)::getAll,
        BuildFluent::editOrNewMetadata, omf -> ((BuildFluent<?>.MetadataNested<?>) omf)::endMetadata);
  }

  public static MetadataVisitor<IngressBuilder> extensionsIngress(ResourceConfig resourceConfig) {
    return new MetadataVisitor<>(
        IngressBuilder.class,
        getAnnotations(resourceConfig)::getIngress, getLabels(resourceConfig)::getIngress,
        IngressFluent::editOrNewMetadata, omf -> ((IngressFluent<?>.MetadataNested<?>) omf)::endMetadata);
  }

  public static MetadataVisitor<io.fabric8.kubernetes.api.model.networking.v1beta1.IngressBuilder> ingressV1beta1(
      ResourceConfig resourceConfig) {
    return new MetadataVisitor<>(
        io.fabric8.kubernetes.api.model.networking.v1beta1.IngressBuilder.class,
        getAnnotations(resourceConfig)::getIngress, getLabels(resourceConfig)::getIngress,
        io.fabric8.kubernetes.api.model.networking.v1beta1.IngressFluent::editOrNewMetadata,
        omf -> ((io.fabric8.kubernetes.api.model.networking.v1beta1.IngressFluent<?>.MetadataNested<?>) omf)::endMetadata);
  }

  public static MetadataVisitor<io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder> ingressV1(
      ResourceConfig resourceConfig) {
    return new MetadataVisitor<>(
        io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder.class,
        getAnnotations(resourceConfig)::getIngress, getLabels(resourceConfig)::getIngress,
        io.fabric8.kubernetes.api.model.networking.v1.IngressFluent::editOrNewMetadata,
        omf -> ((io.fabric8.kubernetes.api.model.networking.v1.IngressFluent<?>.MetadataNested<?>) omf)::endMetadata);
  }

  public static MetadataVisitor<ServiceAccountBuilder> serviceAccount(ResourceConfig resourceConfig) {
    return new MetadataVisitor<>(
        ServiceAccountBuilder.class,
        getAnnotations(resourceConfig)::getServiceAccount, getLabels(resourceConfig)::getServiceAccount,
        ServiceAccountFluent::editOrNewMetadata, omf -> ((ServiceAccountFluent<?>.MetadataNested<?>) omf)::endMetadata);
  }

  public static MetadataVisitor<RouteBuilder> route(ResourceConfig resourceConfig) {
    return new MetadataVisitor<>(RouteBuilder.class,
        getAnnotations(resourceConfig)::getRoute, getLabels(resourceConfig)::getRoute,
        RouteFluent::editOrNewMetadata, omf -> ((RouteFluent<?>.MetadataNested<?>) omf)::endMetadata);
  }

}
