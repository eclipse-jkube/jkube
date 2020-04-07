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
package org.eclipse.jkube.kit.build.service.docker.config.handler.property;

import org.eclipse.jkube.kit.build.service.docker.ImageConfiguration;
import org.eclipse.jkube.kit.build.service.docker.config.LogConfiguration;
import org.eclipse.jkube.kit.build.service.docker.config.NetworkConfig;
import org.eclipse.jkube.kit.build.service.docker.config.RestartPolicy;
import org.eclipse.jkube.kit.build.service.docker.config.RunImageConfiguration;
import org.eclipse.jkube.kit.build.service.docker.config.RunVolumeConfiguration;
import org.eclipse.jkube.kit.build.service.docker.config.UlimitConfig;
import org.eclipse.jkube.kit.build.service.docker.config.WaitConfiguration;
import org.eclipse.jkube.kit.build.service.docker.config.WatchImageConfiguration;
import org.eclipse.jkube.kit.build.service.docker.config.handler.ExternalConfigHandler;
import org.eclipse.jkube.kit.common.JKubeProject;
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.common.util.MapUtil;
import org.eclipse.jkube.kit.config.image.build.Arguments;
import org.eclipse.jkube.kit.config.image.build.AssemblyConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.HealthCheckConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.ALIAS;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.ARGS;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.ASSEMBLY_BASEDIR;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.ASSEMBLY_DESCRIPTOR;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.ASSEMBLY_DESCRIPTOR_REF;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.ASSEMBLY_DOCKER_FILE_DIR;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.ASSEMBLY_EXPORT_BASEDIR;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.ASSEMBLY_IGNORE_PERMISSIONS;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.ASSEMBLY_MODE;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.ASSEMBLY_PERMISSIONS;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.ASSEMBLY_TARLONGFILEMODE;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.ASSEMBLY_USER;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.AUTO_REMOVE;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.BIND;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.BUILD_OPTIONS;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.CAP_ADD;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.CAP_DROP;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.CLEANUP;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.CMD;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.CONTEXT_DIR;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.CPUS;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.CPUSET;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.CPUSHARES;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.DEPENDS_ON;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.DNS;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.DNS_SEARCH;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.DOCKER_ARCHIVE;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.DOCKER_FILE;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.DOCKER_FILE_DIR;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.DOMAINNAME;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.ENTRYPOINT;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.ENV;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.ENV_BUILD;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.ENV_PROPERTY_FILE;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.ENV_RUN;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.EXPOSED_PROPERTY_KEY;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.EXTRA_HOSTS;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.FILTER;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.FROM;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.FROM_EXT;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.HEALTHCHECK;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.HEALTHCHECK_CMD;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.HEALTHCHECK_INTERVAL;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.HEALTHCHECK_MODE;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.HEALTHCHECK_RETRIES;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.HEALTHCHECK_START_PERIOD;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.HEALTHCHECK_TIMEOUT;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.HOSTNAME;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.IMAGE_PULL_POLICY_BUILD;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.IMAGE_PULL_POLICY_RUN;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.LABELS;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.LINKS;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.LOG_COLOR;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.LOG_DATE;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.LOG_DRIVER_NAME;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.LOG_DRIVER_OPTS;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.LOG_ENABLED;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.LOG_FILE;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.LOG_PREFIX;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.MAINTAINER;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.MEMORY;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.MEMORY_SWAP;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.NAME;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.NAMING_STRATEGY;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.NET;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.NETWORK_ALIAS;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.NETWORK_MODE;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.NETWORK_NAME;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.NOCACHE;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.OPTIMISE;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.PORTS;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.PORT_PROPERTY_FILE;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.PRIVILEGED;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.READ_ONLY;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.REGISTRY;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.RESTART_POLICY_NAME;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.RESTART_POLICY_RETRY;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.RUN;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.SECURITY_OPTS;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.SHELL;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.SHMSIZE;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.SKIP_BUILD;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.SKIP_RUN;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.TAGS;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.TMPFS;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.USER;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.VOLUMES;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.VOLUMES_FROM;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.WAIT_EXEC_BREAK_ON_ERROR;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.WAIT_EXEC_POST_START;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.WAIT_EXEC_PRE_STOP;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.WAIT_EXIT;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.WAIT_HEALTHY;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.WAIT_HTTP_METHOD;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.WAIT_HTTP_STATUS;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.WAIT_HTTP_URL;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.WAIT_KILL;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.WAIT_LOG;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.WAIT_SHUTDOWN;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.WAIT_TCP_HOST;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.WAIT_TCP_MODE;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.WAIT_TCP_PORT;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.WAIT_TIME;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.WAIT_URL;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.WATCH_INTERVAL;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.WATCH_POSTEXEC;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.WATCH_POSTGOAL;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.WORKDIR;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.WORKING_DIR;


/**
 * @author roland
 * @since 18/11/14
 */
// Moved temporarily to resources/META-INF/plexus/components.xml because of https://github.com/codehaus-plexus/plexus-containers/issues/4
// @Component(role = ExternalConfigHandler.class)
public class PropertyConfigHandler implements ExternalConfigHandler {

    public static final String TYPE_NAME = "properties";
    public static final String DEFAULT_PREFIX = "docker";

    @Override
    public String getType() {
        return TYPE_NAME;
    }

    @Override
    public List<ImageConfiguration> resolve(ImageConfiguration fromConfig, JKubeProject project) {
        Map<String, String> externalConfig = fromConfig.getExternalConfig();
        String prefix = getPrefix(externalConfig);
        Properties properties = JKubeProjectUtil.getPropertiesWithSystemOverrides(project);
        PropertyMode propertyMode = getMode(externalConfig);
        ValueProvider valueProvider = new ValueProvider(prefix, properties, propertyMode);

        RunImageConfiguration run = extractRunConfiguration(fromConfig, valueProvider);
        BuildConfiguration build = extractBuildConfiguration(fromConfig, valueProvider, project);
        WatchImageConfiguration watch = extractWatchConfig(fromConfig, valueProvider);
        String name = valueProvider.getString(NAME, fromConfig.getName());
        String alias = valueProvider.getString(ALIAS, fromConfig.getAlias());

        if (name == null) {
            throw new IllegalArgumentException(String.format("Mandatory property [%s] is not defined", NAME));
        }

        return Collections.singletonList(
                new ImageConfiguration.Builder()
                        .name(name)
                        .alias(alias)
                        .runConfig(run)
                        .buildConfig(build)
                        .watchConfig(watch)
                        .build());
    }

    private static boolean isStringValueNull(ValueProvider valueProvider, BuildConfiguration config, ConfigKey key, Supplier<String> supplier) {
        return valueProvider.getString(key, config == null ? null : supplier.get()) != null;
    }

    // Enable build config only when a `.from.`, `.dockerFile.`, or `.dockerFileDir.` is configured
    private boolean buildConfigured(BuildConfiguration config, ValueProvider valueProvider, JKubeProject project) {


        if (isStringValueNull(valueProvider, config, FROM, config::getFrom)) {
            return true;
        }

        if (valueProvider.getMap(FROM_EXT, config == null ? null : config.getFromExt()) != null) {
            return true;
        }
        if (isStringValueNull(valueProvider, config, DOCKER_FILE, () -> config.getDockerFileRaw() ))  {
            return true;
        }
        if (isStringValueNull(valueProvider, config, DOCKER_ARCHIVE, () -> config.getDockerArchiveRaw())) {
            return true;
        }

        if (isStringValueNull(valueProvider, config, CONTEXT_DIR, () -> config.getContextDirRaw())) {
            return true;
        }

        if (isStringValueNull(valueProvider, config, DOCKER_FILE_DIR, () -> config.getDockerFileDirRaw())) {
            return true;
        }

        // Simple Dockerfile mode
        return new File(project.getBaseDirectory(),"Dockerfile").exists();
    }

    private static <T, R> R valueOrNull(T input, Function<T,R> function) {
        return Optional.ofNullable(input).map(function).orElse(null);
    }

    private BuildConfiguration extractBuildConfiguration(ImageConfiguration fromConfig, ValueProvider valueProvider, JKubeProject project) {
        BuildConfiguration config = fromConfig.getBuildConfiguration();
        if (!buildConfigured(config, valueProvider, project)) {
            return null;
        }
        return new BuildConfiguration.Builder()
                .cmd(extractArguments(valueProvider, CMD, valueOrNull(config, BuildConfiguration::getCmd)))
                .cleanup(valueProvider.getString(CLEANUP, valueOrNull(config, BuildConfiguration::getCleanup)))
                .nocache(valueProvider.getBoolean(NOCACHE, valueOrNull(config, BuildConfiguration::getNoCache)))
                .optimise(valueProvider.getBoolean(OPTIMISE, valueOrNull(config, BuildConfiguration::getOptimise)))
                .entryPoint(extractArguments(valueProvider, ENTRYPOINT, valueOrNull(config, BuildConfiguration::getEntryPoint)))
                .assembly(extractAssembly(valueOrNull(config, BuildConfiguration::getAssemblyConfiguration), valueProvider))
                .env(MapUtil.mergeMaps(
                        valueProvider.getMap(ENV_BUILD, valueOrNull(config, BuildConfiguration::getEnv)),
                        valueProvider.getMap(ENV, Collections.emptyMap())
                ))
                .args(valueProvider.getMap(ARGS, valueOrNull(config, BuildConfiguration::getArgs)))
                .labels(valueProvider.getMap(LABELS, valueOrNull(config, BuildConfiguration::getLabels)))
                .ports(extractPortValues(valueOrNull(config, BuildConfiguration::getPorts), valueProvider))
                .shell(extractArguments(valueProvider, SHELL, valueOrNull(config, BuildConfiguration::getShell)))
                .runCmds(valueProvider.getList(RUN, valueOrNull(config, BuildConfiguration::getRunCmds)))
                .from(valueProvider.getString(FROM, valueOrNull(config, BuildConfiguration::getFrom)))
                .fromExt(valueProvider.getMap(FROM_EXT, valueOrNull(config, BuildConfiguration::getFromExt)))
                .registry(valueProvider.getString(REGISTRY, valueOrNull(config, BuildConfiguration::getRegistry)))
                .volumes(valueProvider.getList(VOLUMES, valueOrNull(config, BuildConfiguration::getVolumes)))
                .tags(valueProvider.getList(TAGS, valueOrNull(config, BuildConfiguration::getTags)))
                .maintainer(valueProvider.getString(MAINTAINER, valueOrNull(config, BuildConfiguration::getMaintainer)))
                .workdir(valueProvider.getString(WORKDIR, valueOrNull(config, BuildConfiguration::getWorkdir)))
                .skip(valueProvider.getBoolean(SKIP_BUILD, valueOrNull(config, BuildConfiguration::getSkip)))
                .imagePullPolicy(valueProvider.getString(IMAGE_PULL_POLICY_BUILD, valueOrNull(config, BuildConfiguration::getImagePullPolicy)))
                .contextDir(valueProvider.getString(CONTEXT_DIR, valueOrNull(config, BuildConfiguration::getContextDirRaw)))
                .dockerArchive(valueProvider.getString(DOCKER_ARCHIVE, valueOrNull(config, BuildConfiguration::getDockerArchiveRaw)))
                .dockerFile(valueProvider.getString(DOCKER_FILE, valueOrNull(config, BuildConfiguration::getDockerFileRaw)))
                .dockerFileDir(valueProvider.getString(DOCKER_FILE_DIR, valueOrNull(config, BuildConfiguration::getDockerFileDirRaw)))
                .buildOptions(valueProvider.getMap(BUILD_OPTIONS, valueOrNull(config, BuildConfiguration::getBuildOptions)))
                .filter(valueProvider.getString(FILTER, valueOrNull(config, BuildConfiguration::getFilterRaw)))
                .user(valueProvider.getString(USER, valueOrNull(config, BuildConfiguration::getUser)))
                .healthCheck(extractHealthCheck(valueOrNull(config, BuildConfiguration::getHealthCheck), valueProvider))
                .build();
    }

    private RunImageConfiguration extractRunConfiguration(ImageConfiguration fromConfig, ValueProvider valueProvider) {
        RunImageConfiguration config = fromConfig.getRunConfiguration();
        if (config.isDefault()) {
            config = null;
        }

        return new RunImageConfiguration.Builder()
                .capAdd(valueProvider.getList(CAP_ADD, config == null ? null : config.getCapAdd()))
                .capDrop(valueProvider.getList(CAP_DROP, config == null ? null : config.getCapDrop()))
                .securityOpts(valueProvider.getList(SECURITY_OPTS, config == null ? null : config.getSecurityOpts()))
                .cmd(extractArguments(valueProvider, CMD, config == null ? null : config.getCmd()))
                .dns(valueProvider.getList(DNS, config == null ? null : config.getDns()))
                .dependsOn(valueProvider.getList(DEPENDS_ON, config == null ? null : config.getDependsOn()))
                .net(valueProvider.getString(NET, config == null ? null : config.getNetRaw()))
                .network(extractNetworkConfig(config == null ? null : config.getNetworkingConfig(), valueProvider))
                .dnsSearch(valueProvider.getList(DNS_SEARCH, config == null ? null : config.getDnsSearch()))
                .domainname(valueProvider.getString(DOMAINNAME, config == null ? null : config.getDomainname()))
                .entrypoint(extractArguments(valueProvider, ENTRYPOINT, config == null ? null : config.getEntrypoint()))
                .env(MapUtil.mergeMaps(
                        valueProvider.getMap(ENV_RUN, config == null ? null : config.getEnv()),
                        valueProvider.getMap(ENV, Collections.<String, String>emptyMap())
                ))
                .labels(valueProvider.getMap(LABELS, config == null ? null : config.getLabels()))
                .envPropertyFile(valueProvider.getString(ENV_PROPERTY_FILE, config == null ? null : config.getEnvPropertyFile()))
                .extraHosts(valueProvider.getList(EXTRA_HOSTS, config == null ? null : config.getExtraHosts()))
                .hostname(valueProvider.getString(HOSTNAME, config == null ? null : config.getHostname()))
                .links(valueProvider.getList(LINKS, config == null ? null : config.getLinks()))
                .memory(valueProvider.getLong(MEMORY, config == null ? null : config.getMemory()))
                .memorySwap(valueProvider.getLong(MEMORY_SWAP, config == null ? null : config.getMemorySwap()))
                .namingStrategy(valueProvider.getString(NAMING_STRATEGY, config == null || config.getNamingStrategy() == null ? null : config.getNamingStrategy().name()))
                .exposedPropertyKey(valueProvider.getString(EXPOSED_PROPERTY_KEY, config == null ? null : config.getExposedPropertyKey()))
                .portPropertyFile(valueProvider.getString(PORT_PROPERTY_FILE, config == null ? null : config.getPortPropertyFile()))
                .ports(valueProvider.getList(PORTS, config == null ? null : config.getPorts()))
                .shmSize(valueProvider.getLong(SHMSIZE, config == null ? null : config.getShmSize()))
                .privileged(valueProvider.getBoolean(PRIVILEGED, config == null ? null : config.getPrivileged()))
                .restartPolicy(extractRestartPolicy(config == null ? null : config.getRestartPolicy(), valueProvider))
                .user(valueProvider.getString(USER, config == null ? null : config.getUser()))
                .workingDir(valueProvider.getString(WORKING_DIR, config == null ? null : config.getWorkingDir()))
                .log(extractLogConfig(config == null ? null : config.getLogConfiguration(), valueProvider))
                .wait(extractWaitConfig(config == null ? null : config.getWaitConfiguration(), valueProvider))
                .volumes(extractVolumeConfig(config == null ? null : config.getVolumeConfiguration(), valueProvider))
                .skip(valueProvider.getBoolean(SKIP_RUN, config == null ? null : config.getSkip()))
                .imagePullPolicy(valueProvider.getString(IMAGE_PULL_POLICY_RUN, config == null ? null : config.getImagePullPolicy()))
                .ulimits(extractUlimits(config == null ? null : config.getUlimits(), valueProvider))
                .tmpfs(valueProvider.getList(TMPFS, config == null ? null : config.getTmpfs()))
                .cpuShares(valueProvider.getLong(CPUSHARES, config == null ? null : config.getCpuShares()))
                .cpus(valueProvider.getLong(CPUS, config == null ? null : config.getCpus()))
                .cpuSet(valueProvider.getString(CPUSET, config == null ? null : config.getCpuSet()))
                .readOnly(valueProvider.getBoolean(READ_ONLY, config == null ? null : config.getReadOnly()))
                .autoRemove(valueProvider.getBoolean(AUTO_REMOVE, config == null ? null : config.getAutoRemove()))
                .build();
    }

    private NetworkConfig extractNetworkConfig(NetworkConfig config, ValueProvider valueProvider) {
        return new NetworkConfig.Builder()
            .mode(valueProvider.getString(NETWORK_MODE, config == null || config.getMode() == null ? null : config.getMode().name()))
            .name(valueProvider.getString(NETWORK_NAME, config == null ? null : config.getName()))
            .aliases(valueProvider.getList(NETWORK_ALIAS, config == null ? null : config.getAliases()))
            .build();
    }

    @SuppressWarnings("deprecation")
    private AssemblyConfiguration extractAssembly(AssemblyConfiguration config, ValueProvider valueProvider) {
        return new AssemblyConfiguration.Builder()
                .targetDir(valueProvider.getString(ASSEMBLY_BASEDIR, config == null ? null : config.getTargetDir()))
                .descriptor(valueProvider.getString(ASSEMBLY_DESCRIPTOR, config == null ? null : config.getDescriptor()))
                .descriptorRef(valueProvider.getString(ASSEMBLY_DESCRIPTOR_REF, config == null ? null : config.getDescriptorRef()))
                .dockerFileDir(valueProvider.getString(ASSEMBLY_DOCKER_FILE_DIR, config == null ? null : config.getDockerFileDir()))
                .exportBasedir(valueProvider.getBoolean(ASSEMBLY_EXPORT_BASEDIR, config == null ? null : config.getExportTargetDir()))
                .ignorePermissions(valueProvider.getBoolean(ASSEMBLY_IGNORE_PERMISSIONS, config == null ? null : config.getIgnorePermissions()))
                .permissions(valueProvider.getString(ASSEMBLY_PERMISSIONS, config == null ? null : config.getPermissionsRaw()))
                .user(valueProvider.getString(ASSEMBLY_USER, config == null ? null : config.getUser()))
                .mode(valueProvider.getString(ASSEMBLY_MODE, config == null ? null : config.getModeRaw()))
                .tarLongFileMode(valueProvider.getString(ASSEMBLY_TARLONGFILEMODE, config == null ? null : config.getTarLongFileMode()))
                .build();
    }

    private HealthCheckConfiguration extractHealthCheck(HealthCheckConfiguration config, ValueProvider valueProvider) {
        Map<String, String> healthCheckProperties = valueProvider.getMap(HEALTHCHECK, Collections.emptyMap());
        if (healthCheckProperties != null && healthCheckProperties.size() > 0) {
            return new HealthCheckConfiguration.Builder()
                    .interval(valueProvider.getString(HEALTHCHECK_INTERVAL, valueOrNull(config, HealthCheckConfiguration::getInterval)))
                    .timeout(valueProvider.getString(HEALTHCHECK_TIMEOUT, valueOrNull(config, HealthCheckConfiguration::getTimeout)))
                    .startPeriod(valueProvider.getString(HEALTHCHECK_START_PERIOD, valueOrNull(config, HealthCheckConfiguration::getStartPeriod)))
                    .retries(valueProvider.getInteger(HEALTHCHECK_RETRIES, valueOrNull(config, HealthCheckConfiguration::getRetries)))
                    .mode(valueProvider.getString(HEALTHCHECK_MODE, config == null || config.getMode() == null ? null : config.getMode().name()))
                    .cmd(extractArguments(valueProvider, HEALTHCHECK_CMD, valueOrNull(config, HealthCheckConfiguration::getCmd)))
                    .build();
        } else {
            return config;
        }
    }

    // Extract only the values of the port mapping

    private List<String> extractPortValues(List<String> config, ValueProvider valueProvider) {
        List<String> ret = new ArrayList<>();
        List<String> ports = valueProvider.getList(PORTS, config);
        if (ports == null) {
            return null;
        }
        List<String[]> parsedPorts = EnvUtil.splitOnLastColon(ports);
        for (String[] port : parsedPorts) {
            ret.add(port[1]);
        }
        return ret;
    }

    private Arguments extractArguments(ValueProvider valueProvider, ConfigKey configKey, Arguments alternative) {
        return valueProvider.getObject(configKey, alternative, raw -> raw != null ? new Arguments(raw) : null);
    }

    private RestartPolicy extractRestartPolicy(RestartPolicy config, ValueProvider valueProvider) {
        return new RestartPolicy.Builder()
                .name(valueProvider.getString(RESTART_POLICY_NAME, config == null ? null : config.getName()))
                .retry(valueProvider.getInt(RESTART_POLICY_RETRY, config == null || config.getRetry() == 0 ? null : config.getRetry()))
                .build();
    }

    private LogConfiguration extractLogConfig(LogConfiguration config, ValueProvider valueProvider) {
        LogConfiguration.Builder builder = new LogConfiguration.Builder()
            .color(valueProvider.getString(LOG_COLOR, config == null ? null : config.getColor()))
            .date(valueProvider.getString(LOG_DATE, config == null ? null : config.getDate()))
            .file(valueProvider.getString(LOG_FILE, config == null ? null : config.getFileLocation()))
            .prefix(valueProvider.getString(LOG_PREFIX, config == null ? null : config.getPrefix()))
            .logDriverName(valueProvider.getString(LOG_DRIVER_NAME, config == null || config.getDriver() == null ? null : config.getDriver().getName()))
            .logDriverOpts(valueProvider.getMap(LOG_DRIVER_OPTS, config == null || config.getDriver() == null ? null : config.getDriver().getOpts()));

        Boolean configEnabled = config != null ? config.isEnabled() : null;
        Boolean enabled = valueProvider.getBoolean(LOG_ENABLED, configEnabled);
        builder.enabled(enabled);
        return builder.build();
    }

    private WaitConfiguration extractWaitConfig(WaitConfiguration config, ValueProvider valueProvider) {
        String url = valueProvider.getString(WAIT_HTTP_URL, config == null ? null : config.getUrl());
        if (url == null) {
            // Fallback to deprecated old URL
            url = valueProvider.getString(WAIT_URL, config == null ? null : config.getUrl());
        }
        WaitConfiguration.ExecConfiguration exec = config == null ? null : config.getExec();
        WaitConfiguration.TcpConfiguration tcp = config == null ? null : config.getTcp();
        WaitConfiguration.HttpConfiguration http = config == null ? null : config.getHttp();

        return new WaitConfiguration.Builder()
                .time(valueProvider.getInt(WAIT_TIME, config == null ? null : config.getTime()))
                .healthy(valueProvider.getBoolean(WAIT_HEALTHY, config == null ? null : config.getHealthy()))
                .url(url)
                .preStop(valueProvider.getString(WAIT_EXEC_PRE_STOP, exec == null ? null : exec.getPreStop()))
                .postStart(valueProvider.getString(WAIT_EXEC_POST_START, exec == null ? null : exec.getPostStart()))
                .breakOnError(valueProvider.getBoolean(WAIT_EXEC_BREAK_ON_ERROR, exec == null ? null : exec.isBreakOnError()))
                .method(valueProvider.getString(WAIT_HTTP_METHOD, http == null ? null : http.getMethod()))
                .status(valueProvider.getString(WAIT_HTTP_STATUS, http == null ? null : http.getStatus()))
                .log(valueProvider.getString(WAIT_LOG, config == null ? null : config.getLog()))
                .kill(valueProvider.getInteger(WAIT_KILL, config == null ? null : config.getKill()))
                .exit(valueProvider.getInteger(WAIT_EXIT, config == null ? null : config.getExit()))
                .shutdown(valueProvider.getInteger(WAIT_SHUTDOWN, config == null ? null : config.getShutdown()))
                .tcpHost(valueProvider.getString(WAIT_TCP_HOST, tcp == null ? null : tcp.getHost()))
                .tcpPorts(valueProvider.getIntList(WAIT_TCP_PORT, tcp == null ? null : tcp.getPorts()))
                .tcpMode(valueProvider.getString(WAIT_TCP_MODE, tcp == null || tcp.getMode() == null ? null : tcp.getMode().name()))
                .build();
    }

    private WatchImageConfiguration extractWatchConfig(ImageConfiguration fromConfig, ValueProvider valueProvider) {
        WatchImageConfiguration config = fromConfig.getWatchConfiguration();

        return new WatchImageConfiguration.Builder()
                .interval(valueProvider.getInteger(WATCH_INTERVAL, config == null ? null : config.getIntervalRaw()))
                .postGoal(valueProvider.getString(WATCH_POSTGOAL, config == null ? null : config.getPostGoal()))
                .postExec(valueProvider.getString(WATCH_POSTEXEC, config == null ? null : config.getPostExec()))
                .mode(valueProvider.getString(WATCH_POSTGOAL, config == null || config.getMode() == null ? null : config.getMode().name()))
                .build();
    }

    private List<UlimitConfig> extractUlimits(List<UlimitConfig> config, ValueProvider valueProvider) {
        List<String> other = null;
        if (config != null) {
            other = new ArrayList<>();
            // Convert back to string for potential merge
            for (UlimitConfig ulimitConfig : config) {
                other.add(ulimitConfig.serialize());
            }
        }

        List<String> ulimits = valueProvider.getList(ConfigKey.ULIMITS, other);
        if (ulimits == null) {
            return null;
        }
        List<UlimitConfig> ret = new ArrayList<>();
        for (String ulimit : ulimits) {
            ret.add(new UlimitConfig(ulimit));
        }
        return ret;
    }

    private RunVolumeConfiguration extractVolumeConfig(RunVolumeConfiguration config, ValueProvider valueProvider) {
        return new RunVolumeConfiguration.Builder()
                .bind(valueProvider.getList(BIND, config == null ? null : config.getBind()))
                .from(valueProvider.getList(VOLUMES_FROM, config == null ? null : config.getFrom()))
                .build();
    }

    private static String getPrefix(Map<String, String> externalConfig) {
        String prefix = externalConfig.get("prefix");
        if (prefix == null) {
            prefix = DEFAULT_PREFIX;
        }
        return prefix;
    }

    private static PropertyMode getMode(Map<String, String> externalConfig) {
        return PropertyMode.parse(externalConfig.get("mode"));
    }

    public static boolean canCoexistWithOtherPropertyConfiguredImages(Map<String, String> externalConfig) {
        if(externalConfig == null || externalConfig.isEmpty()) {
            return false;
        }

        if(!TYPE_NAME.equals(externalConfig.get("type")))
        {
            // This images loads config from something totally different
            return true;
        }

        if(externalConfig.get("prefix") != null)
        {
            // This image has a specified prefix. If multiple images have explicitly set docker. as prefix we
            // assume user know what they are doing and allow it.
            return true;
        }

        return false;
    }
}
