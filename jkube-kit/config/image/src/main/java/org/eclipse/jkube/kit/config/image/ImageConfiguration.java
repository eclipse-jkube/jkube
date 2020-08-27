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
package org.eclipse.jkube.kit.config.image;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("JavaDoc")
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class ImageConfiguration implements Serializable {
    /**
     * Change the name which can be useful in long running runs e.g. for updating
     * images when doing updates. Use with caution and only for those circumstances.
     *
     * @param name image name to set.
     */
    private String name;
    private String alias;
    private RunImageConfiguration run;
    private BuildConfiguration build;
    private WatchImageConfiguration watch;
    /**
     * Override externalConfiguration when defined via special property.
     *
     * @param external Map with alternative config
     */
    private Map<String,String> external;
    private String registry;

    /**
     * Override externalConfiguration when defined via special property.
     *
     * @param externalConfiguration Map with alternative config
     */
    public void setExternalConfiguration(Map<String, String> externalConfiguration) {
        this.external = externalConfiguration;
    }

    public RunImageConfiguration getRunConfiguration() {
        return (run == null) ? RunImageConfiguration.DEFAULT : run;
    }

    public BuildConfiguration getBuildConfiguration() {
        return build;
    }

    public WatchImageConfiguration getWatchConfiguration() {
        return watch;
    }

    public Map<String, String> getExternalConfig() {
        return external;
    }

    public List<String> getDependencies() {
        final RunImageConfiguration runConfig = getRunConfiguration();
        final List<String> ret = new ArrayList<>();
        if (runConfig != null) {
            ret.addAll(extractVolumes(runConfig));
            ret.addAll(extractLinks(runConfig));
            getContainerNetwork(runConfig).ifPresent(ret::add);
            ret.addAll(extractDependsOn(runConfig));
        }
        return ret;
    }

    private static List<String> extractVolumes(RunImageConfiguration runConfig) {
        return Optional.ofNullable(runConfig).map(RunImageConfiguration::getVolumeConfiguration)
            .map(RunVolumeConfiguration::getFrom).orElse(Collections.emptyList());
    }

    private static List<String> extractLinks(RunImageConfiguration runConfig) {
        // Custom networks can have circular links, no need to be considered for the starting order.
        if (!runConfig.getNetworkingConfig().isCustomNetwork()) {
            return EnvUtil.splitOnLastColon(runConfig.getLinks()).stream().map(a -> a[0]).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private static Optional<String> getContainerNetwork(RunImageConfiguration runConfig) {
        return Optional.ofNullable(runConfig).map(RunImageConfiguration::getNetworkingConfig).map(NetworkConfig::getContainerAlias);
    }

    private static List<String> extractDependsOn(RunImageConfiguration runConfig) {
        // Only used in custom networks.
        if (runConfig.getNetworkingConfig().isCustomNetwork()) {
            return runConfig.getDependsOn();
        }
        return Collections.emptyList();
    }

    public String getDescription() {
        return String.format("[%s] %s", new ImageName(name).getFullName(), (alias != null ? "\"" + alias + "\"" : "")).trim();
    }


}
