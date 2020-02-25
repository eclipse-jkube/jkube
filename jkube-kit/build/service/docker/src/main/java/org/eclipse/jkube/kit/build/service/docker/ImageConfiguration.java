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
package org.eclipse.jkube.kit.build.service.docker;

import org.eclipse.jkube.kit.build.core.config.JkubeBuildConfiguration;
import org.eclipse.jkube.kit.build.service.docker.config.ConfigHelper;
import org.eclipse.jkube.kit.build.service.docker.config.NetworkConfig;
import org.eclipse.jkube.kit.build.service.docker.config.RunImageConfiguration;
import org.eclipse.jkube.kit.build.service.docker.config.RunVolumeConfiguration;
import org.eclipse.jkube.kit.build.service.docker.config.WatchImageConfiguration;
import org.eclipse.jkube.kit.build.service.docker.helper.DeepCopy;
import org.eclipse.jkube.kit.build.service.docker.helper.StartOrderResolver;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.eclipse.jkube.kit.config.image.ImageName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImageConfiguration implements StartOrderResolver.Resolvable, Serializable {

    private String name;

    private String alias;

    private RunImageConfiguration run;

    private JkubeBuildConfiguration build;

    private WatchImageConfiguration watch;

    private Map<String,String> external;

    private String registry;

    // Used for injection
    public ImageConfiguration() {}

    @Override
    public String getName() {
        return name;
    }

    /**
     * Change the name which can be useful in long running runs e.g. for updating
     * images when doing updates. Use with caution and only for those circumstances.
     *
     * @param name image name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Override externalConfiguration when defined via special property.
     *
     * @param externalConfiguration Map with alternative config
     */
    public void setExternalConfiguration(Map<String, String> externalConfiguration) {
        this.external = externalConfiguration;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    public RunImageConfiguration getRunConfiguration() {
        return (run == null) ? RunImageConfiguration.DEFAULT : run;
    }

    public JkubeBuildConfiguration getBuildConfiguration() {
        return build;
    }

    public WatchImageConfiguration getWatchConfiguration() {
        return watch;
    }

    public Map<String, String> getExternalConfig() {
        return external;
    }

    @Override
    public List<String> getDependencies() {
        RunImageConfiguration runConfig = getRunConfiguration();
        List<String> ret = new ArrayList<>();
        if (runConfig != null) {
            addVolumes(runConfig, ret);
            addLinks(runConfig, ret);
            addContainerNetwork(runConfig, ret);
            addDependsOn(runConfig, ret);
        }
        return ret;
    }

    private void addVolumes(RunImageConfiguration runConfig, List<String> ret) {
        RunVolumeConfiguration volConfig = runConfig.getVolumeConfiguration();
        if (volConfig != null) {
            List<String> volumeImages = volConfig.getFrom();
            if (volumeImages != null) {
                ret.addAll(volumeImages);
            }
        }
    }

    private void addLinks(RunImageConfiguration runConfig, List<String> ret) {
        // Custom networks can have circular links, no need to be considered for the starting order.
        if (!runConfig.getNetworkingConfig().isCustomNetwork()) {
            for (String[] link : EnvUtil.splitOnLastColon(runConfig.getLinks())) {
                ret.add(link[0]);
            }
        }
    }

    private void addContainerNetwork(RunImageConfiguration runConfig, List<String> ret) {
        NetworkConfig config = runConfig.getNetworkingConfig();
        String alias = config.getContainerAlias();
        if (alias != null) {
            ret.add(alias);
        }
    }

    private void addDependsOn(RunImageConfiguration runConfig, List<String> ret) {
        // Only used in custom networks.
        if (runConfig.getNetworkingConfig().isCustomNetwork()) {
            ret.addAll(runConfig.getDependsOn());
        }
    }

    public boolean isDataImage() {
        // If there is no explicit run configuration, its a data image
        // TODO: Probably add an explicit property so that a user can indicated whether it
        // is a data image or not on its own.
        return getRunConfiguration() == null;
    }

    public String getDescription() {
        return String.format("[%s] %s", new ImageName(name).getFullName(), (alias != null ? "\"" + alias + "\"" : "")).trim();
    }

    public String getRegistry() {
        return registry;
    }

    @Override
    public String toString() {
        return String.format("ImageConfiguration {name='%s', alias='%s'}", name, alias);
    }

    public String initAndValidate(ConfigHelper.NameFormatter nameFormatter, KitLogger log) {
        name = nameFormatter.format(name);
        String minimalApiVersion = null;
        if (build != null) {
            minimalApiVersion = build.initAndValidate(log);
        }
        if (run != null) {
            minimalApiVersion = EnvUtil.extractLargerVersion(minimalApiVersion, run.initAndValidate());
        }
        return minimalApiVersion;
    }

    // =========================================================================
    // Builder for image configurations

    public static class Builder {
        private final ImageConfiguration config;

        public Builder()  {
            this(null);
        }


        public Builder(ImageConfiguration that) {
            if (that == null) {
                this.config = new ImageConfiguration();
            } else {
                this.config = DeepCopy.copy(that);
            }
        }

        public Builder name(String name) {
            config.name = name;
            return this;
        }

        public Builder alias(String alias) {
            config.alias = alias;
            return this;
        }

        public Builder runConfig(RunImageConfiguration runConfig) {
            config.run = runConfig;
            return this;
        }

        public Builder buildConfig(JkubeBuildConfiguration buildConfig) {
            config.build = buildConfig;
            return this;
        }

        public Builder externalConfig(Map<String, String> externalConfig) {
            config.external = externalConfig;
            return this;
        }

        public Builder registry(String registry) {
            config.registry = registry;
            return this;
        }


        public Builder watchConfig(WatchImageConfiguration watchConfig) {
            config.watch = watchConfig;
            return this;
        }

        public ImageConfiguration build() {
            return config;
        }
    }
}
