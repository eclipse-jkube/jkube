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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.List;
import java.util.Optional;

/**
 * @author roland
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode()
public class ServiceConfig {

  /**
   * Service name.
   */
  private String name;

  /**
   * Ports to expose
   */
  @Singular
  private List<Port> ports;

  /**
   * Whether this is a headless service.
   */
  private boolean headless;

  /**
   * If the expose label is added to the service.
   */
  private boolean expose;

  /**
   * Service type.
   */
  private String type;

  /**
   * Whether to normalize service port numbering.
   */
  private boolean normalizePort;

  @Builder(toBuilder = true)
  @AllArgsConstructor
  @NoArgsConstructor
  @Getter
  @EqualsAndHashCode()
  public static class Port {

    /**
     * Protocol to use. Can be either "tcp" or "udp".
     */
    private String protocol;

    /**
     * Container port to expose.
     */
    private int port;

    /**
     * Target port to expose.
     */
    private int targetPort;

    /**
     * Port to expose from the port.
     */
    private Integer nodePort;

    /**
     * Name of the port
     */
    private String name;

    public ServiceProtocol getProtocol() {
      return Optional.ofNullable(protocol).map(String::toUpperCase).map(ServiceProtocol::valueOf).orElse(null);
    }
  }
}
