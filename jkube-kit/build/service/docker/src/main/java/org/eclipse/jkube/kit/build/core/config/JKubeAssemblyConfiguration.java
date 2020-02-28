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


import org.eclipse.jkube.kit.common.JKubeProjectAssembly;
import org.eclipse.jkube.kit.config.image.build.AssemblyConfiguration;

/**
 * @author roland
 * @since 19.10.18
 */
public class JKubeAssemblyConfiguration extends AssemblyConfiguration {

    /**
     * Assembly defined inline in the pom.xml
     */
    private JKubeProjectAssembly inline;

    public void setInline(JKubeProjectAssembly inline) {
        this.inline = inline;
    }

    public JKubeProjectAssembly getInline() {
        return inline;
    }

    public static class Builder extends AssemblyConfiguration.TypedBuilder<JKubeAssemblyConfiguration> {

        public Builder() {
            super(new JKubeAssemblyConfiguration());
        }

        public Builder assemblyDef(JKubeProjectAssembly descriptor) {
            ((JKubeAssemblyConfiguration)config).inline = set(descriptor);
            return this;
        }
    }
}
