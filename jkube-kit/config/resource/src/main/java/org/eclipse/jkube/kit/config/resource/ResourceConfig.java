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
package org.eclipse.jkube.kit.config.resource;

import io.fabric8.kubernetes.api.model.extensions.IngressRule;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author roland
 * @since 22/03/16
 */
public class ResourceConfig {

    private Map<String, String> env;

    private MetaDataConfig labels = new MetaDataConfig();

    private MetaDataConfig annotations = new MetaDataConfig();

    private List<VolumeConfig> volumes;

    private List<SecretConfig> secrets;

    private String controllerName;

    private List<ServiceConfig> services;

    private List<String> remotes;

    private ConfigMap configMap;

    private ProbeConfig liveness;

    private ProbeConfig readiness;

    private MetricsConfig metrics;

    // Run container in privileged mode
    private boolean containerPrivileged = false;

    // How images should be pulled (maps to ImagePullPolicy)
    private String imagePullPolicy;

    // Mapping of port to names
    private Map<String, Integer> ports;

    // Number of replicas to create
    private int replicas = 1;

    private String namespace;

    private String serviceAccount;

    private List<String> customResourceDefinitions;

    private List<ServiceAccountConfig> serviceAccounts;

    private List<IngressRule> ingressRules;

    private OpenshiftBuildConfig openshiftBuildConfig;

    public Optional<Map<String, String>> getEnv() {
        return Optional.ofNullable(env);
    }

    public MetaDataConfig getLabels() {
        return labels;
    }

    public MetaDataConfig getAnnotations() {
        return annotations;
    }

    public List<VolumeConfig> getVolumes() {
        return volumes;
    }

    public List<ServiceConfig> getServices() {
        return services;
    }

    public List<SecretConfig> getSecrets() { return secrets; }

    public ProbeConfig getLiveness() {
        return liveness;
    }

    public ProbeConfig getReadiness() {
        return readiness;
    }

    public MetricsConfig getMetrics() {
        return metrics;
    }

    public boolean isContainerPrivileged() {
        return containerPrivileged;
    }

    public String getImagePullPolicy() {
        return imagePullPolicy;
    }

    public String getControllerName() {
        return controllerName;
    }

    public Map<String, Integer> getPorts() {
        return ports;
    }

    public int getReplicas() {
        return replicas;
    }

    public String getServiceAccount() {
        return serviceAccount;
    }

    public List<ServiceAccountConfig> getServiceAccounts() {
        return serviceAccounts;
    }

    public String getNamespace() {
        return namespace;
    }

    public ConfigMap getConfigMap() {
        return configMap;
    }

    public List<String> getRemotes() {
        return remotes;
    }

    public List<String> getCrdContexts() { return customResourceDefinitions; }

    public List<IngressRule> getIngressRules() { return ingressRules; }

    public OpenshiftBuildConfig getOpenshiftBuildConfig() {
        return openshiftBuildConfig;
    }

    // =============================================================================================

    public static class Builder {
        private ResourceConfig config = new ResourceConfig();

        public Builder() { }

        public Builder(ResourceConfig config) {
            if(config != null) {
                this.config.env = config.getEnv().orElse(null);
                this.config.controllerName = config.getControllerName();
                this.config.imagePullPolicy = config.getImagePullPolicy();
                this.config.replicas = config.getReplicas();
                this.config.liveness = config.getLiveness();
                this.config.readiness = config.getReadiness();
                this.config.annotations = config.getAnnotations();
                this.config.serviceAccount = config.getServiceAccount();
                this.config.serviceAccounts = config.getServiceAccounts();
                this.config.configMap = config.getConfigMap();
                this.config.volumes = config.getVolumes();
                this.config.labels = config.getLabels();
                this.config.annotations = config.getAnnotations();
                this.config.secrets = config.getSecrets();
                this.config.services = config.getServices();
                this.config.metrics = config.getMetrics();
                this.config.namespace = config.getNamespace();
                this.config.remotes = config.remotes;
                this.config.ingressRules = config.getIngressRules();
            }
        }

        public Builder env(Map<String, String> env) {
            config.env = env;
            return this;
        }

        public Builder controllerName(String name) {
            config.controllerName = name;
            return this;
        }

        public Builder imagePullPolicy(String policy) {
            config.imagePullPolicy = policy;
            return this;
        }

        public Builder withReplicas(int replicas) {
            config.replicas = replicas;
            return this;
        }

        public Builder volumes(List<VolumeConfig> volumes) {
            config.volumes = volumes;
            return this;
        }

        public Builder withServiceAccount(String serviceAccount) {
            config.serviceAccount = serviceAccount;
            return this;
        }

        public Builder withServiceAccounts(List<ServiceAccountConfig> serviceAccounts) {
            config.serviceAccounts = serviceAccounts;
            return this;
        }

        public Builder withConfigMap(ConfigMap configMap) {
            config.configMap = configMap;
            return this;
        }

        public Builder withLiveness(ProbeConfig liveness) {
            config.liveness = liveness;
            return this;
        }

        public Builder withReadiness(ProbeConfig readiness) {
            config.readiness = readiness;
            return this;
        }

        public Builder withRemotes(List<String> remotes) {
            config.remotes = remotes;
            return this;
        }

        public Builder withNamespace(String s) {
            config.namespace = s;
            return this;
        }

        public Builder withIngressRules(List<IngressRule> ingressRules) {
            config.ingressRules = ingressRules;
            return this;
        }

        public Builder withCustomResourceDefinitions(List<String> customResourceDefinitions) {
            config.customResourceDefinitions = customResourceDefinitions;
            return this;
        }

        public Builder withOpenshiftBuildConfig(OpenshiftBuildConfig openshiftBuildConfig) {
            config.openshiftBuildConfig = openshiftBuildConfig;
            return this;
        }

        public ResourceConfig build() {
            return config;
        }
    }

    // TODO: SCC

    // ===============================
    // TODO:
    // jkube.extended.environment.metadata
    // jkube.envProperties
    // jkube.combineDependencies
    // jkube.combineJson.target
    // jkube.combineJson.project

    // jkube.container.name	 --> alias name ?
    // jkube.replicationController.name

    // jkube.iconRef
    // jkube.iconUrl
    // jkube.iconUrlPrefix
    // jkube.iconUrlPrefix

    // jkube.imagePullPolicySnapshot

    // jkube.includeAllEnvironmentVariables
    // jkube.includeNamespaceEnvVar

    // jkube.namespaceEnvVar

    // jkube.provider
}

