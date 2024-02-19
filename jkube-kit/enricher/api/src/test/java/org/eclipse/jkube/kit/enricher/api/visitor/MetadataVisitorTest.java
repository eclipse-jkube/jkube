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

import java.util.Properties;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import org.eclipse.jkube.kit.config.resource.MetaDataConfig;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.DaemonSetBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.api.model.extensions.IngressBuilder;
import io.fabric8.openshift.api.model.BuildBuilder;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.ImageStreamBuilder;
import io.fabric8.openshift.api.model.RouteBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

class MetadataVisitorTest {

  private ResourceConfig resourceConfig;

  @BeforeEach
  void setUp() {
    final MetaDataConfig annotations = MetaDataConfig.builder()
        .all(new Properties())
        .deployment(new Properties())
        .ingress(new Properties())
        .pod(new Properties())
        .replicaSet(new Properties())
        .service(new Properties())
        .serviceAccount(new Properties())
        .route(new Properties())
        .build();
    annotations.getAll().put("this-is-all", 1);
    annotations.getDeployment().put("deployment", "Yay");
    annotations.getIngress().put("ingress", "Yay");
    annotations.getPod().put("pod", "Yay");
    annotations.getReplicaSet().put("replica-set", "Yay");
    annotations.getService().put("service", "Yay");
    annotations.getServiceAccount().put("service-account", "Yay");
    annotations.getRoute().put("route", "Yay");
    final Properties labelsAll = (Properties) annotations.getAll().clone();
    labelsAll.put("super-label", "S");
    final MetaDataConfig labels = annotations.toBuilder()
        .all(labelsAll)
        .build();
    resourceConfig = ResourceConfig.builder().annotations(annotations).labels(labels).build();
  }

  @Test
  void configMap() {
    // Given
    final KubernetesListBuilder klb = new KubernetesListBuilder();
    klb.addToItems(new ConfigMapBuilder().editOrNewMetadata().endMetadata());
    // When
    klb.accept(MetadataVisitor.metadata(resourceConfig));
    // Then
    assertThat(klb.build().getItems().get(0).getMetadata().getAnnotations())
        .containsOnly(entry("this-is-all", "1"));
    assertThat(klb.build().getItems().get(0).getMetadata().getLabels())
        .containsOnly(entry("this-is-all", "1"), entry("super-label", "S"));
  }

  @Test
  void configMap_nullResourceConfig() {
    // Given
    final KubernetesListBuilder klb = new KubernetesListBuilder();
    klb.addToItems(new ConfigMapBuilder().editOrNewMetadata().endMetadata());
    // When
    klb.accept(MetadataVisitor.metadata(null));
    // Then
    assertThat(klb.build().getItems().get(0).getMetadata().getAnnotations()).isEmpty();
    assertThat(klb.build().getItems().get(0).getMetadata().getLabels()).isEmpty();
  }

  @Test
  void job() {
    // Given
    final KubernetesListBuilder klb = new KubernetesListBuilder();
    klb.addToItems(new JobBuilder().editOrNewMetadata().endMetadata());
    // When
    klb.accept(MetadataVisitor.job(resourceConfig));
    // Then
    assertThat(klb.build().getItems().get(0).getMetadata().getAnnotations())
        .containsOnly(entry("this-is-all", "1"));
    assertThat(klb.build().getItems().get(0).getMetadata().getLabels())
        .containsOnly(entry("this-is-all", "1"), entry("super-label", "S"));
  }

  @Test
  void deployment() {
    // Given
    final DeploymentBuilder db = new DeploymentBuilder();
    // When
    MetadataVisitor.deployment(resourceConfig).visit(db);
    // Then
    assertThat(db.build().getMetadata().getAnnotations()).containsOnly(entry("deployment", "Yay"));
    assertThat(db.build().getMetadata().getLabels()).containsOnly(entry("deployment", "Yay"));
  }

  @Test
  void extensionsDeployment() {
    // Given
    final io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder db =
        new io.fabric8.kubernetes.api.model.extensions.DeploymentBuilder();
    // When
    MetadataVisitor.extensionsDeployment(resourceConfig).visit(db);
    // Then
    assertThat(db.build().getMetadata().getAnnotations()).containsOnly(entry("deployment", "Yay"));
    assertThat(db.build().getMetadata().getLabels()).containsOnly(entry("deployment", "Yay"));
  }

  @Test
  void deploymentConfig() {
    // Given
    final DeploymentConfigBuilder dc = new DeploymentConfigBuilder();
    // When
    MetadataVisitor.deploymentConfig(resourceConfig).visit(dc);
    // Then
    assertThat(dc.build().getMetadata().getAnnotations()).containsOnly(entry("deployment", "Yay"));
    assertThat(dc.build().getMetadata().getLabels()).containsOnly(entry("deployment", "Yay"));
  }

  @Test
  void replicaSetBuilder() {
    // Given
    final ReplicaSetBuilder rs = new ReplicaSetBuilder();
    // When
    MetadataVisitor.replicaSet(resourceConfig).visit(rs);
    // Then
    assertThat(rs.build().getMetadata().getAnnotations())
        .containsOnly(entry("replica-set", "Yay"));
    assertThat(rs.build().getMetadata().getLabels())
        .containsOnly(entry("replica-set", "Yay"));
  }

  @Test
  void replicaSetBuilder_preservesOriginalValues() {
    // Given
    final ReplicaSetBuilder rs = new ReplicaSetBuilder();
    rs.editOrNewMetadata().addToAnnotations("extra", "EXTRA").endMetadata();
    rs.editOrNewMetadata().addToLabels("replica-set", "to-be-preserved").endMetadata();
    // When
    MetadataVisitor.replicaSet(resourceConfig).visit(rs);
    // Then
    assertThat(rs.build().getMetadata().getAnnotations())
        .containsOnly(entry("replica-set", "Yay"), entry("extra", "EXTRA"));
    assertThat(rs.build().getMetadata().getLabels())
        .containsOnly(entry("replica-set", "to-be-preserved"));
  }

  @Test
  void replicationController() {
    // Given
    final ReplicationControllerBuilder rc = new ReplicationControllerBuilder();
    // When
    MetadataVisitor.replicationController(resourceConfig).visit(rc);
    // Then
    assertThat(rc.build().getMetadata().getAnnotations())
        .containsOnly(entry("replica-set", "Yay"));
    assertThat(rc.build().getMetadata().getLabels())
        .containsOnly(entry("replica-set", "Yay"));
  }
  @Test
  void podTemplateSpecBuilder() {
    // Given
    final PodTemplateSpecBuilder pb = new PodTemplateSpecBuilder();
    // When
    MetadataVisitor.podTemplateSpec(resourceConfig).visit(pb);
    // Then
    assertThat(pb.build().getMetadata().getAnnotations())
        .containsOnly(entry("pod", "Yay"));
    assertThat(pb.build().getMetadata().getLabels())
        .containsOnly(entry("pod", "Yay"));
  }

  @Test
  void serviceBuilder() {
    // Given
    final ServiceBuilder sb = new ServiceBuilder();
    // When
    MetadataVisitor.service(resourceConfig).visit(sb);
    // Then
    assertThat(sb.build().getMetadata().getAnnotations())
        .containsOnly(entry("service", "Yay"));
    assertThat(sb.build().getMetadata().getLabels())
        .containsOnly(entry("service", "Yay"));
  }

  @Test
  void serviceBuilder_preservesOriginalValues() {
    // Given
    final ServiceBuilder sb = new ServiceBuilder();
    sb.editOrNewMetadata().addToAnnotations("service", "to-be-preserved").endMetadata();
    // When
    MetadataVisitor.service(resourceConfig).visit(sb);
    // Then
    assertThat(sb.build().getMetadata().getAnnotations())
        .containsOnly(entry("service", "to-be-preserved"));
    assertThat(sb.build().getMetadata().getLabels())
        .containsOnly(entry("service", "Yay"));
  }

  @Test
  void daemonSet() {
    // Given
    final DaemonSetBuilder ds = new DaemonSetBuilder();
    // When
    MetadataVisitor.daemonSet(resourceConfig).visit(ds);
    // Then
    assertThat(ds.build().getMetadata().getAnnotations())
        .containsOnly(entry("this-is-all", "1"));
    assertThat(ds.build().getMetadata().getLabels())
        .containsOnly(entry("this-is-all", "1"), entry("super-label", "S"));
  }

  @Test
  void statefulSet() {
    // Given
    final StatefulSetBuilder ss = new StatefulSetBuilder();
    // When
    MetadataVisitor.statefulSet(resourceConfig).visit(ss);
    // Then
    assertThat(ss.build().getMetadata().getAnnotations())
        .containsOnly(entry("this-is-all", "1"));
    assertThat(ss.build().getMetadata().getLabels())
        .containsOnly(entry("this-is-all", "1"), entry("super-label", "S"));
  }

  @Test
  void imageStream() {
    // Given
    final ImageStreamBuilder is = new ImageStreamBuilder();
    // When
    MetadataVisitor.imageStream(resourceConfig).visit(is);
    // Then
    assertThat(is.build().getMetadata().getAnnotations())
        .containsOnly(entry("this-is-all", "1"));
    assertThat(is.build().getMetadata().getLabels())
        .containsOnly(entry("this-is-all", "1"), entry("super-label", "S"));
  }

  @Test
  void buildConfig() {
    // Given
    final BuildConfigBuilder bc = new BuildConfigBuilder();
    // When
    MetadataVisitor.buildConfig(resourceConfig).visit(bc);
    // Then
    assertThat(bc.build().getMetadata().getAnnotations())
        .containsOnly(entry("this-is-all", "1"));
    assertThat(bc.build().getMetadata().getLabels())
        .containsOnly(entry("this-is-all", "1"), entry("super-label", "S"));
  }

  @Test
  void build() {
    // Given
    final BuildBuilder b = new BuildBuilder();
    // When
    MetadataVisitor.build(resourceConfig).visit(b);
    // Then
    assertThat(b.build().getMetadata().getAnnotations())
        .containsOnly(entry("this-is-all", "1"));
    assertThat(b.build().getMetadata().getLabels())
        .containsOnly(entry("this-is-all", "1"), entry("super-label", "S"));
  }

  @Test
  void extensionsIngress() {
    // Given
    final IngressBuilder ib = new IngressBuilder();
    // When
    MetadataVisitor.extensionsIngress(resourceConfig).visit(ib);
    // Then
    assertThat(ib.build().getMetadata().getAnnotations()).containsOnly( entry("ingress", "Yay"));
    assertThat(ib.build().getMetadata().getLabels()).containsOnly(entry("ingress", "Yay"));
  }

  @Test
  void ingressV1beta1() {
    // Given
    final io.fabric8.kubernetes.api.model.networking.v1beta1.IngressBuilder ib =
        new io.fabric8.kubernetes.api.model.networking.v1beta1.IngressBuilder();
    // When
    MetadataVisitor.ingressV1beta1(resourceConfig).visit(ib);
    // Then
    assertThat(ib.build().getMetadata().getAnnotations()).containsOnly( entry("ingress", "Yay"));
    assertThat(ib.build().getMetadata().getLabels()).containsOnly(entry("ingress", "Yay"));
  }

  @Test
  void ingressV1() {
    // Given
    final io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder ib =
        new io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder();
    // When
    MetadataVisitor.ingressV1(resourceConfig).visit(ib);
    // Then
    assertThat(ib.build().getMetadata().getAnnotations()).containsOnly( entry("ingress", "Yay"));
    assertThat(ib.build().getMetadata().getLabels()).containsOnly(entry("ingress", "Yay"));
  }

  @Test
  void serviceAccount() {
    // Given
    final ServiceAccountBuilder sa = new ServiceAccountBuilder();
    // When
    MetadataVisitor.serviceAccount(resourceConfig).visit(sa);
    // Then
    assertThat(sa.build().getMetadata().getAnnotations()).containsOnly( entry("service-account", "Yay"));
    assertThat(sa.build().getMetadata().getLabels()).containsOnly(entry("service-account", "Yay"));
  }

  @Test
  void route() {
    // Given
    final RouteBuilder route = new RouteBuilder();
    // When
    MetadataVisitor.route(resourceConfig).visit(route);
    // Then
    assertThat(route.build().getMetadata().getAnnotations()).containsOnly( entry("route", "Yay"));
    assertThat(route.build().getMetadata().getLabels()).containsOnly(entry("route", "Yay"));
  }

  @Test
  void metadataVisit_whenMultilineAnnotationProvided_shouldAddTrailingNewline() {
    // Given
    Properties allProps = new Properties();
    final ObjectMetaBuilder db = new ObjectMetaBuilder();
    allProps.put("multiline/config", String.format("proxyMetadata:%n ISTIO_META_DNS_CAPTURE: \"false\"%nholdUntilProxyStarts: true"));
    ResourceConfig rc = ResourceConfig.builder()
        .annotations(MetaDataConfig.builder()
            .all(allProps)
            .build())
        .build();

    // When
    MetadataVisitor.metadata(rc).visit(db);

    // Then
    assertThat(db.build().getAnnotations())
        .containsOnly(entry("multiline/config", String.format("proxyMetadata:%n ISTIO_META_DNS_CAPTURE: \"false\"%nholdUntilProxyStarts: true%n")));
  }
}