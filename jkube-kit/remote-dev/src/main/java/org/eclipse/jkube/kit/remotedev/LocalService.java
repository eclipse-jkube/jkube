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
package org.eclipse.jkube.kit.remotedev;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

import static org.eclipse.jkube.kit.remotedev.RemoteDevelopmentService.REMOTE_DEVELOPMENT_APP;
import static org.eclipse.jkube.kit.remotedev.RemoteDevelopmentService.REMOTE_DEVELOPMENT_GROUP;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class LocalService {

  private String serviceName;
  private String type;
  /**
   * Local port where the service is exposed
   */
  private int port;

  public Service toKubernetesService(UUID sessionID) {
    return new ServiceBuilder()
      .withNewMetadata()
      .withName(serviceName)
      .endMetadata()
      .withNewSpec()
      .withType(type)
      .addToSelector("app", REMOTE_DEVELOPMENT_APP)
      .addToSelector("group", REMOTE_DEVELOPMENT_GROUP)
      .addToSelector("jkube-id", sessionID.toString())
      .addNewPort()
      .withProtocol("TCP")
      .withPort(port)
      .withTargetPort(new IntOrString(port))
      .endPort()
      .endSpec()
      .build();
  }
}
