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
package org.eclipse.jkube.kit.build.service.docker.config.handler.property;

import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.WatchImageConfiguration;
import org.eclipse.jkube.kit.build.service.docker.config.handler.ExternalConfigHandler;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.common.util.MapUtil;
import org.eclipse.jkube.kit.common.Arguments;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.HealthCheckConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.ALIAS;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.ARGS;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.ASSEMBLY_BASEDIR;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.ASSEMBLY_EXPORT_TARGET_DIR;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.ASSEMBLY_MODE;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.ASSEMBLY_PERMISSIONS;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.ASSEMBLY_TARLONGFILEMODE;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.ASSEMBLY_USER;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.BUILD_OPTIONS;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.CACHEFROM;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.CLEANUP;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.CMD;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.CONTEXT_DIR;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.DOCKER_ARCHIVE;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.DOCKER_FILE;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.ENTRYPOINT;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.ENV;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.ENV_BUILD;
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
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.IMAGE_PULL_POLICY_BUILD;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.LABELS;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.MAINTAINER;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.NAME;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.NOCACHE;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.OPTIMISE;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.PORTS;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.RUN;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.SHELL;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.SKIP_BUILD;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.TAGS;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.USER;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.VOLUMES;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.WATCH_INTERVAL;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.WATCH_POSTEXEC;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.WATCH_POSTGOAL;
import static org.eclipse.jkube.kit.build.service.docker.config.handler.property.ConfigKey.WORKDIR;

/**
 * @author roland
 */
public class PropertyConfigHandler implements ExternalConfigHandler {

    public static final String TYPE_NAME = "properties";
    public static final String DEFAULT_PREFIX = "docker";

    @Override
    public String getType() {
        return TYPE_NAME;
    }

    @Override
    public List<ImageConfiguration> resolve(ImageConfiguration fromConfig, JavaProject project) {
        Map<String, String> externalConfig = fromConfig.getExternalConfig();
        String prefix = getPrefix(externalConfig);
        Properties properties = JKubeProjectUtil.getPropertiesWithSystemOverrides(project);
        PropertyMode propertyMode = getMode(externalConfig);
        ValueProvider valueProvider = new ValueProvider(prefix, properties, propertyMode);

        BuildConfiguration build = extractBuildConfiguration(fromConfig, valueProvider, project);
        WatchImageConfiguration watch = extractWatchConfig(fromConfig, valueProvider);
        String name = valueProvider.getString(NAME, fromConfig.getName());
        String alias = valueProvider.getString(ALIAS, fromConfig.getAlias());

        if (name == null) {
            throw new IllegalArgumentException(String.format("Mandatory property [%s] is not defined", NAME));
        }

        return Collections.singletonList(ImageConfiguration.builder()
                .name(name)
                .alias(alias)
                .build(build)
                .watch(watch)
                .build());
    }

    private static boolean isStringValueNull(ValueProvider valueProvider, BuildConfiguration config, ConfigKey key, Supplier<String> supplier) {
        return valueProvider.getString(key, config == null ? null : supplier.get()) != null;
    }

    // Enable build config only when a `.from.`, or `.dockerFile.` is configured
    private boolean buildConfigured(BuildConfiguration config, ValueProvider valueProvider, JavaProject project) {

        if (isStringValueNull(valueProvider, config, FROM, config::getFrom)) {
            return true;
        }

        if (valueProvider.getMap(FROM_EXT, config == null ? null : config.getFromExt()) != null) {
            return true;
        }
        if (isStringValueNull(valueProvider, config, DOCKER_FILE, config::getDockerFileRaw))  {
            return true;
        }
        if (isStringValueNull(valueProvider, config, DOCKER_ARCHIVE, config::getDockerArchiveRaw)) {
            return true;
        }

        if (isStringValueNull(valueProvider, config, CONTEXT_DIR, config::getContextDirRaw)) {
            return true;
        }

        // Simple Dockerfile mode
        return new File(project.getBaseDirectory(),"Dockerfile").exists();
    }

    private static <T, R> R valueOrNull(T input, Function<T,R> function) {
        return Optional.ofNullable(input).map(function).orElse(null);
    }

    private BuildConfiguration extractBuildConfiguration(ImageConfiguration fromConfig, ValueProvider valueProvider, JavaProject project) {
        BuildConfiguration config = fromConfig.getBuildConfiguration();
        if (!buildConfigured(config, valueProvider, project)) {
            return null;
        }
        return BuildConfiguration.builder()
                .cmd(extractArguments(valueProvider, CMD, valueOrNull(config, BuildConfiguration::getCmd)))
                .cleanup(valueProvider.getString(CLEANUP, valueOrNull(config, BuildConfiguration::getCleanup)))
                .nocache(valueProvider.getBoolean(NOCACHE, valueOrNull(config, BuildConfiguration::getNocache)))
                .cacheFrom(extractCacheFrom(valueProvider.getString(CACHEFROM, config == null ? null : (config.getCacheFrom() == null ? null : config.getCacheFrom().toString()))))
                .optimise(valueProvider.getBoolean(OPTIMISE, valueOrNull(config, BuildConfiguration::getOptimise)))
                .entryPoint(extractArguments(valueProvider, ENTRYPOINT, valueOrNull(config, BuildConfiguration::getEntryPoint)))
                .assembly(extractAssembly(valueOrNull(config, BuildConfiguration::getAssembly), valueProvider))
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
                .volumes(valueProvider.getList(VOLUMES, valueOrNull(config, BuildConfiguration::getVolumes)))
                .tags(valueProvider.getList(TAGS, valueOrNull(config, BuildConfiguration::getTags)))
                .maintainer(valueProvider.getString(MAINTAINER, valueOrNull(config, BuildConfiguration::getMaintainer)))
                .workdir(valueProvider.getString(WORKDIR, valueOrNull(config, BuildConfiguration::getWorkdir)))
                .skip(valueProvider.getBoolean(SKIP_BUILD, valueOrNull(config, BuildConfiguration::getSkip)))
                .imagePullPolicy(valueProvider.getString(IMAGE_PULL_POLICY_BUILD, valueOrNull(config, BuildConfiguration::getImagePullPolicy)))
                .contextDir(valueProvider.getString(CONTEXT_DIR, valueOrNull(config, BuildConfiguration::getContextDirRaw)))
                .dockerArchive(valueProvider.getString(DOCKER_ARCHIVE, valueOrNull(config, BuildConfiguration::getDockerArchiveRaw)))
                .dockerFile(valueProvider.getString(DOCKER_FILE, valueOrNull(config, BuildConfiguration::getDockerFileRaw)))
                .buildOptions(valueProvider.getMap(BUILD_OPTIONS, valueOrNull(config, BuildConfiguration::getBuildOptions)))
                .filter(valueProvider.getString(FILTER, valueOrNull(config, BuildConfiguration::getFilter)))
                .user(valueProvider.getString(USER, valueOrNull(config, BuildConfiguration::getUser)))
                .healthCheck(extractHealthCheck(valueOrNull(config, BuildConfiguration::getHealthCheck), valueProvider))
                .build();
    }

    List<String> extractCacheFrom(String cacheFrom, String ...more) {
        if (more == null || more.length == 0) {
            return Collections.singletonList(cacheFrom);
        }

        return Stream.concat(Stream.of(cacheFrom), Arrays.stream(more)).collect(Collectors.toList());
    }

    private AssemblyConfiguration extractAssembly(AssemblyConfiguration config, ValueProvider valueProvider) {
        return AssemblyConfiguration.builder()
                .targetDir(valueProvider.getString(ASSEMBLY_BASEDIR, valueOrNull(config, AssemblyConfiguration::getTargetDir)))
                .exportTargetDir(valueProvider.getBoolean(ASSEMBLY_EXPORT_TARGET_DIR, valueOrNull(config, AssemblyConfiguration::getExportTargetDir)))
                .permissionsString(valueProvider.getString(ASSEMBLY_PERMISSIONS, valueOrNull(config, AssemblyConfiguration::getPermissionsRaw)))
                .user(valueProvider.getString(ASSEMBLY_USER, valueOrNull(config, AssemblyConfiguration::getUser)))
                .modeString(valueProvider.getString(ASSEMBLY_MODE, valueOrNull(config, AssemblyConfiguration::getModeRaw)))
                .tarLongFileMode(valueProvider.getString(ASSEMBLY_TARLONGFILEMODE, valueOrNull(config, AssemblyConfiguration::getTarLongFileMode)))
                .build();
    }

    private HealthCheckConfiguration extractHealthCheck(HealthCheckConfiguration config, ValueProvider valueProvider) {
        Map<String, String> healthCheckProperties = valueProvider.getMap(HEALTHCHECK, Collections.emptyMap());
        if (healthCheckProperties != null && healthCheckProperties.size() > 0) {
            return HealthCheckConfiguration.builder()
                    .interval(valueProvider.getString(HEALTHCHECK_INTERVAL, valueOrNull(config, HealthCheckConfiguration::getInterval)))
                    .timeout(valueProvider.getString(HEALTHCHECK_TIMEOUT, valueOrNull(config, HealthCheckConfiguration::getTimeout)))
                    .startPeriod(valueProvider.getString(HEALTHCHECK_START_PERIOD, valueOrNull(config, HealthCheckConfiguration::getStartPeriod)))
                    .retries(valueProvider.getInteger(HEALTHCHECK_RETRIES, valueOrNull(config, HealthCheckConfiguration::getRetries)))
                    .modeString(valueProvider.getString(HEALTHCHECK_MODE, config == null || config.getMode() == null ? null : config.getMode().name()))
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
        return valueProvider.getObject(configKey, alternative, raw -> raw != null ? Arguments.builder().shell(raw).build() : null);
    }

    private WatchImageConfiguration extractWatchConfig(ImageConfiguration fromConfig, ValueProvider valueProvider) {
        WatchImageConfiguration config = fromConfig.getWatchConfiguration();

        return WatchImageConfiguration.builder()
                .interval(valueProvider.getInteger(WATCH_INTERVAL, config == null ? null : config.getIntervalRaw()))
                .postGoal(valueProvider.getString(WATCH_POSTGOAL, config == null ? null : config.getPostGoal()))
                .postExec(valueProvider.getString(WATCH_POSTEXEC, config == null ? null : config.getPostExec()))
                .modeString(valueProvider.getString(WATCH_POSTGOAL, config == null || config.getMode() == null ? null : config.getMode().name()))
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

        // This image has a specified prefix. If multiple images have explicitly set docker. as prefix we
        // assume user know what they are doing and allow it.
        return externalConfig.get("prefix") != null;
    }
}
