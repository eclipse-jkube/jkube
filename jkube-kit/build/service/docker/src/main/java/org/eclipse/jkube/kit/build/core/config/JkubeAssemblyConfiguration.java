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


import org.eclipse.jkube.kit.common.JkubeProjectAssembly;
import org.eclipse.jkube.kit.config.image.build.AssemblyConfiguration;

/**
 * @author roland
 * @since 19.10.18
 */
public class JkubeAssemblyConfiguration extends AssemblyConfiguration {

    /**
     * Assembly defined inline in the pom.xml
     */
    private JkubeProjectAssembly inline;

    public void setInline(JkubeProjectAssembly inline) {
        this.inline = inline;
    }

    public JkubeProjectAssembly getInline() {
        return inline;
    }

    public static class Builder extends AssemblyConfiguration.TypedBuilder<JkubeAssemblyConfiguration> {

        public Builder() {
            super(new JkubeAssemblyConfiguration());
        }

        public Builder assemblyDef(JkubeProjectAssembly descriptor) {
            ((JkubeAssemblyConfiguration)config).inline = set(descriptor);
            return this;
        }
    }
}
