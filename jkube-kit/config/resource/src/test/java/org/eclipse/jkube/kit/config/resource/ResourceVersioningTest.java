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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceVersioningTest {

  private ResourceVersioning resourceVersioning;
  @BeforeEach
  void setUp() {
    resourceVersioning = ResourceVersioning.builder()
      .withCoreVersion("core")
      .withExtensionsVersion("extensions")
      .withAppsVersion("apps")
      .withNetworkingVersion("networking")
      .withCronJobVersion("cronjob")
      .withJobVersion("job")
      .withOpenshiftV1version("openshift")
      .withRbacVersion("rbac")
      .withApiExtensionsVersion("api-extensions")
      .build();
  }
  @ParameterizedTest(name = "{index}: With Kind=''{0}'' should return ''{1}''")
  @DisplayName("getForKind")
  @CsvSource({
    "Ingress,extensions",
    "StatefulSet,apps",
    "Deployment,apps",
    "NetworkPolicy,networking",
    "Job,job",
    "DeploymentConfig,openshift",
    "CronJob,cronjob",
    "CustomResourceDefinition,api-extensions",
    "ClusterRole,rbac",
    "ClusterRoleBinding,rbac",
    "Role,rbac",
    "RoleBinding,rbac",
    "UnknownKind,core"
  })
  void getForKind(String kind, String apiVersion) {
    assertThat(resourceVersioning.getForKind(kind)).isEqualTo(apiVersion);
  }
}
