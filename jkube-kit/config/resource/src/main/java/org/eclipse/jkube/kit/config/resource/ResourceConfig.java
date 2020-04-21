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

import io.fabric8.kubernetes.api.model.extensions.IngressRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;

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

  private Map<String, String> env;
  private MetaDataConfig labels;
  private MetaDataConfig annotations;
  @Singular
  private List<VolumeConfig> volumes;
  @Singular
  private List<SecretConfig> secrets;
  private String controllerName;
  @Singular
  private List<ServiceConfig> services;
  @Singular
  private List<String> remotes;
  private ConfigMap configMap;
  private ProbeConfig liveness;
  private ProbeConfig readiness;
  private MetricsConfig metrics;

  /**
   * Run container in privileged mode.
   */
  private boolean containerPrivileged;

  /**
   * How images should be pulled (maps to ImagePullPolicy).
   */
  private String imagePullPolicy;

  /**
   * Mapping of port to names.
   */
  @Singular
  private Map<String, Integer> ports;

  /**
   * Number of replicas to create.
   */
  private int replicas = 1;
  private String namespace;
  private String serviceAccount;
  @Singular
  private List<String> customResourceDefinitions;
  @Singular
  private List<ServiceAccountConfig> serviceAccounts;
  @Singular
  private List<IngressRule> ingressRules;
  private OpenshiftBuildConfig openshiftBuildConfig;

  public static ResourceConfigBuilder toBuilder(ResourceConfig original) {
    return Optional.ofNullable(original).orElse(new ResourceConfig()).toBuilder();
  }

    // TODO: SCC

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

