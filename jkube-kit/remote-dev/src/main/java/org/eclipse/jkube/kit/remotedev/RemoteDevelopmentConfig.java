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
package org.eclipse.jkube.kit.remotedev;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.Collections;
import java.util.List;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class RemoteDevelopmentConfig {

  private int socksPort;
  @Singular
  private List<RemoteService> remoteServices;

  @Singular
  private List<LocalService> localServices;

  public List<RemoteService> getRemoteServices() {
    return remoteServices == null ? Collections.emptyList() : remoteServices;
  }

  public List<LocalService> getLocalServices() {
    return localServices == null ? Collections.emptyList() : localServices;
  }
}
