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
package org.eclipse.jkube.kit.resource.helm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class HelmRepository {

  private String name;
  private String url;
  private String username;
  private String password;
  private HelmRepoType type;

  public enum HelmRepoType {
    CHARTMUSEUM(HelmRepositoryConnectionUtils::getConnectionForUploadToChartMuseum),
    ARTIFACTORY(HelmRepositoryConnectionUtils::getConnectionForUploadToArtifactory),
    NEXUS(HelmRepositoryConnectionUtils::getConnectionForUploadToNexus);

    private final ConnectionCreator connectionCreator;

    HelmRepoType(ConnectionCreator connectionCreator) {
      this.connectionCreator = connectionCreator;
    }

    public HttpURLConnection createConnection(File file, HelmRepository repository) throws IOException {
      return connectionCreator.createConnectionForUploadToArtifactory(file, repository);
    }

    @FunctionalInterface
    protected interface ConnectionCreator {
      HttpURLConnection createConnectionForUploadToArtifactory(File file, HelmRepository repository) throws IOException;
    }
  }

  @Override
  public String toString() {
    return "[" + name + " / " + url + "]";
  }
}
