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
package org.eclipse.jkube.kit.common.util;

import io.fabric8.kubernetes.client.utils.Utils;
import org.eclipse.jkube.kit.common.JavaProject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;

import static org.eclipse.jkube.kit.common.util.JKubeProjectUtil.getClassLoader;
import static org.eclipse.jkube.kit.common.util.YamlUtil.getPropertiesFromYamlResource;

public class PropertiesUtil {

  private PropertiesUtil() {}

  /**
   * Returns the given properties resource on the project classpath if found or an empty properties object if not
   *
   * @param resource resource url
   * @return properties
   */
  public static Properties getPropertiesFromResource(URL resource) {
    Properties ret = new Properties();
    if (resource != null) {
      try(InputStream stream = resource.openStream()) {
        ret.load(stream);
      } catch (IOException e) {
        throw new IllegalStateException("Error while reading resource from URL " + resource, e);
      }
    }
    return ret;
  }

  public static Properties readProperties(Path properties) {
    final Properties ret = new Properties();
    if (properties != null && properties.toFile().exists() && properties.toFile().isFile()) {
      try (InputStream stream = Files.newInputStream(properties)) {
        ret.load(stream);
      } catch (IOException e) {
        throw new IllegalStateException("Error while reading properties from file " + properties, e);
      }
    }
    return ret;
  }

  /**
   * Return first Non-Null set property from a set of provided properties
   *
   * @param properties {@link Properties} in a given project
   * @param keys an array of property key values to find
   * @return a string which is first non-null value found for the provided list
   */
  public static String getValueFromProperties(Properties properties, String... keys) {
    for (String property : keys) {
      if (properties.containsKey(property)) {
        String value = properties.get(property).toString();
        if (Utils.isNotNullOrEmpty(value)) {
          return value;
        }
      }
    }
    return null;
  }

  /**
   * Converts the provided Properties to a <code>Map&lt;String, String&gt;</code>
   * @param properties to convert to Map
   * @return a Map representation of the provided Properties
   */
  public static Map<String, String> toMap(Properties properties) {
    final Map<String, String> map = new HashMap<>();
    for (Map.Entry<Object, Object> entry : Optional.ofNullable(properties).map(Properties::entrySet)
        .orElse(Collections.emptySet())) {
      map.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
    }
    return map;
  }

  public static Properties createPropertiesFromApplicationConfig(JavaProject project, List<String> appConfigSources) {
    final List<Supplier<Properties>> sources = getPropertySuppliersList(project, appConfigSources);
    for (Supplier<Properties> source : sources) {
      final Properties props = source.get();
      if (!props.isEmpty()) {
        props.putAll(toMap(project.getProperties()));
        return props;
      }
    }
    return project.getProperties();
  }

  private static List<Supplier<Properties>> getPropertySuppliersList(JavaProject javaProject, List<String> propertySources) {
    final URLClassLoader urlClassLoader = getClassLoader(javaProject);
    List<Supplier<Properties>> sources = new ArrayList<>();
    for (String source : propertySources) {
      if (source.endsWith(".properties")) {
        sources.add(() -> getPropertiesFromResource(urlClassLoader.findResource(source)));
      } else {
        sources.add(() -> getPropertiesFromYamlResource(urlClassLoader.findResource(source)));
      }
    }
    return sources;
  }
}
