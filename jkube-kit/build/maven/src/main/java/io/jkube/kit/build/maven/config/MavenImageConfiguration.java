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
package io.jkube.kit.build.maven.config;

import io.jkube.kit.config.image.ImageConfiguration;
import org.apache.commons.lang3.SerializationUtils;

/**
 * @author roland
 * @since 19.10.18
 */
public class MavenImageConfiguration extends ImageConfiguration {

    private MavenBuildConfiguration build;

    @Override
    public MavenBuildConfiguration getBuildConfiguration() {
        return build;
    }

    public static class Builder extends  ImageConfiguration.Builder {

        private MavenImageConfiguration mavenConfig;

        public Builder() {
            this(null);
        }

        public Builder buildConfig(MavenBuildConfiguration build) {
            mavenConfig.build = build;
            return this;
        }

        public Builder(MavenImageConfiguration that) {
            if (that == null) {
                this.mavenConfig = new MavenImageConfiguration();
                this.config = mavenConfig;
            } else {
                this.config = SerializationUtils.clone(that);
            }
        }

        @Override
        public MavenImageConfiguration build() {
            return mavenConfig;
        }
    }
}
