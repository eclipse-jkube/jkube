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
package org.eclipse.jkube.kit.build.maven;

import java.io.IOException;

import org.eclipse.jkube.kit.build.api.RegistryContext;
import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.build.api.auth.RegistryAuthConfig;
import org.eclipse.jkube.kit.build.api.auth.RegistryAuthFactory;
import org.eclipse.jkube.kit.config.image.build.ImagePullPolicy;


/**
 * @author roland
 * @since 17.10.18
 */
public class MavenRegistryContext implements RegistryContext {

    private ImagePullPolicy defaultImagePullPolicy;
    private RegistryAuthFactory registryAuthFactory;
    private String pushRegistry;
    private String pullRegistry;

    @Override
    public ImagePullPolicy getDefaultImagePullPolicy() {
        return defaultImagePullPolicy;
    }

    @Override
    public AuthConfig getAuthConfig(RegistryAuthConfig.Kind kind, String user, String registry) throws IOException {
        return registryAuthFactory.createAuthConfig(kind, user, registry);
    }

    @Override
    public String getRegistry(RegistryAuthConfig.Kind kind) {
        return kind == RegistryAuthConfig.Kind.PULL ? pullRegistry : pushRegistry;
    }

    // ===============================================================================================

    public static class Builder {

        private MavenRegistryContext context = new MavenRegistryContext();

        public Builder defaultImagePullPolicy(ImagePullPolicy defaultImagePullPolicy) {
            context.defaultImagePullPolicy = defaultImagePullPolicy;
            return this;
        }

        public Builder pushRegistry(String pushRegistry) {
            context.pushRegistry = pushRegistry;
            return this;
        }

        public Builder pullRegistry(String pullRegistry) {
            context.pullRegistry = pullRegistry;
            return this;
        }

        public Builder authRegistryAuthFactory(RegistryAuthFactory registryAuthFactory) {
            context.registryAuthFactory = registryAuthFactory;
            return this;
        }

        // ================================================================================
        public MavenRegistryContext build() {
            return context;
        }

    }
}
