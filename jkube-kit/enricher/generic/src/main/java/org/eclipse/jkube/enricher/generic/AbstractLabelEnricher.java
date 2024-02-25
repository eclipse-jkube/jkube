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

import io.fabric8.kubernetes.api.builder.TypedVisitor;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.apps.DaemonSetBuilder;
import io.fabric8.kubernetes.api.model.apps.DaemonSetSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigSpec;
import org.eclipse.jkube.kit.common.util.MapUtil;
import org.eclipse.jkube.kit.config.resource.MetaDataConfig;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.EnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public abstract class AbstractLabelEnricher extends BaseEnricher {
  protected AbstractLabelEnricher(EnricherContext enricherContext, String name) {
    super(enricherContext, name);
  }

  abstract Map<String, String> createLabels(boolean includeVersion, Map<String, String> labelsViaResourceConfig);

  @Override
  public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
    builder.accept(new TypedVisitor<ServiceBuilder>() {
      @Override
      public void visit(ServiceBuilder serviceBuilder) {
        final Map<String, String> selectors = processSelectors(
          Optional.ofNullable(serviceBuilder.buildSpec())
            .map(ServiceSpec::getSelector)
            .orElse(new HashMap<>()),
          false,
          getResourceConfigLabels().getService(),getResourceConfigLabels().getAll());
        serviceBuilder.editOrNewSpec().addToSelector(selectors).endSpec();
      }
    });

    builder.accept(new TypedVisitor<DeploymentBuilder>() {
      @Override
      public void visit(DeploymentBuilder builder) {
        final Map<String, String> selectors = processSelectors(
          Optional.ofNullable(builder.buildSpec())
            .map(DeploymentSpec::getSelector)
            .map(LabelSelector::getMatchLabels)
            .orElse(new HashMap<>()),
          false,
          getResourceConfigLabels().getDeployment(),
          getResourceConfigLabels().getPod(),
          getResourceConfigLabels().getAll());
        builder.editOrNewSpec().editOrNewSelector().withMatchLabels(selectors).endSelector().endSpec();
      }
    });

    builder.accept(new TypedVisitor<DeploymentConfigBuilder>() {
      @Override
      public void visit(DeploymentConfigBuilder builder) {
        final Map<String, String> selectors = processSelectors(
          Optional.ofNullable(builder.buildSpec())
            .map(DeploymentConfigSpec::getSelector)
            .orElse(new HashMap<>()),
          false,
          getResourceConfigLabels().getPod(), getResourceConfigLabels().getAll());
        builder.editOrNewSpec().addToSelector(selectors).endSpec();
      }
    });

    builder.accept(new TypedVisitor<DaemonSetBuilder>() {
      @Override
      public void visit(DaemonSetBuilder builder) {
        final Map<String, String> selectors = processSelectors(
          Optional.ofNullable(builder.buildSpec())
            .map(DaemonSetSpec::getSelector)
            .map(LabelSelector::getMatchLabels)
            .orElse(new HashMap<>()),
          true, getResourceConfigLabels().getAll());
        builder.editOrNewSpec().editOrNewSelector().withMatchLabels(selectors).endSelector().endSpec();
      }
    });

    builder.accept(new TypedVisitor<ReplicationControllerBuilder>() {
      @Override
      public void visit(ReplicationControllerBuilder builder) {
        final Map<String, String> selectors = processSelectors(
          Optional.ofNullable(builder.buildSpec())
            .map(ReplicationControllerSpec::getSelector)
            .orElse(new HashMap<>()),
          false,
          getResourceConfigLabels().getPod(), getResourceConfigLabels().getAll());
        builder.editOrNewSpec().addToSelector(selectors).endSpec();
      }
    });

    builder.accept(new TypedVisitor<ReplicaSetBuilder>() {
      @Override
      public void visit(ReplicaSetBuilder builder) {
        final Map<String, String> selectors = processSelectors(
          Optional.ofNullable(builder.buildSpec())
            .map(ReplicaSetSpec::getSelector)
            .map(LabelSelector::getMatchLabels)
            .orElse(new HashMap<>()),
          false,
          getResourceConfigLabels().getReplicaSet(),
          getResourceConfigLabels().getPod(),
          getResourceConfigLabels().getAll());
        builder.editOrNewSpec().editOrNewSelector().withMatchLabels(selectors).endSelector().endSpec();
      }
    });

    builder.accept(new TypedVisitor<StatefulSetBuilder>() {
      @Override
      public void visit(StatefulSetBuilder builder) {
        final Map<String, String> selectors = processSelectors(
          Optional.ofNullable(builder.buildSpec())
            .map(StatefulSetSpec::getSelector)
            .map(LabelSelector::getMatchLabels)
            .orElse(new HashMap<>()),
          true,
          getResourceConfigLabels().getPod(), getResourceConfigLabels().getAll());
        builder.editOrNewSpec().editOrNewSelector().withMatchLabels(selectors).endSelector().endSpec();
      }
    });

  }

  @Override
  public void enrich(PlatformMode platformMode, KubernetesListBuilder builder) {
    // Add to all objects in the builder
    builder.accept(new TypedVisitor<ObjectMetaBuilder>() {
      @Override
      public void visit(ObjectMetaBuilder element) {
        final Map<String, String> labels = processSelectors(
          Optional.ofNullable(element.build())
            .map(ObjectMeta::getLabels)
            .orElse(new HashMap<>()),
          true);
        element.withLabels(labels);
      }
    });
  }

  private Map<String, String> processSelectors(Map<String, String> selectors, boolean includeVersion, Properties... labelPropertyList) {
    MapUtil.mergeIfAbsent(selectors, createLabels(includeVersion, MapUtil.mergeMaps(labelPropertyList)));
    return selectors;
  }

  protected MetaDataConfig getResourceConfigLabels() {
    return Optional.ofNullable(getConfiguration())
        .map(Configuration::getResource)
        .map(ResourceConfig::getLabels)
        .orElse(MetaDataConfig.builder().build());
  }
}
