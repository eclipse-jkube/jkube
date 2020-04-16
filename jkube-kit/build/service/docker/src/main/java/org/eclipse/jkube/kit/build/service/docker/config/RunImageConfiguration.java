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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.eclipse.jkube.kit.config.image.build.Arguments;

/**
 * @author roland
 */
@SuppressWarnings("JavaDoc")
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class RunImageConfiguration implements Serializable {

  private static final long serialVersionUID = 439009097742935171L;

  public static final RunImageConfiguration DEFAULT = new RunImageConfiguration();

  public boolean isDefault() {
    return this == RunImageConfiguration.DEFAULT;
  }

  /**
   * Environment variables to set when starting the container. key: variable name, value: env value.
   */
  private Map<String, String> env;
  private Map<String,String> labels;
  /**
   * Path to a property file holding environment variables.
   */
  private String envPropertyFile;
  /**
   * Command to execute in container.
   */
  private Arguments cmd;
  /**
   * Container domain name.
   */
  private String domainname;
  private List<String> dependsOn;
  /**
   * Container entry point.
   */
  private Arguments entrypoint;
  /**
   * Container hostname.
   */
  private String hostname;
  /**
   * Container user.
   */
  private String user;
  /**
   * Working directory.
   */
  private String workingDir;
  /**
   * Size of /dev/shm in bytes.
   */
  private Long shmSize;
  /**
   * Memory in bytes.
   */
  private Long memory;
  /**
   * Total memory (swap + ram) in bytes, -1 to disable.
   */
  private Long memorySwap;
  /**
   * Path to a file where the dynamically mapped properties are written to.
   */
  private String portPropertyFile;
  /**
   * For simple network setups. For complex stuff use "network".
   */
  private String net;
  private NetworkConfig network;
  private List<String> dns;
  private List<String> dnsSearch;
  private List<String> capAdd;
  private List<String> capDrop;
  private List<String> securityOpts;
  private Boolean privileged;
  private List<String> extraHosts;
  private Long cpuShares;
  private Long cpus;
  private String cpuSet;
  /**
   * Port mapping. Can contain symbolic names in which case dynamic ports are used.
   *
   *
   */
  private List<String> ports;
  /**
   * @deprecated use {@link #getContainerNamePattern} instead
   * @return NamingStrategy naming strategy
   */
  @Deprecated
  private NamingStrategy namingStrategy;
  /**
   * A pattern to define the naming of the container where:
   *
   * <ul>
   *   <li>%a for the "alias" mode</li>
   *   <li>%n for the image name</li>
   *   <li>%t for a timestamp</li>
   *   <li>%i for an increasing index of container names</li>
   * </ul>
   *
   */
  private String containerNamePattern;
  /**
   * Property key part used to expose the container ip when running.
   */
  private String exposedPropertyKey;
  /**
   * Mount volumes from the given image's started containers.
   */
  private RunVolumeConfiguration volumes;
  /**
   * Links to other container started.
   */
  private List<String> links;
  /**
   * Configuration for how to wait during startup of the container.
   */
  private WaitConfiguration wait;
  /**
   * Mountpath for tmps.
   */
  private List<String> tmpfs;
  private LogConfiguration log;
  private RestartPolicy restartPolicy;
  private List<UlimitConfig> ulimits;
  private Boolean skip;
  /**
   * Policy for pulling the image to start
   */
  private String imagePullPolicy;
  /**
   * Mount the container's root filesystem as read only.
   */
  private Boolean readOnly;
  /**
   * Automatically remove the container when it exists.
   */
  private Boolean autoRemove;

  public String initAndValidate() {
    if (entrypoint != null) {
      entrypoint.validate();
    }
    if (cmd != null) {
      cmd.validate();
    }

    /**
     * Custom networks are available since API 1.21 (Docker 1.9).
     */
    NetworkConfig config = getNetworkingConfig();
    if (config != null && config.isCustomNetwork()) {
      return "1.21";
    }

    return null;
  }

  @Nonnull
  public List<String> getDependsOn() {
    return EnvUtil.splitAtCommasAndTrim(dependsOn);
  }

  @Nonnull
  public List<String> getPorts() {
    return EnvUtil.removeEmptyEntries(ports);
  }

  @Deprecated
  public String getNetRaw() {
    return net;
  }

  public NetworkConfig getNetworkingConfig() {
    if (network != null) {
      return network;
    } else if (net != null) {
      return NetworkConfig.fromLegacyNetSpec(net);
    } else {
      return NetworkConfig.builder().build();
    }
  }

  public RunVolumeConfiguration getVolumeConfiguration() {
    return volumes;
  }

  @Nonnull
  public List<String> getLinks() {
    return EnvUtil.splitAtCommasAndTrim(links);
  }

  /**
   * Naming scheme for how to name container.
   *
   * @deprecated for backward compatibility, use containerNamePattern instead
   */
  @Deprecated
  public enum NamingStrategy {
    /**
     * No extra naming
     */
    none,
    /**
     * Use the alias as defined in the configuration
     */
    alias
  }

  public RestartPolicy getRestartPolicy() {
    return Optional.ofNullable(restartPolicy).orElse(RestartPolicy.DEFAULT);
  }

  public boolean skip() {
    return Optional.ofNullable(skip).orElse(false);
  }

  public static class RunImageConfigurationBuilder {
    public RunImageConfigurationBuilder cmdString(String cmdString) {
      if (cmdString != null) {
        cmd = Arguments.builder().shell(cmdString).build();
      }
      return this;
    }

    /**
     * @deprecated use {@link #containerNamePattern} instead
     * @return RunImageConfigurationBuilder object
     */
    @Deprecated
    public RunImageConfigurationBuilder namingStrategyString(String namingStrategyString) {
      namingStrategy = Optional.ofNullable(namingStrategyString)
          .map(String::toLowerCase)
          .map(NamingStrategy::valueOf)
          .orElse(NamingStrategy.none);
      return this;
    }
  }
}

