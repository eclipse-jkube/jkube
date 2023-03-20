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
package org.eclipse.jkube.kit.config.resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author roland
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class ResourceConfig {

  /**
   * @deprecated Use nested controller configuration instead
   */
  @Deprecated
  private Map<String, String> env;
  private MetaDataConfig labels;
  private MetaDataConfig annotations;
  /**
   * @deprecated Use nested controller configuration instead
   */
  @Deprecated
  @Singular
  private List<VolumeConfig> volumes;
  @Singular
  private List<SecretConfig> secrets;
  /**
   * @deprecated Use nested controller configuration instead
   */
  @Deprecated
  private String controllerName;
  @Singular
  private List<ServiceConfig> services;
  @Singular
  private List<String> remotes;
  private ConfigMap configMap;
  /**
   * @deprecated Use nested controller configuration instead
   */
  @Deprecated
  private ProbeConfig liveness;
  /**
   * @deprecated Use nested controller configuration instead
   */
  @Deprecated
  private ProbeConfig readiness;
  /**
   * @deprecated Use nested controller configuration instead
   */
  @Deprecated
  private ProbeConfig startup;
  private MetricsConfig metrics;

  /**
   * Run container in privileged mode.
   * @deprecated Use nested controller configuration instead
   */
  @Deprecated
  private boolean containerPrivileged;

  /**
   * How images should be pulled (maps to ImagePullPolicy).
   * @deprecated Use nested controller configuration instead
   */
  @Deprecated
  private String imagePullPolicy;

  /**
   * Number of replicas to create.
   * @deprecated Use nested controller configuration instead
   */
  @Deprecated
  private Integer replicas;
  private String namespace;
  private String serviceAccount;
  @Singular
  private List<String> customResourceDefinitions;
  @Singular
  private List<ServiceAccountConfig> serviceAccounts;
  private RequestsLimitsConfig openshiftBuildConfig;
  private Boolean createExternalUrls;
  private IngressConfig ingress;
  private String routeDomain;
  /**
   * @deprecated Use nested controller configuration instead
   */
  @Deprecated
  private String restartPolicy;
  private ControllerResourceConfig controller;
  private boolean useLegacyJKubePrefix;

  public final ControllerResourceConfig getController() {
    if (controller != null && isAnyControllerLegacyConfigFieldSet()) {
      throw new IllegalArgumentException("Can't use both controller and resource level controller configuration fields." +
        " Please migrate to controller configuration");
    }
    return controller == null ? controllerResourceConfigFromLegacyFields() : controller;
  }

  public static ResourceConfigBuilder toBuilder(ResourceConfig original) {
    return Optional.ofNullable(original).orElse(new ResourceConfig()).toBuilder();
  }

  private ControllerResourceConfig controllerResourceConfigFromLegacyFields() {
    ControllerResourceConfig.ControllerResourceConfigBuilder builder = ControllerResourceConfig.builder();
    if (env != null && !env.isEmpty()) {
      builder.env(env);
    }
    if (volumes != null) {
      builder.volumes(volumes);
    }
    if (StringUtils.isNotBlank(controllerName)) {
      builder.controllerName(controllerName);
    }
    if (liveness != null) {
      builder.liveness(liveness);
    }
    if (readiness != null) {
      builder.readiness(readiness);
    }
    if (startup != null) {
      builder.startup(startup);
    }
    if (imagePullPolicy != null) {
      builder.imagePullPolicy(imagePullPolicy);
    }
    if (replicas != null) {
      builder.replicas(replicas);
    }
    if (StringUtils.isNotBlank(restartPolicy)) {
      builder.restartPolicy(restartPolicy);
    }
    builder.containerPrivileged(containerPrivileged);
    return builder.build();
  }

  public boolean isAnyControllerLegacyConfigFieldSet() {
    return (env != null && !env.isEmpty()) ||
        (volumes != null && !volumes.isEmpty()) ||
        (controllerName != null) ||
        (liveness != null) ||
        (readiness != null) ||
        (startup != null) ||
        (imagePullPolicy != null) ||
        (replicas != null) ||
        (restartPolicy != null) ||
        (containerPrivileged);
  }

    // ===============================
    // TODO:
    // jkube.extended.environment.metadata
    // jkube.envProperties
    // jkube.combineDependencies
    // jkube.combineJson.target
    // jkube.combineJson.project

    // jkube.container.name	 --> alias name ?
    // jkube.replicationController.name

    // jkube.iconRef
    // jkube.iconUrl
    // jkube.iconUrlPrefix
    // jkube.iconUrlPrefix

    // jkube.imagePullPolicySnapshot

    // jkube.includeAllEnvironmentVariables
    // jkube.includeNamespaceEnvVar

    // jkube.namespaceEnvVar

    // jkube.provider
}

