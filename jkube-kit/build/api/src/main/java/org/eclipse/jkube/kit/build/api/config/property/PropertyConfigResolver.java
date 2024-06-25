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
package org.eclipse.jkube.kit.build.api.config.property;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.WatchImageConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.eclipse.jkube.kit.common.util.JKubeProjectUtil;
import org.eclipse.jkube.kit.common.util.MapUtil;
import org.eclipse.jkube.kit.common.Arguments;
import org.eclipse.jkube.kit.common.AssemblyConfiguration;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.eclipse.jkube.kit.config.image.build.HealthCheckConfiguration;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

public class PropertyConfigResolver {

  private static final String DEFAULT_PREFIX = "jkube.container-image";
  // The ValueProvider class has more functionality than the one used by the PropertyConfigResolver class.
  // Let's add some default values so that we can conserve the ValueProvider as is and still leverage its functionality.
  private static final PropertyMode DEFAULT_MODE = PropertyMode.OVERRIDE;

  public final ImageConfiguration resolve(ImageConfiguration fromConfig, JavaProject project) {
    final Properties properties = JKubeProjectUtil.getPropertiesWithSystemOverrides(project);
    final String prefix = StringUtils.isBlank(fromConfig.getPropertyResolverPrefix()) ?
      DEFAULT_PREFIX : fromConfig.getPropertyResolverPrefix();
    final ValueProvider valueProvider = new ValueProvider(prefix, properties, DEFAULT_MODE);
    return ImageConfiguration.builder()
      .name(valueProvider.getString(ConfigKey.NAME, fromConfig.getName()))
      .alias(valueProvider.getString(ConfigKey.ALIAS, fromConfig.getAlias()))
      .registry(valueProvider.getString(ConfigKey.REGISTRY, fromConfig.getRegistry()))
      .build(extractBuildConfiguration(fromConfig, valueProvider))
      .watch(extractWatchConfig(fromConfig, valueProvider))
      .build();
  }

  private static <T, R> R valueOr(T input, Function<T,R> function, R defaultValue) {
    return Optional.ofNullable(input).map(function).orElse(defaultValue);
  }

  private static <T, R> R valueOrNull(T input, Function<T,R> function) {
    return valueOr(input, function, null);
  }

  private BuildConfiguration extractBuildConfiguration(ImageConfiguration fromConfig, ValueProvider valueProvider) {
    final BuildConfiguration config = fromConfig.getBuild() != null ?
      fromConfig.getBuild() : new BuildConfiguration();
    return config.toBuilder()
      .buildpacksBuilderImage(valueProvider.getString(ConfigKey.BUILDPACKS_BUILDER_IMAGE, valueOrNull(config, BuildConfiguration::getBuildpacksBuilderImage)))
      .cmd(extractArguments(valueProvider, ConfigKey.CMD, valueOrNull(config, BuildConfiguration::getCmd)))
      .cleanup(valueProvider.getString(ConfigKey.CLEANUP, valueOrNull(config, BuildConfiguration::getCleanup)))
      .nocache(valueProvider.getBoolean(ConfigKey.NOCACHE, valueOrNull(config, BuildConfiguration::getNocache)))
      .clearCacheFrom().cacheFrom(valueProvider.getList(ConfigKey.CACHEFROM, valueOr(config, BuildConfiguration::getCacheFrom, Collections.emptyList())))
      .createImageOptions(valueProvider.getMap(ConfigKey.CREATE_IMAGE_OPTIONS, valueOrNull(config, BuildConfiguration::getCreateImageOptions)))
      .optimise(valueProvider.getBoolean(ConfigKey.OPTIMISE, valueOrNull(config, BuildConfiguration::getOptimise)))
      .entryPoint(extractArguments(valueProvider, ConfigKey.ENTRYPOINT, valueOrNull(config, BuildConfiguration::getEntryPoint)))
      .assembly(extractAssembly(valueOrNull(config, BuildConfiguration::getAssembly), valueProvider))
      .env(MapUtil.mergeMaps(
        valueProvider.getMap(ConfigKey.ENV_BUILD, Collections.emptyMap()),
        valueProvider.getMap(ConfigKey.ENV, valueOrNull(config, BuildConfiguration::getEnv))
      ))
      .args(valueProvider.getMap(ConfigKey.ARGS, valueOr(config, BuildConfiguration::getArgs, Collections.emptyMap())))
      .labels(valueProvider.getMap(ConfigKey.LABELS, valueOr(config, BuildConfiguration::getLabels, Collections.emptyMap())))
      .clearPorts().ports(extractPortValues(valueOr(config, BuildConfiguration::getPorts, Collections.emptyList()), valueProvider))
      .shell(extractArguments(valueProvider, ConfigKey.SHELL, valueOrNull(config, BuildConfiguration::getShell)))
      .clearRunCmds().runCmds(valueProvider.getList(ConfigKey.RUN, valueOr(config, BuildConfiguration::getRunCmds, Collections.emptyList())))
      .from(valueProvider.getString(ConfigKey.FROM, valueOrNull(config, BuildConfiguration::getFrom)))
      .fromExt(valueProvider.getMap(ConfigKey.FROM_EXT, valueOrNull(config, BuildConfiguration::getFromExt)))
      .clearVolumes().volumes(valueProvider.getList(ConfigKey.VOLUMES, valueOr(config, BuildConfiguration::getVolumes, Collections.emptyList())))
      .clearTags().tags(valueProvider.getList(ConfigKey.TAGS, valueOr(config, BuildConfiguration::getTags, Collections.emptyList())))
      .clearPlatforms().platforms(valueProvider.getList(ConfigKey.PLATFORMS, valueOr(config, BuildConfiguration::getPlatforms, Collections.emptyList())))
      .maintainer(valueProvider.getString(ConfigKey.MAINTAINER, valueOrNull(config, BuildConfiguration::getMaintainer)))
      .workdir(valueProvider.getString(ConfigKey.WORKDIR, valueOrNull(config, BuildConfiguration::getWorkdir)))
      .skip(valueProvider.getBoolean(ConfigKey.SKIP, valueOrNull(config, BuildConfiguration::getSkip)))
      .imagePullPolicy(valueProvider.getString(ConfigKey.IMAGE_PULL_POLICY, valueOrNull(config, BuildConfiguration::getImagePullPolicy)))
      .contextDir(valueProvider.getString(ConfigKey.CONTEXT_DIR, valueOrNull(config, BuildConfiguration::getContextDirRaw)))
      .dockerArchive(valueProvider.getString(ConfigKey.DOCKER_ARCHIVE, valueOrNull(config, BuildConfiguration::getDockerArchiveRaw)))
      .dockerFile(valueProvider.getString(ConfigKey.DOCKER_FILE, valueOrNull(config, BuildConfiguration::getDockerFileRaw)))
      .buildOptions(valueProvider.getMap(ConfigKey.BUILD_OPTIONS, valueOrNull(config, BuildConfiguration::getBuildOptions)))
      .filter(valueProvider.getString(ConfigKey.FILTER, valueOrNull(config, BuildConfiguration::getFilter)))
      .user(valueProvider.getString(ConfigKey.USER, valueOrNull(config, BuildConfiguration::getUser)))
      .healthCheck(extractHealthCheck(valueOrNull(config, BuildConfiguration::getHealthCheck), valueProvider))
      .build();
  }

  private AssemblyConfiguration extractAssembly(AssemblyConfiguration originalConfig, ValueProvider valueProvider) {
    final AssemblyConfiguration config = originalConfig != null ? originalConfig : new AssemblyConfiguration();
    return config.toBuilder()
      .name(valueProvider.getString(ConfigKey.ASSEMBLY_NAME, valueOrNull(originalConfig, AssemblyConfiguration::getName)))
      .targetDir(valueProvider.getString(ConfigKey.ASSEMBLY_TARGET_DIR, valueOrNull(originalConfig, AssemblyConfiguration::getTargetDir)))
      .exportTargetDir(valueProvider.getBoolean(ConfigKey.ASSEMBLY_EXPORT_TARGET_DIR, valueOrNull(originalConfig, AssemblyConfiguration::getExportTargetDir)))
      .permissionsString(valueProvider.getString(ConfigKey.ASSEMBLY_PERMISSIONS, valueOrNull(originalConfig, AssemblyConfiguration::getPermissionsRaw)))
      .user(valueProvider.getString(ConfigKey.ASSEMBLY_USER, valueOrNull(originalConfig, AssemblyConfiguration::getUser)))
      .modeString(valueProvider.getString(ConfigKey.ASSEMBLY_MODE, valueOrNull(originalConfig, AssemblyConfiguration::getModeRaw)))
      .tarLongFileMode(valueProvider.getString(ConfigKey.ASSEMBLY_TARLONGFILEMODE, valueOrNull(originalConfig, AssemblyConfiguration::getTarLongFileMode)))
      .excludeFinalOutputArtifact(valueProvider.getBoolean(ConfigKey.ASSEMBLY_EXCLUDE_FINAL_OUTPUT_ARTIFACT, Optional.ofNullable(valueOrNull(originalConfig, AssemblyConfiguration::isExcludeFinalOutputArtifact)).orElse(false)))
      .build();
  }

  private HealthCheckConfiguration extractHealthCheck(HealthCheckConfiguration originalConfig, ValueProvider valueProvider) {
    final Map<String, String> healthCheckProperties = valueProvider.getMap(ConfigKey.HEALTHCHECK, Collections.emptyMap());
    if (healthCheckProperties != null && !healthCheckProperties.isEmpty()) {
      final HealthCheckConfiguration config = originalConfig != null ? originalConfig : new HealthCheckConfiguration();
      return config.toBuilder()
        .interval(valueProvider.getString(ConfigKey.HEALTHCHECK_INTERVAL, valueOrNull(originalConfig, HealthCheckConfiguration::getInterval)))
        .timeout(valueProvider.getString(ConfigKey.HEALTHCHECK_TIMEOUT, valueOrNull(originalConfig, HealthCheckConfiguration::getTimeout)))
        .startPeriod(valueProvider.getString(ConfigKey.HEALTHCHECK_START_PERIOD, valueOrNull(originalConfig, HealthCheckConfiguration::getStartPeriod)))
        .retries(valueProvider.getInteger(ConfigKey.HEALTHCHECK_RETRIES, valueOrNull(originalConfig, HealthCheckConfiguration::getRetries)))
        .modeString(valueProvider.getString(ConfigKey.HEALTHCHECK_MODE, originalConfig == null || originalConfig.getMode() == null ? null : originalConfig.getMode().name()))
        .cmd(extractArguments(valueProvider, ConfigKey.HEALTHCHECK_CMD, valueOrNull(originalConfig, HealthCheckConfiguration::getCmd)))
        .build();
    } else {
      return originalConfig;
    }
  }

  // Extract only the values of the port mapping

  private Set<String> extractPortValues(List<String> config, ValueProvider valueProvider) {
    final Set<String> ret = new LinkedHashSet<>();
    final List<String> ports = valueProvider.getList(ConfigKey.PORTS, config);
    if (ports == null) {
      return null;
    }
    final List<String[]> parsedPorts = EnvUtil.splitOnLastColon(ports);
    for (String[] port : parsedPorts) {
      ret.add(port[1]);
    }
    return ret;
  }

  private Arguments extractArguments(ValueProvider valueProvider, ConfigKey configKey, Arguments alternative) {
    return valueProvider.getObject(configKey, alternative, raw -> raw != null ? Arguments.builder().shell(raw).build() : null);
  }

  private WatchImageConfiguration extractWatchConfig(ImageConfiguration fromConfig, ValueProvider valueProvider) {
    final WatchImageConfiguration config = fromConfig.getWatchConfiguration() != null ?
      fromConfig.getWatchConfiguration() : new WatchImageConfiguration();
    return config.toBuilder()
      .interval(valueProvider.getInteger(ConfigKey.WATCH_INTERVAL, config.getIntervalRaw()))
      .postGoal(valueProvider.getString(ConfigKey.WATCH_POSTGOAL, config.getPostGoal()))
      .postExec(valueProvider.getString(ConfigKey.WATCH_POSTEXEC, config.getPostExec()))
      .modeString(valueProvider.getString(ConfigKey.WATCH_MODE, config.getMode() == null ? null : config.getMode().name()))
      .build();
  }

}
