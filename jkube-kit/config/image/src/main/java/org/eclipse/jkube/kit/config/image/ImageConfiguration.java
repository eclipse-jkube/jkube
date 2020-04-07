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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

/**
 * @author roland
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class ImageConfiguration implements Serializable {

    private String name;

    private String alias;

    private String registry;

    private BuildConfiguration build;

    public BuildConfiguration getBuildConfiguration() {
        return build;
    }

    public String getDescription() {
        return String.format("[%s] %s", new ImageName(name).getFullName(), (alias != null ? "\"" + alias + "\"" : "")).trim();
    }

}
