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
     * Change the name which can be useful in long-running runs e.g. for updating
     * images when doing updates. Use with caution and only for those circumstances.
     *
     * @param name image name to set.
     */
    private String name;
    private String alias;
    private BuildConfiguration build;
    private WatchImageConfiguration watch;
    private String registry;
    /**
     * Prefix to use for property resolution.
     *
     * <p> If not set, properties are resolved from <code>jkube.container-image.xxx</code>.
     * The image name property would be resolved from <code>jkube.container-image.name</code>.
     *
     * <p> Use this to narrow down the properties to use for image configuration resolution.
     * For example, if set with <code>app.images.image-1</code>, properties are resolved from <code>app.images.image-1.xxx</code>.
     * The image name property is then resolved from <code>app.images.image-1.name</code>.
     */
    private String propertyResolverPrefix;

    public BuildConfiguration getBuildConfiguration() {
        return build;
    }

    public WatchImageConfiguration getWatchConfiguration() {
        return watch;
    }

    public String getDescription() {
        return String.format("[%s] %s", new ImageName(name).getFullName(), (alias != null ? "\"" + alias + "\"" : "")).trim();
    }

}
