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

import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;
import org.apache.commons.lang3.SerializationUtils;

/**
 * @author roland
 * @since 19.10.18
 */
public class JKubeBuildConfiguration extends BuildConfiguration<JKubeAssemblyConfiguration> {

    /**
     * Explicit typed setter is required by Plexus @Parameter injection in order to compute the target value
     * object type when reading xml tags
     *
     * i.e. The following xml wil be converted to a MavenAssemblyConfiguration:
     * <pre>{@code
     *   <assembly>
     *     <mode>dir</mode>
     *     <!-- ... -->
     *   </assembly>
     * }</pre>
     * @param assembly to be set
     */
    @Override
    public void setAssembly(JKubeAssemblyConfiguration assembly) {
        super.setAssembly(assembly);
    }

    public static class Builder
            extends BuildConfiguration.TypedBuilder<JKubeAssemblyConfiguration, JKubeBuildConfiguration> {

        public Builder() {
            this(null);
        }

        public Builder(JKubeBuildConfiguration that) {
            super(that == null ? new JKubeBuildConfiguration() : SerializationUtils.clone(that));
        }

    }

}
