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
package org.eclipse.jkube.kit.resource.helm.oci;


import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;

public class OCIRegistryEndpoint {
  private final String url;

  public OCIRegistryEndpoint(String baseUrl) {
    this.url = StringUtils.removeEnd(baseUrl, "/");
  }

  public String getBlobUrl(String chartName, String digest) throws MalformedURLException {
    return String.format("%s/%s/blobs/sha256:%s", getV2ApiUrl(), chartName, digest);
  }

  public String getBlobUploadInitUrl(String chartName) throws MalformedURLException {
    return String.format("%s/%s/blobs/uploads/", getV2ApiUrl(), chartName);
  }

  public String getManifestUrl(String chartName, String version) throws MalformedURLException {
    return String.format("%s/%s/manifests/%s", getV2ApiUrl(), chartName, version);
  }

  public String getV2ApiUrl() throws MalformedURLException {
    URL registryUrl = new URL(url);

    return String.format("%s/v2%s", getBaseUrl(), registryUrl.getPath());
  }

  public String getBaseUrl() throws MalformedURLException {
    URL registryUrl = new URL(url);

    String portString = "";
    if (registryUrl.getPort() > 0) {
      portString = String.format(":%d", registryUrl.getPort());
    }

    return String.format("%s://%s%s", registryUrl.getProtocol(), registryUrl.getHost(), portString);
  }
}
