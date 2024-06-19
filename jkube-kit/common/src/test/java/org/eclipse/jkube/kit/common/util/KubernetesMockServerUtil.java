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
package org.eclipse.jkube.kit.common.util;

import io.fabric8.kubernetes.api.model.APIGroupBuilder;
import io.fabric8.kubernetes.api.model.APIGroupList;
import io.fabric8.kubernetes.api.model.APIGroupListBuilder;
import io.fabric8.kubernetes.api.model.APIResourceBuilder;
import io.fabric8.kubernetes.api.model.APIResourceList;
import io.fabric8.kubernetes.api.model.APIResourceListBuilder;
import io.fabric8.kubernetes.client.VersionInfo;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;

public class KubernetesMockServerUtil {

  private KubernetesMockServerUtil() { }

  // TODO: Remove after https://github.com/fabric8io/kubernetes-client/issues/6062 is fixed
  public static void prepareMockWebServerExpectationsForAggregatedDiscoveryEndpoints(KubernetesMockServer server) {
    server.expect().get()
      .withPath("/version?timeout=32s")
      .andReturn(200, new VersionInfo.Builder()
        .withMajor("1")
        .withMinor("30")
        .build())
      .always();
    server.expect().get().withPath("/api?timeout=32s")
      .andReturn(200, String.format("{\"kind\":\"APIVersions\",\"versions\":[\"v1\"],\"serverAddressByClientCIDRs\":[{\"clientCIDR\":\"0.0.0.0/0\",\"serverAddress\":\"%s:%d\"}]}", server.getHostName(), server.getPort()))
      .always();
    server.expect().get().withPath("/apis?timeout=32s")
      .andReturn(200, createNewAPIGroupList())
      .always();
    server.expect().get().withPath("/api/v1?timeout=32s")
      .andReturn(200, createNewAPIResourceList())
      .always();
    server.expect().get().withPath("/apis/apps/v1?timeout=32s")
      .andReturn(200, createNewAPIResourceList())
      .always();
  }

  private static APIResourceList createNewAPIResourceList() {
    APIResourceListBuilder apiResourceListBuilder = new APIResourceListBuilder();
    apiResourceListBuilder.addToResources(new APIResourceBuilder()
      .withNamespaced()
      .withKind("Service")
      .withName("services")
      .withSingularName("service")
      .build());
    apiResourceListBuilder.addToResources(new APIResourceBuilder()
      .withName("deployments")
      .withKind("Deployment")
      .withSingularName("deployment")
      .withNamespaced()
      .build());
    apiResourceListBuilder.addToResources(new APIResourceBuilder()
      .withName("serviceaccounts")
      .withKind("ServiceAccount")
      .withSingularName("serviceaccount")
      .withNamespaced()
      .build());
    apiResourceListBuilder.addToResources(new APIResourceBuilder()
      .withName("secrets")
      .withKind("Secret")
      .withSingularName("secret")
      .withNamespaced()
      .build());
    return apiResourceListBuilder.build();
  }

  private static APIGroupList createNewAPIGroupList() {
    return new APIGroupListBuilder()
      .addToGroups(new APIGroupBuilder()
        .withName("apps")
        .addNewVersion()
        .withGroupVersion("apps/v1")
        .withVersion("v1")
        .endVersion()
        .withNewPreferredVersion()
        .withGroupVersion("apps/v1")
        .withVersion("v1")
        .endPreferredVersion()
        .build())
      .build();
  }
}
