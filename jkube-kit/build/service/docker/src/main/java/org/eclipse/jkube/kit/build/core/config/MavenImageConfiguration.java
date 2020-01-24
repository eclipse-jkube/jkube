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
package org.eclipse.jkube.kit.build.core.config;

import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.apache.commons.lang3.SerializationUtils;

/**
 * @author roland
 * @since 19.10.18
 */
public class MavenImageConfiguration extends ImageConfiguration<MavenBuildConfiguration> {

    public static class Builder
            extends ImageConfiguration.TypedBuilder<MavenBuildConfiguration, MavenImageConfiguration> {

        public Builder() {
            this(null);
        }

        public Builder(MavenImageConfiguration that) {
            super(that == null ? new MavenImageConfiguration() : SerializationUtils.clone(that));
        }

    }

}
