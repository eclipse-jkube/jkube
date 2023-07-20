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
package org.eclipse.jkube.kit.enricher.api;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.openshift.api.model.DeploymentConfig;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.Configs;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.PrefixedLogger;
import org.eclipse.jkube.kit.config.resource.ControllerResourceConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.enricher.api.model.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * @author roland
 */
public class BaseEnricher implements Enricher {
    public static final String FABRIC8_GENERATED_CONTAINERS = "FABRIC8_GENERATED_CONTAINERS";
    public static final String NEED_IMAGECHANGE_TRIGGERS = "IMAGECHANGE_TRIGGER";
    public static final String IMAGE_CHANGE_TRIGGERS = "jkube.openshift.imageChangeTriggers";
    public static final String OPENSHIFT_TRIM_IMAGE_IN_CONTAINER_SPEC = "jkube.openshift.trimImageInContainerSpec";
    public static final String OPENSHIFT_ENABLE_AUTOMATIC_TRIGGER = "jkube.openshift.enableAutomaticTrigger";
    public static final String SIDECAR = "jkube.sidecar";
    public static final String ENRICH_ALL_WITH_IMAGE_TRIGGERS = "jkube.openshift.enrichAllWithImageChangeTrigger";
    public static final String OPENSHIFT_DEPLOY_TIMEOUT_SECONDS = "jkube.openshift.deployTimeoutSeconds";
    private static final String SWITCH_TO_DEPLOYMENT = "jkube.build.switchToDeployment";
    public static final String CREATE_EXTERNAL_URLS = "jkube.createExternalUrls";
    public static final String JKUBE_DOMAIN = "jkube.domain";
    public static final String JKUBE_ENFORCED_REPLICAS = "jkube.replicas";
    public static final String JKUBE_DEFAULT_IMAGE_PULL_POLICY = "IfNotPresent";
    public static final String JKUBE_ENFORCED_IMAGE_PULL_POLICY = "jkube.imagePullPolicy";
    private static final String JKUBE_USE_OLD_PREFIX = "jkube.useLegacyJKubePrefix";

    private final EnricherConfig config;
    private final String name;
    protected EnricherContext enricherContext;

    protected KitLogger log;

    public BaseEnricher(EnricherContext enricherContext, String name) {
        this.enricherContext = enricherContext;
        // Pick the configuration which is for us
        this.config = new EnricherConfig(name, enricherContext);
        this.log = new PrefixedLogger(name, enricherContext.getLog());
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void enrich(PlatformMode platformMode, KubernetesListBuilder builder) { }

    @Override
    public void create(PlatformMode platformMode, KubernetesListBuilder builder) { }


    @Override
    public EnricherContext getContext() {
        return enricherContext;
    }

    protected KitLogger getLog() {
        return log;
    }

    protected List<ImageConfiguration> getImages() {
        return Optional.ofNullable(enricherContext.getConfiguration().getImages()).orElse(Collections.emptyList());
    }

    protected boolean hasImageConfiguration() {
        return !enricherContext.getConfiguration().getImages().isEmpty();
    }

    protected Configuration getConfiguration() {
        return enricherContext.getConfiguration();
    }

    protected String getConfig(Configs.Config key) {
        return config.get(key);
    }

    protected String getConfig(Configs.Config key, String defaultVal) {
        return config.get(key, defaultVal);
    }

    protected String getConfigWithFallback(Configs.Config key, String fallbackPropertyKey, String defaultVal) {
        final String value = getConfig(key, Configs.getFromSystemPropertyWithPropertiesAsFallback(enricherContext.getProperties(), fallbackPropertyKey));
        if (value != null) {
            return value;
        }
        return defaultVal;
    }


    /**
     * Returns true if we are in OpenShift S2I binary building mode
     *
     * @return boolean value indicating whether OpenShift or not.
     */
    protected boolean isOpenShiftMode() {
        Properties properties = getContext().getProperties();
        if (properties != null) {
            return RuntimeMode.isOpenShiftMode(properties);
        }
        return false;
    }

    protected List<String> getProcessingInstructionViaKey(String key) {
        List<String> containers = new ArrayList<>();
        if(enricherContext.getProcessingInstructions() != null
            && enricherContext.getProcessingInstructions().get(key) != null) {
            containers.addAll(Arrays.asList(enricherContext.getProcessingInstructions().get(key).split(",")));
        }
        return containers;
    }

    protected Long getOpenshiftDeployTimeoutInSeconds(Long defaultValue) {
        return Long.parseLong(getValueFromConfig(OPENSHIFT_DEPLOY_TIMEOUT_SECONDS, defaultValue.toString()));
    }

    /**
     * This method overrides the controller name value by the value provided in XML config.
     *
     * @param defaultValue default value
     * @return string as controller name
     */
    protected String getControllerName(String defaultValue) {
        return StringUtils.isNotBlank(getControllerResourceConfig().getControllerName()) ?
          getControllerResourceConfig().getControllerName() : defaultValue;
    }

    protected ControllerResourceConfig getControllerResourceConfig() {
        ResourceConfig resourceConfig = getConfiguration().getResource();
        if (resourceConfig != null) {
            if (resourceConfig.isAnyControllerLegacyConfigFieldSet()) {
                log.debug("Controller configuration fields in resource are deprecated." +
                    " Please use nested field controller for specifying controller configuration");
            }
            return resourceConfig.getController();
        }
        return ControllerResourceConfig.builder().build();
    }

    /**
     * This method overrides the ImagePullPolicy value by the value provided in
     * XML config.
     *
     * @param enricherConfig Enricher specific configuration for ImagePullPolicy
     * @return string as image pull policy
     */
    protected String getImagePullPolicy(Configs.Config enricherConfig) {
        String imagePullPolicyFromProperty = getValueFromConfig(JKUBE_ENFORCED_IMAGE_PULL_POLICY, null);
        if (StringUtils.isNotBlank(imagePullPolicyFromProperty)) {
            return imagePullPolicyFromProperty;
        }
        if (StringUtils.isNotBlank(getControllerResourceConfig().getImagePullPolicy())) {
            return getControllerResourceConfig().getImagePullPolicy();
        }
        final String imagePullPolicyFromEnricherConfig = enricherConfig != null ? getConfig(enricherConfig) : null;
        if (StringUtils.isNotBlank(imagePullPolicyFromEnricherConfig)) {
            return imagePullPolicyFromEnricherConfig;
        }
        return JKUBE_DEFAULT_IMAGE_PULL_POLICY;
    }

    protected boolean getCreateExternalUrls() {
        final String propertyValue = getContext().getProperty(CREATE_EXTERNAL_URLS);
        if (StringUtils.isNotBlank(propertyValue)) {
            return Boolean.parseBoolean(propertyValue);
        }
        if (getConfiguration().getResource().getCreateExternalUrls() != null) {
            return getConfiguration().getResource().getCreateExternalUrls();
        }
        return false;
    }

    /**
     * This method just makes sure that the replica count provided in XML config
     * overrides the default option; and resource fragments are always given
     * topmost priority.
     *
     * @param builder kubernetes list builder containing objects
     * @param defaultValue default value
     * @return resolved replica count
     */
    protected int getReplicaCount(KubernetesListBuilder builder, int defaultValue) {
        final List<HasMetadata> items = Optional.ofNullable(builder)
            .map(KubernetesListBuilder::buildItems).orElse(Collections.emptyList());
        for (HasMetadata item : items) {
            if (item instanceof Deployment && ((Deployment)item).getSpec().getReplicas() != null) {
                return ((Deployment)item).getSpec().getReplicas();
            }
            if (item instanceof DeploymentConfig && ((DeploymentConfig)item).getSpec().getReplicas() != null) {
                return ((DeploymentConfig)item).getSpec().getReplicas();
            }
        }
        return getControllerResourceConfig().getReplicas() != null ?
          getControllerResourceConfig().getReplicas() : defaultValue;
    }

    public static String getNamespace(ResourceConfig resourceConfig, String defaultValue) {
        return Optional.ofNullable(resourceConfig).map(ResourceConfig::getNamespace).orElse(defaultValue);
    }

    protected void setProcessingInstruction(String key, List<String> containerNames) {
        Map<String, String> processingInstructionsMap = new HashMap<>();
        if(enricherContext.getProcessingInstructions() != null) {
            processingInstructionsMap.putAll(enricherContext.getProcessingInstructions());
        }
        processingInstructionsMap.put(key, String.join(",", containerNames));
        enricherContext.setProcessingInstructions(processingInstructionsMap);
    }

    protected boolean shouldUseLegacyJKubePrefix() {
        String valueFromProperties = getValueFromConfig(JKUBE_USE_OLD_PREFIX, null);
        if (StringUtils.isNotBlank(valueFromProperties)) {
            return Boolean.parseBoolean(valueFromProperties);
        }
        ResourceConfig resourceConfig = getConfiguration().getResource();
        if (resourceConfig != null) {
            return resourceConfig.isUseLegacyJKubePrefix();
        }
        return false;
    }

    /**
     * Getting a property value from configuration
     *
     * @param propertyName name of property
     * @param defaultValue default value if not defined (true or false)
     * @return property value
     */
    protected boolean getValueFromConfig(String propertyName, boolean defaultValue) {
        return Boolean.parseBoolean(getValueFromConfig(propertyName, Boolean.toString(defaultValue)));
    }

    protected boolean useDeploymentForOpenShift() {
        return getValueFromConfig(SWITCH_TO_DEPLOYMENT, false);
    }

    /**
     * Getting a property value from configuration
     *
     * @param propertyName name of property
     * @param defaultValue default value if not defined
     * @return property value
     */
    protected String getValueFromConfig(String propertyName, String defaultValue) {
        if (getContext().getProperty(propertyName) != null) {
            return getContext().getProperty(propertyName);
        } else {
            return defaultValue;
        }
    }
}
