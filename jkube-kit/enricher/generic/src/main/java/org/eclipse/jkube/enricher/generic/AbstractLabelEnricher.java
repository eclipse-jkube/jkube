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
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerBuilder;
import io.fabric8.kubernetes.api.model.ReplicationControllerSpec;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.DaemonSetBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSetSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentConfigSpec;
import org.eclipse.jkube.kit.common.util.MapUtil;
import org.eclipse.jkube.kit.common.util.PropertiesUtil;
import org.eclipse.jkube.kit.config.resource.MetaDataConfig;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.EnricherContext;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.eclipse.jkube.kit.common.util.PropertiesUtil.toMap;

public abstract class AbstractLabelEnricher extends BaseEnricher {
  protected AbstractLabelEnricher(EnricherContext enricherContext, String name) {
    super(enricherContext, name);
  }

  abstract Map<String, String> createLabels(boolean withoutVersion, Map<String, String> labelsViaResourceConfig);

  @Override
  @SuppressWarnings("unchecked")
  public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
    builder.accept(new TypedVisitor<ServiceBuilder>() {
      @Override
      public void visit(ServiceBuilder serviceBuilder) {
        Map<String, String> selectors = new HashMap<>();
        Map<String, String> labelsConfiguredViaResourceConfig = MapUtil.mergeMaps(
            toMap(getResourceConfigLabels().getService()),
            toMap(getResourceConfigLabels().getAll()));
        if(serviceBuilder.buildSpec() != null && serviceBuilder.buildSpec().getSelector() != null) {
          selectors.putAll(serviceBuilder.buildSpec().getSelector());
        }
        MapUtil.mergeIfAbsent(selectors, createLabels(true, labelsConfiguredViaResourceConfig));
        serviceBuilder.editOrNewSpec().addToSelector(selectors).endSpec();
      }
    });

    builder.accept(new TypedVisitor<DeploymentBuilder>() {
      @Override
      public void visit(DeploymentBuilder builder) {
        final Map<String, String> selectors = mergedSelectors(Optional.ofNullable(builder.buildSpec())
            .map(DeploymentSpec::getSelector)
            .map(LabelSelector::getMatchLabels)
            .orElse(new HashMap<>()), Arrays.asList(
                getResourceConfigLabels().getDeployment(),
                getResourceConfigLabels().getPod(),
                getResourceConfigLabels().getAll()));
        builder.editOrNewSpec().editOrNewSelector().withMatchLabels(selectors).endSelector().endSpec();
      }
    });

    builder.accept(new TypedVisitor<DeploymentConfigBuilder>() {
      @Override
      public void visit(DeploymentConfigBuilder builder) {
        final Map<String, String> selectors = mergedSelectors(Optional.ofNullable(builder.buildSpec())
            .map(DeploymentConfigSpec::getSelector)
            .orElse(new HashMap<>()), Arrays.asList(
                getResourceConfigLabels().getPod(),
                getResourceConfigLabels().getAll()));
        builder.editOrNewSpec().addToSelector(selectors).endSpec();
      }
    });

    builder.accept(new TypedVisitor<DaemonSetBuilder>() {
      @Override
      public void visit(DaemonSetBuilder builder) {
        Map<String, String> selectors = new HashMap<>();
        Map<String, String> labelsFromResourceConfig = MapUtil.mergeMaps(
            toMap(getResourceConfigLabels().getPod()),
            toMap(getResourceConfigLabels().getAll()));
        if(builder.buildSpec() != null && builder.buildSpec().getSelector() != null && builder.buildSpec().getSelector().getMatchLabels() != null) {
          selectors.putAll(builder.buildSpec().getSelector().getMatchLabels());
        }
        MapUtil.mergeIfAbsent(selectors, createLabels(false, labelsFromResourceConfig));
        builder.editOrNewSpec().editOrNewSelector().withMatchLabels(selectors).endSelector().endSpec();
      }
    });

    builder.accept(new TypedVisitor<ReplicationControllerBuilder>() {
      @Override
      public void visit(ReplicationControllerBuilder builder) {
        final Map<String, String> selectors = mergedSelectors(Optional.ofNullable(builder.buildSpec())
            .map(ReplicationControllerSpec::getSelector)
            .orElse(new HashMap<>()), Arrays.asList(
                getResourceConfigLabels().getPod(),
                getResourceConfigLabels().getAll()));
        builder.editOrNewSpec().addToSelector(selectors).endSpec();
      }
    });

    builder.accept(new TypedVisitor<ReplicaSetBuilder>() {
      @Override
      public void visit(ReplicaSetBuilder builder) {
        final Map<String, String> selectors = mergedSelectors(Optional.ofNullable(builder.buildSpec())
            .map(ReplicaSetSpec::getSelector)
            .map(LabelSelector::getMatchLabels)
            .orElse(new HashMap<>()), Arrays.asList(
                getResourceConfigLabels().getReplicaSet(),
                getResourceConfigLabels().getPod(),
                getResourceConfigLabels().getAll()));
        builder.editOrNewSpec().editOrNewSelector().withMatchLabels(selectors).endSelector().endSpec();
      }
    });

    builder.accept(new TypedVisitor<StatefulSetBuilder>() {
      @Override
      public void visit(StatefulSetBuilder builder) {
        Map<String, String> selectors = new HashMap<>();
        Map<String, String> labelsFromResourceConfig = MapUtil.mergeMaps(
            toMap(getResourceConfigLabels().getPod()),
            toMap(getResourceConfigLabels().getAll()));
        if(builder.buildSpec() != null && builder.buildSpec().getSelector() != null && builder.buildSpec().getSelector().getMatchLabels() != null) {
          selectors.putAll(builder.buildSpec().getSelector().getMatchLabels());
        }
        MapUtil.mergeIfAbsent(selectors, createLabels(false, labelsFromResourceConfig));
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
        Map<String, String> labels = Optional.ofNullable(element.getLabels()).orElse(new HashMap<>());
        MapUtil.mergeIfAbsent(labels, createLabels(false, Collections.emptyMap()));
        element.withLabels(labels);
      }
    });
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> mergedSelectors(Map<String, String> originalSelectors, List<Properties> labelPropertyList) {
    Map<String, String> labelsFromResourceConfig = MapUtil.mergeMaps(labelPropertyList.stream()
        .map(PropertiesUtil::toMap)
        .toArray(Map[]::new));
    MapUtil.mergeIfAbsent(originalSelectors, createLabels(true, labelsFromResourceConfig));
    return originalSelectors;
  }

  protected MetaDataConfig getResourceConfigLabels() {
    return Optional.ofNullable(getConfiguration())
        .map(Configuration::getResource)
        .map(ResourceConfig::getLabels)
        .orElse(MetaDataConfig.builder().build());
  }
}
