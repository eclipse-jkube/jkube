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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.BaseEnricher;
import org.eclipse.jkube.kit.enricher.api.EnricherContext;
import org.eclipse.jkube.kit.enricher.api.util.KubernetesResourceUtil;
import org.eclipse.jkube.kit.enricher.handler.ControllerHandler;

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

  private static final Map<String, Class<? extends HasMetadata>> CONTROLLER_TYPES = new HashMap<>();
  static {
    CONTROLLER_TYPES.put("STATEFULSET", StatefulSet.class);
    CONTROLLER_TYPES.put("DAEMONSET", DaemonSet.class);
    CONTROLLER_TYPES.put("REPLICASET", ReplicaSet.class);
    CONTROLLER_TYPES.put("REPLICATIONCONTROLLER", ReplicationController.class);
    CONTROLLER_TYPES.put("JOB", Job.class);
  }

  @AllArgsConstructor
  public enum Config implements Configs.Config {
    NAME("name", null),
    /**
     * @deprecated in favor of <code>jkube.imagePullPolicy</code> property
     */
    @Deprecated
    PULL_POLICY("pullPolicy", JKUBE_DEFAULT_IMAGE_PULL_POLICY),
    TYPE("type", null),
    REPLICA_COUNT("replicaCount", "1");

    @Getter
    protected String key;
    @Getter
    protected String defaultValue;
  }

  public DefaultControllerEnricher(EnricherContext buildContext) {
    super(buildContext, ENRICHER_NAME);
  }

  @Override
  public void create(PlatformMode platformMode, KubernetesListBuilder builder) {
    final String name = getConfig(Config.NAME,
        JKubeProjectUtil.createDefaultResourceName(getContext().getGav().getSanitizedArtifactId()));
    ResourceConfig providedResourceConfig = Optional.ofNullable(getConfiguration().getResource())
        .orElse(ResourceConfig.builder().build());
    ResourceConfig config = ResourceConfig.toBuilder(providedResourceConfig)
        .controllerName(getControllerName(providedResourceConfig, name))
        .imagePullPolicy(getImagePullPolicy(providedResourceConfig, Config.PULL_POLICY))
        .replicas(getReplicaCount(builder, providedResourceConfig, Configs.asInt(getConfig(Config.REPLICA_COUNT))))
        .restartPolicy(providedResourceConfig.getRestartPolicy())
        .build();

    final List<ImageConfiguration> images = getImages();

    // Check if at least a replica set is added. If not add a default one
    // At least one image must be present, otherwise the resulting config will be invalid
    if (!KubernetesResourceUtil.checkForKind(builder, POD_CONTROLLER_KINDS) && !images.isEmpty()) {
      final ControllerHandler<? extends HasMetadata> ch = getContext().getHandlerHub()
          .getHandlerFor(fromType(getConfig(Config.TYPE)));
      final HasMetadata resource = ch.get(config, images);
      log.info("Adding a default %s", resource.getKind());
      builder.addToItems(resource);
      setProcessingInstruction(FABRIC8_GENERATED_CONTAINERS,
          getContainersFromPodSpec(ch.getPodTemplateSpec(config, images)));
    }
  }

  private static List<String> getContainersFromPodSpec(PodTemplateSpec spec) {
    return spec.getSpec().getContainers().stream().map(Container::getName).collect(Collectors.toList());
  }

  private static Class<? extends HasMetadata> fromType(String type) {
    return CONTROLLER_TYPES.getOrDefault(
        Optional.ofNullable(type).orElse("").toUpperCase(Locale.ENGLISH),
        Deployment.class);
  }
}
