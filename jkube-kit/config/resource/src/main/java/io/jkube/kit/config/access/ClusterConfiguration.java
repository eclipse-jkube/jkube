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
package io.jkube.kit.config.access;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import java.lang.reflect.Field;
import java.util.Properties;

import io.jkube.kit.common.util.KubernetesHelper;
import org.apache.commons.lang3.StringUtils;

public class ClusterConfiguration {

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

    public ClusterConfiguration() {
        this.namespace = KubernetesHelper.getDefaultNamespace();
    }

    public String getNamespace() {
        return namespace;
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

        if (StringUtils.isNotBlank(this.namespace)) {
            configBuilder.withNamespace(this.namespace);
        }

        if (StringUtils.isNotBlank(this.trustStoreFile)) {
            configBuilder.withTrustStoreFile(this.trustStoreFile);
        }

        if (StringUtils.isNotBlank(this.trustStorePassphrase)) {
            configBuilder.withTrustStorePassphrase(this.trustStorePassphrase);
        }

        return configBuilder.build();

    }

    public static class Builder {
        private ClusterConfiguration clusterConfiguration;

        public Builder() {
            this(new ClusterConfiguration());
        }

        public Builder(ClusterConfiguration clusterConfiguration) {
            if(clusterConfiguration != null) {
                this.clusterConfiguration = clusterConfiguration;
            } else {
                this.clusterConfiguration = new ClusterConfiguration();
            }
        }

        public Builder from(Properties properties) {

            Field[] fields = ClusterConfiguration.class.getDeclaredFields();

            for (Field f :fields) {
                final String propertyName = "jkube." + f.getName();
                if (properties.containsKey(propertyName)) {
                    String value = (String) properties.get(propertyName);
                    f.setAccessible(true);
                    try {
                        f.set(this.clusterConfiguration, value);
                    } catch (IllegalAccessException e) {
                        throw new IllegalArgumentException(e);
                    }
                }
            }

            return this;
        }

        public Builder username(String username) {
            this.clusterConfiguration.username = username;
            return this;
        }

        public Builder password(String password) {
            this.clusterConfiguration.password = password;
            return this;
        }

        public Builder masterUrl(String masterUrl) {
            this.clusterConfiguration.masterUrl = masterUrl;
            return this;
        }

        public Builder apiVersion(String apiVersion) {
            this.clusterConfiguration.apiVersion = apiVersion;
            return this;
        }

        public Builder namespace(String ns) {
            if (StringUtils.isBlank(ns)) {
                ns = KubernetesHelper.getDefaultNamespace();
            }

            this.clusterConfiguration.namespace = ns;
            return this;
        }

        public Builder caCertFile(String caCertFile) {
            this.clusterConfiguration.caCertFile = caCertFile;
            return this;
        }

        public Builder caCertData(String caCertData) {
            this.clusterConfiguration.caCertData = caCertData;
            return this;
        }

        public Builder clientCertFile(String clientCertFile) {
            this.clusterConfiguration.clientCertFile = clientCertFile;
            return this;
        }

        public Builder clientCertData(String clientCertData) {
            this.clusterConfiguration.clientCertData = clientCertData;
            return this;
        }

        public Builder clientKeyFile(String clientKeyFile) {
            this.clusterConfiguration.clientKeyFile = clientKeyFile;
            return this;
        }

        public Builder clientKeyData(String clientKeyData) {
            this.clusterConfiguration.clientKeyData = clientKeyData;
            return this;
        }

        public Builder clientKeyAlgo(String clientKeyAlgo) {
            this.clusterConfiguration.clientKeyAlgo = clientKeyAlgo;
            return this;
        }

        public Builder clientKeyPassphrase(String clientKeyPassphrase) {
            this.clusterConfiguration.clientKeyPassphrase = clientKeyPassphrase;
            return this;
        }

        public Builder trustStoreFile(String trustStoreFile) {
            this.clusterConfiguration.trustStoreFile = trustStoreFile;
            return this;
        }

        public Builder trustStorePassphrase(String trustStorePassphrase) {
            this.clusterConfiguration.trustStorePassphrase = trustStorePassphrase;
            return this;
        }

        public Builder keyStoreFile(String keyStoreFile) {
            this.clusterConfiguration.keyStoreFile = keyStoreFile;
            return this;
        }

        public Builder keyStorePassphrase(String keyStorePassphrase) {
            this.clusterConfiguration.keyStorePassphrase = keyStorePassphrase;
            return this;
        }

        public ClusterConfiguration build(){
            return this.clusterConfiguration;
        }
    }
}

