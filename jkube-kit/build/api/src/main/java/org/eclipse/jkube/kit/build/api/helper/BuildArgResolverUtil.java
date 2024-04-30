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
package org.eclipse.jkube.kit.build.api.helper;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.eclipse.jkube.kit.common.util.MapUtil.mergeMapsImmutable;

public class BuildArgResolverUtil {
  private static final String ARG_PREFIX = "docker.buildArg.";

  private BuildArgResolverUtil() { }

  /**
   * Merges Docker Build Args in the following order (in decreasing order of precedence):
   * <ul>
   *   <li>Build Args specified directly in ImageConfiguration</li>
   *   <li>Build Args specified via System Properties</li>
   *   <li>Build Args specified via Project Properties</li>
   *   <li>Build Args specified via Plugin configuration</li>
   *   <li>Docker Proxy Build Args detected from ~/.docker/config.json</li>
   * </ul>
   * @param imageConfig ImageConfiguration for which Build Args would be resolved
   * @param configuration {@link JKubeConfiguration} JKubeConfiguration
   * @return a Map containing merged Build Args from all sources.
   */
  public static Map<String, String> mergeBuildArgs(ImageConfiguration imageConfig, JKubeConfiguration configuration) {
    Map<String, String> buildArgsFromProjectProperties = addBuildArgsFromProperties(configuration.getProject().getProperties());
    Map<String, String> buildArgsFromSystemProperties = addBuildArgsFromProperties(System.getProperties());
    Map<String, String> buildArgsFromDockerConfig = addBuildArgsFromDockerConfig();

    return mergeMapsImmutable(imageConfig.getBuild().getArgs(),
        buildArgsFromSystemProperties,
        buildArgsFromProjectProperties,
        Optional.ofNullable(configuration.getBuildArgs()).orElse(Collections.emptyMap()),
        buildArgsFromDockerConfig);
  }

  private static Map<String, String> addBuildArgsFromProperties(Properties properties) {
    Map<String, String> buildArgs = new HashMap<>();
    for (Object keyObj : properties.keySet()) {
      String key = (String) keyObj;
      if (key.startsWith(ARG_PREFIX)) {
        String argKey = key.replaceFirst(ARG_PREFIX, "");
        String value = properties.getProperty(key);

        if (StringUtils.isNotBlank(value)) {
          buildArgs.put(argKey, value);
        }
      }
    }
    return buildArgs;
  }

  private static Map<String, String> addBuildArgsFromDockerConfig() {
    final Map<String, Object> dockerConfig = DockerFileUtil.readDockerConfig();
    if (dockerConfig == null) {
      return Collections.emptyMap();
    }

    // add proxies
    Map<String, String> buildArgs = new HashMap<>();
    if (dockerConfig.containsKey("proxies")) {
      final Map<String, Object> proxies = (Map<String, Object>) dockerConfig.get("proxies");
      if (proxies.containsKey("default")) {
        final Map<String, String> defaultProxyObj = (Map<String, String>) proxies.get("default");
        String[] proxyMapping = new String[] {
            "httpProxy", "http_proxy",
            "httpsProxy", "https_proxy",
            "noProxy", "no_proxy",
            "ftpProxy", "ftp_proxy"
        };

        for(int index = 0; index < proxyMapping.length; index += 2) {
          if (defaultProxyObj.containsKey(proxyMapping[index])) {
            buildArgs.put(ARG_PREFIX + proxyMapping[index+1], defaultProxyObj.get(proxyMapping[index]));
          }
        }
      }
    }
    return buildArgs;
  }
}
