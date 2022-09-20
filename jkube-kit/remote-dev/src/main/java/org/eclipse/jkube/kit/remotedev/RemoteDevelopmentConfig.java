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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import org.eclipse.jkube.kit.common.util.IoUtil;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class RemoteDevelopmentConfig {

  private final AtomicInteger sshPort = new AtomicInteger(-1);
  private String user;
  private String password;

  @Singular
  private List<RemotePort> remotePorts;

  @Singular
  private List<LocalService> localServices;

  public int getSshPort() {
    return sshPort.updateAndGet(v -> v == -1 ? IoUtil.getFreeRandomPort() : v);
  }
}
