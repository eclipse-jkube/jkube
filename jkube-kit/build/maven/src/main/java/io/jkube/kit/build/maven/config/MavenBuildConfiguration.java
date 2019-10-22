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

import io.jkube.kit.config.image.build.BuildConfiguration;
import org.apache.commons.lang3.SerializationUtils;

/**
 * @author roland
 * @since 19.10.18
 */
public class MavenBuildConfiguration extends BuildConfiguration {

    private MavenAssemblyConfiguration assembly;

    @Override
    public MavenAssemblyConfiguration getAssemblyConfiguration() {
        return assembly;
    }

    public static class Builder extends BuildConfiguration.Builder {

        private MavenBuildConfiguration mavenConfig;

        public Builder() {
            this(null);
        }

        public Builder(MavenBuildConfiguration that) {
            if (that == null) {
                this.mavenConfig = new MavenBuildConfiguration();
                this.config =  mavenConfig;
            } else {
                this.config = SerializationUtils.clone(that);
            }
        }

        public Builder assembly(MavenAssemblyConfiguration assembly) {
            this.mavenConfig.assembly = assembly;
            return this;
        }

        @Override
        public MavenBuildConfiguration build() {
            return mavenConfig;
        }
    }

}
