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

import javax.annotation.Nonnull;

import org.eclipse.jkube.kit.build.service.docker.helper.DeepCopy;
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.eclipse.jkube.kit.config.image.build.Arguments;

/**
 * @author roland
 * @since 02.09.14
 */
public class RunImageConfiguration implements Serializable {

    public static final RunImageConfiguration DEFAULT = new RunImageConfiguration();

    public boolean isDefault() {
        return this == RunImageConfiguration.DEFAULT;
    }

    /**
     * Environment variables to set when starting the container. key: variable name, value: env value
     */
    private Map<String, String> env;

    private Map<String,String> labels;

    // Path to a property file holding environment variables

    private String envPropertyFile;

    // Command to execute in container

    private Arguments cmd;

    // container domain name

    private String domainname;

    // container domain name

    private List<String> dependsOn;

    // container entry point

    private Arguments entrypoint;

    // container hostname

    private String hostname;

    // container user

    private String user;

    // working directory

    private String workingDir;

    // Size of /dev/shm in bytes
    private Long shmSize;

    // memory in bytes

    private Long memory;

    // total memory (swap + ram) in bytes, -1 to disable

    private Long memorySwap;

    // Path to a file where the dynamically mapped properties are written to

    private String portPropertyFile;

    // For simple network setups. For complex stuff use "network"

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

    // Port mapping. Can contain symbolic names in which case dynamic
    // ports are used

    private List<String> ports;

    /**
     * @deprecated
     */

    @Deprecated
    private NamingStrategy namingStrategy;

    /**
     * A pattern to define the naming of the container where
     *
     * - %a for the "alias" mode
     * - %n for the image name
     * - %t for a timestamp
     * - %i for an increasing index of container names
     *
     */

    private String containerNamePattern;

    /**
     * Property key part used to expose the container ip when running.
     */

    private String exposedPropertyKey;

    // Mount volumes from the given image's started containers

    private RunVolumeConfiguration volumes;

    // Links to other container started

    private List<String> links;

    // Configuration for how to wait during startup of the container

    private WaitConfiguration wait;

    // Mountpath for tmps

    private List<String> tmpfs;


    private LogConfiguration log;


    private RestartPolicy restartPolicy;


    private List<UlimitConfig> ulimits;


    private Boolean skip;

    /**
     * Policy for pulling the image to start
     */

    private String imagePullPolicy;

    // Mount the container's root filesystem as read only

    private Boolean readOnly;

    // Automatically remove the container when it exists

    private Boolean autoRemove;

    public RunImageConfiguration() { }

    public String initAndValidate() {
        if (entrypoint != null) {
            entrypoint.validate();
        }
        if (cmd != null) {
            cmd.validate();
        }

        // Custom networks are available since API 1.21 (Docker 1.9)
        NetworkConfig config = getNetworkingConfig();
        if (config != null && config.isCustomNetwork()) {
            return "1.21";
        }

        return null;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public String getEnvPropertyFile() {
        return envPropertyFile;
    }

    public Arguments getEntrypoint() {
        return entrypoint;
    }

    public String getHostname() {
        return hostname;
    }

    public String getDomainname() {
        return domainname;
    }

    @Nonnull
    public List<String> getDependsOn() {
        return EnvUtil.splitAtCommasAndTrim(dependsOn);
    }

    public String getUser() {
        return user;
    }

    public Long getShmSize() {
        return shmSize;
    }

    public Long getMemory() {
        return memory;
    }

    public Long getMemorySwap() {
        return memorySwap;
    }

    public Long getCpuShares() {
        return cpuShares;
    }

    public Long getCpus() {
        return cpus;
    }

    public String getCpuSet() {
        return cpuSet;
    }

    @Nonnull
    public List<String> getPorts() {
        return EnvUtil.removeEmptyEntries(ports);
    }

    public Arguments getCmd() {
        return cmd;
    }

    public String getPortPropertyFile() {
        return portPropertyFile;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public WaitConfiguration getWaitConfiguration() {
        return wait;
    }

    public LogConfiguration getLogConfiguration() {
        return log;
    }

    public List<String> getCapAdd() {
        return capAdd;
    }

    public List<String> getCapDrop() {
        return capDrop;
    }

    public List<String> getSecurityOpts() {
        return securityOpts;
    }

    public List<String> getDns() {
        return dns;
    }

    @Deprecated
    public String getNetRaw() {
        return net;
    }

    public NetworkConfig getNetworkingConfig() {
        if (network != null) {
            return network;
        } else if (net != null) {
            return new NetworkConfig(net);
        } else {
            return new NetworkConfig();
        }
    }

    public List<String> getDnsSearch() {
        return dnsSearch;
    }

    public List<String> getExtraHosts() {
        return extraHosts;
    }

    public RunVolumeConfiguration getVolumeConfiguration() {
        return volumes;
    }

    @Nonnull
    public List<String> getLinks() {
        return EnvUtil.splitAtCommasAndTrim(links);
    }

    public List<UlimitConfig> getUlimits() {
        return ulimits;
    }

    public List<String> getTmpfs() {
        return tmpfs;
    }

    /**
     * @deprecated
     */
    // Naming scheme for how to name container
    @Deprecated // for backward compatibility, us containerNamePattern instead
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

    public String getExposedPropertyKey() {
        return exposedPropertyKey;
    }

    public Boolean getPrivileged() {
        return privileged;
    }

    public RestartPolicy getRestartPolicy() {
        return (restartPolicy == null) ? RestartPolicy.DEFAULT : restartPolicy;
    }

    public RestartPolicy getRestartPolicyRaw() {
        return restartPolicy;
    }

    public boolean skip() {
        return skip != null ? skip : false;
    }

    public Boolean getSkip() {
        return skip;
    }

    public String getImagePullPolicy() {
        return imagePullPolicy;
    }

    public String getContainerNamePattern() {
        return containerNamePattern;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public Boolean getAutoRemove() {
        return autoRemove;
    }

    /**
     * @deprecated use {@link #getContainerNamePattern} instead
     * @return NamingStrategy naming strategy
     */
    @Deprecated
    public NamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    // ======================================================================================

    public static class Builder {

        public Builder(RunImageConfiguration config) {
            if (config == null) {
                this.config = new RunImageConfiguration();
            } else {
                this.config = DeepCopy.copy(config);
            }
        }

        public Builder() {
            this(null);
        }

        private RunImageConfiguration config;

        public Builder env(Map<String, String> env) {
            config.env = env;
            return this;
        }

        public Builder labels(Map<String, String> labels) {
            config.labels = labels;
            return this;
        }


        public Builder envPropertyFile(String envPropertyFile) {
            config.envPropertyFile = envPropertyFile;
            return this;
        }

        public Builder cmd(String cmd) {
            if (cmd != null) {
                config.cmd = new Arguments(cmd);
            }
            return this;
        }

        public Builder cmd(Arguments args) {
            config.cmd = args;
            return this;
        }

        public Builder domainname(String domainname) {
            config.domainname = domainname;
            return this;
        }

        public Builder entrypoint(Arguments args) {
            config.entrypoint = args;
            return this;
        }

        public Builder hostname(String hostname) {
            config.hostname = hostname;
            return this;
        }

        public Builder portPropertyFile(String portPropertyFile) {
            config.portPropertyFile = portPropertyFile;
            return this;
        }

        public Builder workingDir(String workingDir) {
            config.workingDir = workingDir;
            return this;
        }

        public Builder user(String user) {
            config.user = user;
            return this;
        }

        public Builder shmSize(Long shmSize) {
            config.shmSize = shmSize;
            return this;
        }

        public Builder memory(Long memory) {
            config.memory = memory;
            return this;
        }

        public Builder memorySwap(Long memorySwap) {
            config.memorySwap = memorySwap;
            return this;
        }

        public Builder capAdd(List<String> capAdd) {
            config.capAdd = capAdd;
            return this;
        }

        public Builder capDrop(List<String> capDrop) {
            config.capDrop = capDrop;
            return this;
        }

        public Builder securityOpts(List<String> securityOpts) {
            config.securityOpts = securityOpts;
            return this;
        }

        public Builder net(String net) {
            config.net = net;
            return this;
        }

        public Builder network(NetworkConfig networkConfig) {
            config.network = networkConfig;
            return this;
        }

        public Builder dependsOn(List<String> dependsOn) {
            config.dependsOn = dependsOn;
            return this;
        }

        public Builder dns(List<String> dns) {
            config.dns = dns;
            return this;
        }

        public Builder dnsSearch(List<String> dnsSearch) {
            config.dnsSearch = dnsSearch;
            return this;
        }

        public Builder extraHosts(List<String> extraHosts) {
            config.extraHosts = extraHosts;
            return this;
        }

        public Builder ulimits(List<UlimitConfig> ulimits) {
            config.ulimits = ulimits;
            return this;
        }

        public Builder ports(List<String> ports) {
            config.ports = ports;
            return this;
        }

        public Builder volumes(RunVolumeConfiguration volumes) {
            config.volumes = volumes;
            return this;
        }

        public Builder links(List<String> links) {
            config.links = links;
            return this;
        }

        public Builder tmpfs(List<String> tmpfs) {
            config.tmpfs = tmpfs;
            return this;
        }

        public Builder wait(WaitConfiguration wait) {
            config.wait = wait;
            return this;
        }

        public Builder log(LogConfiguration log) {
            config.log = log;
            return this;
        }

        public Builder cpuShares(Long cpuShares){
            config.cpuShares = cpuShares;
            return this;
        }

        public Builder cpus(Long cpus){
            config.cpus = cpus;
            return this;
        }

        public Builder cpuSet(String cpuSet){
            config.cpuSet = cpuSet;
            return this;
        }

        public Builder containerNamePattern(String pattern) {
            config.containerNamePattern = pattern;
            return this;
        }

        /**
         * @deprecated use {@link #containerNamePattern} instead
         *
         * @param namingStrategy naming strategy
         * @return Builder object
         */
        @Deprecated
        public Builder namingStrategy(String namingStrategy) {
            config.namingStrategy = namingStrategy == null ?
                    NamingStrategy.none :
                    NamingStrategy.valueOf(namingStrategy.toLowerCase());
            return this;
        }

        /**
         * @deprecated use {@link #containerNamePattern} instead
         *
         * @param namingStrategy naming strategy
         * @return builder object
         */
        @Deprecated
        public Builder namingStrategy(NamingStrategy namingStrategy) {
            config.namingStrategy = namingStrategy;
            return this;
        }

        public Builder exposedPropertyKey(String key) {
            config.exposedPropertyKey = key;
            return this;
        }

        public Builder privileged(Boolean privileged) {
            config.privileged = privileged;
            return this;
        }

        public Builder restartPolicy(RestartPolicy restartPolicy) {
            config.restartPolicy = restartPolicy;
            return this;
        }

        public Builder skip(Boolean skip) {
            config.skip = skip;
            return this;
        }

        public Builder imagePullPolicy(String imagePullPolicy) {
            if (imagePullPolicy != null) {
                config.imagePullPolicy = imagePullPolicy;
            }
            return this;
        }

        public Builder readOnly(Boolean readOnly) {
            config.readOnly = readOnly;
            return this;
        }

        public Builder autoRemove(Boolean autoRemove) {
            config.autoRemove = autoRemove;
            return this;
        }

        public RunImageConfiguration build() {
            return config;
        }
    }
}

