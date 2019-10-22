package io.jkube.kit.build.api;

import java.io.IOException;

import io.jkube.kit.build.api.auth.AuthConfig;
import io.jkube.kit.build.api.auth.RegistryAuth;
import io.jkube.kit.build.api.auth.RegistryAuthConfig;
import io.jkube.kit.config.image.build.ImagePullPolicy;

/**
 * @author roland
 * @since 17.10.18
 */
public interface RegistryContext {

    ImagePullPolicy getDefaultImagePullPolicy();

    String getRegistry(RegistryAuthConfig.Kind kind);

    AuthConfig getAuthConfig(RegistryAuthConfig.Kind kind, String user, String registry) throws IOException;

}
