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
package org.eclipse.jkube.kit.build.maven.config;


import org.eclipse.jkube.kit.config.image.build.AssemblyConfiguration;
import org.apache.maven.plugins.assembly.model.Assembly;

/**
 * @author roland
 * @since 19.10.18
 */
public class MavenAssemblyConfiguration extends AssemblyConfiguration {

    /**
     * Assembly defined inline in the pom.xml
     */
    private Assembly inline;

    public Assembly getInline() {
        return inline;
    }

    public static class Builder extends AssemblyConfiguration.TypedBuilder<MavenAssemblyConfiguration> {

        public Builder() {
            super(new MavenAssemblyConfiguration());
        }

        public Builder assemblyDef(Assembly descriptor) {
            ((MavenAssemblyConfiguration)config).inline = set(descriptor);
            return this;
        }
    }
}
