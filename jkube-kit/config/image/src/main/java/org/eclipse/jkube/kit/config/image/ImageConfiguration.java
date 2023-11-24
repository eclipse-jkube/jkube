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
package org.eclipse.jkube.kit.config.image;

import java.io.Serializable;
import java.util.Map;

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

    public BuildConfiguration getBuildConfiguration() {
        return build;
    }

    public WatchImageConfiguration getWatchConfiguration() {
        return watch;
    }

    public Map<String, String> getExternalConfig() {
        return external;
    }

    public String getDescription() {
        return String.format("[%s] %s", new ImageName(name).getFullName(), (alias != null ? "\"" + alias + "\"" : "")).trim();
    }


}
