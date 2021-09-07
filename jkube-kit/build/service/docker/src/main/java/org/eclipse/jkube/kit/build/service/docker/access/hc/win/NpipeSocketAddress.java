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
package org.eclipse.jkube.kit.build.service.docker.access.hc.win;

import java.io.File;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

class NpipeSocketAddress extends java.net.SocketAddress {

  private static final long serialVersionUID = -201738636850828643L;

  private final String path;

  NpipeSocketAddress(File path) {
    this.path = path.getPath();
  }

  public String getPath() {
    return path;
  }

  @Override
  public String toString() {
    return "NpipeSocketAddress{path='" + path + "'}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (o == null || getClass() != o.getClass()) return false;

    NpipeSocketAddress that = (NpipeSocketAddress) o;

    return new EqualsBuilder()
        .append(path, that.path)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(path)
        .toHashCode();
  }
}
