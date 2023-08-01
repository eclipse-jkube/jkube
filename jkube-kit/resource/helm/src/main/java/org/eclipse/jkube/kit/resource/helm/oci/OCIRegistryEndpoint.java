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
package org.eclipse.jkube.kit.resource.helm.oci;


import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.resource.helm.Chart;
import org.eclipse.jkube.kit.resource.helm.HelmRepository;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class OCIRegistryEndpoint {

  private static final Map<String, String> PROTOCOL_MAPPER = new HashMap<>();
  static {
    PROTOCOL_MAPPER.put("oci", "http");
    PROTOCOL_MAPPER.put("ocis", "https");
  }

  private final URI baseUrl;
  private final URI apiV2Url;

  public OCIRegistryEndpoint(HelmRepository repository) {
    final URI repositoryUri = URI.create(StringUtils.removeEnd(repository.getUrl(), "/"));
    this.baseUrl = URI.create(PROTOCOL_MAPPER.getOrDefault(repositoryUri.getScheme(), repositoryUri.getScheme()) + "://" +
      repositoryUri.getHost() +
      (repositoryUri.getPort() > 0 ? ":" + repositoryUri.getPort() : ""));
    this.apiV2Url = URI.create(baseUrl + "/v2" + repositoryUri.getPath());
  }

  public String getBlobUrl(Chart chart, OCIManifestLayer blob) {
    return String.format("%s/%s/blobs/%s", apiV2Url, chart.getName(), blob.getDigest());
  }

  public String getBlobUploadInitUrl(Chart chart) {
    return String.format("%s/%s/blobs/uploads/", apiV2Url, chart.getName());
  }

  public String getManifestUrl(Chart chart) {
    return String.format("%s/%s/manifests/%s", apiV2Url, chart.getName(), chart.getVersion());
  }

  public URI getBaseUrl() {
    return baseUrl;
  }
}
