package io.jshift.kit.build.api;

import java.io.IOException;

import io.jshift.kit.build.api.auth.AuthConfig;
import io.jshift.kit.build.api.auth.RegistryAuth;
import io.jshift.kit.build.api.auth.RegistryAuthConfig;
import io.jshift.kit.config.image.build.ImagePullPolicy;

/**
 * @author roland
 * @since 17.10.18
 */
public interface RegistryContext {

    ImagePullPolicy getDefaultImagePullPolicy();

    String getRegistry(RegistryAuthConfig.Kind kind);

    AuthConfig getAuthConfig(RegistryAuthConfig.Kind kind, String user, String registry) throws IOException;

}
