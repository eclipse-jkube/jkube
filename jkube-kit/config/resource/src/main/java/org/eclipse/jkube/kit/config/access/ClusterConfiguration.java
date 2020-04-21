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
package org.eclipse.jkube.kit.config.access;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.eclipse.jkube.kit.common.util.KubernetesHelper;
import org.apache.commons.lang3.StringUtils;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class ClusterConfiguration {

  private static final String PROPERTY_PREFIX = "jkube.";

  private String username;
  private String password;
  private String masterUrl;
  private String apiVersion;
  private String namespace;
  private String caCertFile;
  private String caCertData;
  private String clientCertFile;
  private String clientCertData;
  private String clientKeyFile;
  private String clientKeyData;
  private String clientKeyAlgo;
  private String clientKeyPassphrase;
  private String trustStoreFile;
  private String trustStorePassphrase;
  private String keyStoreFile;
  private String keyStorePassphrase;

  public String getNamespace() {
    return Optional.ofNullable(namespace).orElse(KubernetesHelper.getDefaultNamespace());
  }

  public Config getConfig() {
    final ConfigBuilder configBuilder = new ConfigBuilder();

    if (StringUtils.isNotBlank(this.username)) {
      configBuilder.withUsername(this.username);
    }

    if (StringUtils.isNotBlank(this.password)) {
      configBuilder.withPassword(this.password);
    }

    if (StringUtils.isNotBlank(this.masterUrl)) {
      configBuilder.withMasterUrl(this.masterUrl);
    }

    if (StringUtils.isNotBlank(this.apiVersion)) {
      configBuilder.withApiVersion(this.apiVersion);
    }

    if (StringUtils.isNotBlank(this.caCertData)) {
      configBuilder.withCaCertData(this.caCertData);
    }

    if (StringUtils.isNotBlank(this.caCertFile)) {
      configBuilder.withCaCertFile(this.caCertFile);
    }

    if (StringUtils.isNotBlank(this.clientCertData)) {
      configBuilder.withClientCertData(this.clientCertData);
    }

    if (StringUtils.isNotBlank(this.clientCertFile)) {
      configBuilder.withClientCertFile(this.clientCertFile);
    }

    if (StringUtils.isNotBlank(this.clientKeyAlgo)) {
      configBuilder.withClientKeyAlgo(this.clientKeyAlgo);
    }

    if (StringUtils.isNotBlank(this.clientKeyData)) {
      configBuilder.withClientKeyData(this.clientKeyData);
    }

    if (StringUtils.isNotBlank(this.clientKeyFile)) {
      configBuilder.withClientKeyFile(this.clientKeyFile);
    }

    if (StringUtils.isNotBlank(this.clientKeyPassphrase)) {
      configBuilder.withClientKeyPassphrase(this.clientKeyPassphrase);
    }

    if (StringUtils.isNotBlank(this.keyStoreFile)) {
      configBuilder.withKeyStoreFile(this.keyStoreFile);
    }

    if (StringUtils.isNotBlank(this.keyStorePassphrase)) {
      configBuilder.withKeyStorePassphrase(this.keyStorePassphrase);
    }

    if (StringUtils.isNotBlank(namespace)) {
      configBuilder.withNamespace(getNamespace());
    }

    if (StringUtils.isNotBlank(this.trustStoreFile)) {
      configBuilder.withTrustStoreFile(this.trustStoreFile);
    }

    if (StringUtils.isNotBlank(this.trustStorePassphrase)) {
      configBuilder.withTrustStorePassphrase(this.trustStorePassphrase);
    }

    return configBuilder.build();

  }

  public static ClusterConfigurationBuilder from(Properties... properties) {
    return from(new ClusterConfiguration(), properties);
  }

  public static ClusterConfigurationBuilder from(ClusterConfiguration clusterConfiguration, Properties... properties) {
    final ClusterConfiguration c = Optional.ofNullable(clusterConfiguration).orElse(new ClusterConfiguration());
    final Properties mergedProperties = Stream.of(properties).collect(Properties::new, Map::putAll, Map::putAll);
    Field[] fields = ClusterConfiguration.class.getDeclaredFields();
    Stream.of(fields)
        .filter(f -> mergedProperties.containsKey(PROPERTY_PREFIX.concat(f.getName())))
        .forEach(f -> {
          f.setAccessible(true);
          try {
            f.set(c, mergedProperties.get(PROPERTY_PREFIX.concat(f.getName())).toString());
          } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
          }
        });
    return c.toBuilder();
  }
}

