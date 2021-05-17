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

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.JKubeEnricherContext;
import org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil;
import org.eclipse.jkube.kit.enricher.handler.ControllerHandler;
import org.eclipse.jkube.kit.enricher.handler.HandlerHub;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static org.eclipse.jkube.enricher.generic.ControllerViaPluginConfigurationEnricher.POD_CONTROLLER_KINDS;

/**
 * Enrich with controller if not already present.
 *
 * By default the following objects will be added
 *
 * <ul>
 *     <li>Deployment</li>
 *     <li>StatefulSet</li>
 *     <li>DaemonSet</li>
 *     <li>ReplicaSet</li>
 *     <li>ReplicationController</li>
 *     <li>Job</li>
 * </ul>
 *
 * TODO: There is a certain overlap with the ImageEnricher with adding default images etc.. This must be resolved.
 *
 * @author roland
 */
public class DefaultControllerEnricher extends BaseEnricher {

  public static final String ENRICHER_NAME = "jkube-controller";

  private enum ControllerType {
    DEPLOYMENT(HandlerHub::getDeploymentHandler),
    STATEFUL_SET(HandlerHub::getStatefulSetHandler),
    DAEMON_SET(HandlerHub::getDaemonSetHandler),
    REPLICA_SET(HandlerHub::getReplicaSetHandler),
    REPLICATION_CONTROLLER(HandlerHub::getReplicationControllerHandler),
    JOB(HandlerHub::getJobHandler);

    private final Function<HandlerHub, ControllerHandler<?>> handler;

    ControllerType(Function<HandlerHub, ControllerHandler<?>> handler) {
      this.handler = handler;
    }

    private static ControllerType fromType(String type) {
      // There's no DEPLOYMENTCONFIG entry since it will be taken care of by DeploymentConfigEnricher
      // where the resulting Deployment is converted to a DeploymentConfig
      switch (Optional.ofNullable(type).orElse("").toUpperCase(Locale.ENGLISH)) {
        case "STATEFULSET":
          return STATEFUL_SET;
        case "DAEMONSET":
          return DAEMON_SET;
        case "REPLICASET":
          return REPLICA_SET;
        case "REPLICATIONCONTROLLER":
          return REPLICATION_CONTROLLER;
        case "JOB":
          return JOB;
        default:
          return DEPLOYMENT;
      }
    }
  }

  @AllArgsConstructor
  public enum Config implements Configs.Config {
    NAME("name", null),
    PULL_POLICY("pullPolicy", "IfNotPresent"),
    TYPE("type", null),
    REPLICA_COUNT("replicaCount", "1");

    @Getter
    protected String key;
    @Getter
    protected String defaultValue;
  }

  private final HandlerHub handlerHub;

  public DefaultControllerEnricher(JKubeEnricherContext buildContext) {
    super(buildContext, ENRICHER_NAME);
    handlerHub = new HandlerHub(getContext().getGav(), getContext().getProperties());
  }

  @Override
  public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
    final String name = getConfig(Config.NAME,
        JKubeProjectUtil.createDefaultResourceName(getContext().getGav().getSanitizedArtifactId()));
    ResourceConfig xmlResourceConfig = Optional.ofNullable(getConfiguration().getResource())
        .orElse(ResourceConfig.builder().build());
    ResourceConfig config = ResourceConfig.toBuilder(xmlResourceConfig)
        .controllerName(getControllerName(xmlResourceConfig, name))
        .imagePullPolicy(getImagePullPolicy(xmlResourceConfig, getConfig(Config.PULL_POLICY)))
        .replicas(getReplicaCount(builder, xmlResourceConfig, Configs.asInt(getConfig(Config.REPLICA_COUNT))))
        .build();

    final List<ImageConfiguration> images = getImages();

    // Check if at least a replica set is added. If not add a default one
    // At least one image must be present, otherwise the resulting config will be invalid
    if (!KubernetesResourceUtil.checkForKind(builder, POD_CONTROLLER_KINDS) && !images.isEmpty()) {
      final ControllerType ct = ControllerType.fromType(getConfig(Config.TYPE));
      final HasMetadata resource = ct.handler.apply(handlerHub).get(config, images);
      log.info("Adding a default %s", resource.getKind());
      builder.addToItems(resource);
      setProcessingInstruction(FABRIC8_GENERATED_CONTAINERS,
          getContainersFromPodSpec(ct.handler.apply(handlerHub).getPodTemplateSpec(config, images)));
    }
  }

  private static List<String> getContainersFromPodSpec(PodTemplateSpec spec) {
    return spec.getSpec().getContainers().stream().map(Container::getName).collect(Collectors.toList());
  }

}
