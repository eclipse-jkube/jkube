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
package org.eclipse.jkube.kit.build.service.docker.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.Opt;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;


/**
 * @author roland
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class WaitConfiguration implements Serializable {

  // Default HTTP Method to use
  public static final String DEFAULT_HTTP_METHOD = "HEAD";

  // Default status codes
  public static final int DEFAULT_MIN_STATUS = 200;
  public static final int DEFAULT_MAX_STATUS = 399;

  public static final String DEFAULT_STATUS_RANGE = String.format("%d..%d", DEFAULT_MIN_STATUS, DEFAULT_MAX_STATUS);

  private Integer time;
  private HttpConfiguration http;
  private ExecConfiguration exec;
  private TcpConfiguration tcp;
  private Boolean healthy;
  private String log;
  private Integer shutdown;
  private Integer kill;
  private Integer exit;

  @SuppressWarnings("unused")
  @Builder
  public WaitConfiguration(
      Integer time,
      String url, String method, String status,
      String postStart, String preStop, Boolean breakOnError,
      TcpConfigMode tcpMode, String tcpHost, List<Integer> tcpPorts,
      Boolean healthy, String log, Integer shutdown, Integer kill, Integer exit) {

    this.time = time;
    if (url != null) {
      this.http = new HttpConfiguration(url, method, status);
    }
    if (postStart != null || preStop != null) {
      this.exec = new ExecConfiguration(postStart, preStop, Optional.ofNullable(breakOnError).orElse(false));
    }
    if (tcpPorts != null) {
      this.tcp = new TcpConfiguration(tcpMode, tcpHost, tcpPorts);
    }
    this.healthy = healthy;
    this.log = log;
    this.shutdown = shutdown;
    this.kill = kill;
    this.exit = exit;
  }

  public String getUrl() {
    return Optional.ofNullable(http).map(HttpConfiguration::getUrl).orElse(null);
  }

  public static class WaitConfigurationBuilder {
    public WaitConfigurationBuilder tcpModeString(String tcpModeString) {
      tcpMode = Optional.ofNullable(tcpModeString).map(String::toLowerCase).map(TcpConfigMode::valueOf).orElse(null);
      return this;
    }
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Getter
  @EqualsAndHashCode
  public static class ExecConfiguration implements Serializable {
    private String postStart;
    private String preStop;
    private boolean breakOnError;
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Getter
  @EqualsAndHashCode
  public static class HttpConfiguration implements Serializable {

    private static final long serialVersionUID = -4093004978420554981L;

    private String url;
    private String method = DEFAULT_HTTP_METHOD;
    private String status = DEFAULT_STATUS_RANGE;
    private boolean allowAllHosts;

    private HttpConfiguration(String url, String method, String status) {
      this(url, method, status, false);
    }
  }

  public enum TcpConfigMode {
    // Use mapped ports
    mapped,
    // Use ports directly on the container
    direct,
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Getter
  @EqualsAndHashCode
  public static class TcpConfiguration implements Serializable {

    private static final long serialVersionUID = 4809023624478231971L;

    private TcpConfigMode mode;
    private String host;
    private List<Integer> ports;
  }

}

