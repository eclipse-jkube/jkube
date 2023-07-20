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
package org.eclipse.jkube.kit.config.resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author nicola
 */
@Builder(setterPrefix = "with")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class ResourceVersioning {

  private static final Map<String, Function<ResourceVersioning, String>> KIND_API_VERSION_MAPPINGS = new HashMap<>();
  static {
    KIND_API_VERSION_MAPPINGS.put("Ingress", ResourceVersioning::getExtensionsVersion);
    KIND_API_VERSION_MAPPINGS.put("StatefulSet", ResourceVersioning::getAppsVersion);
    KIND_API_VERSION_MAPPINGS.put("Deployment", ResourceVersioning::getAppsVersion);
    KIND_API_VERSION_MAPPINGS.put("NetworkPolicy", ResourceVersioning::getNetworkingVersion);
    KIND_API_VERSION_MAPPINGS.put("Job", ResourceVersioning::getJobVersion);
    KIND_API_VERSION_MAPPINGS.put("DeploymentConfig", ResourceVersioning::getOpenshiftV1version);
    KIND_API_VERSION_MAPPINGS.put("CronJob", ResourceVersioning::getCronJobVersion);
    KIND_API_VERSION_MAPPINGS.put("CustomResourceDefinition", ResourceVersioning::getApiExtensionsVersion);
    KIND_API_VERSION_MAPPINGS.put("ClusterRole", ResourceVersioning::getRbacVersion);
    KIND_API_VERSION_MAPPINGS.put("ClusterRoleBinding", ResourceVersioning::getRbacVersion);
    KIND_API_VERSION_MAPPINGS.put("Role", ResourceVersioning::getRbacVersion);
    KIND_API_VERSION_MAPPINGS.put("RoleBinding", ResourceVersioning::getRbacVersion);
  }

  private String coreVersion;
  private String extensionsVersion;
  private String appsVersion;
  private String networkingVersion;
  private String jobVersion;
  private String openshiftV1version;
  private String rbacVersion;
  private String cronJobVersion;
  private String apiExtensionsVersion;

  public String getForKind(String kind) {
    return KIND_API_VERSION_MAPPINGS.getOrDefault(kind, ResourceVersioning::getCoreVersion).apply(this);
  }
}

