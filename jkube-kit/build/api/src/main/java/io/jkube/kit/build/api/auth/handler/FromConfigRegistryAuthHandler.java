package io.jkube.kit.build.api.auth.handler;

import java.util.function.Function;

import io.jkube.kit.build.api.auth.AuthConfig;
import io.jkube.kit.build.api.auth.RegistryAuth;
import io.jkube.kit.build.api.auth.RegistryAuthConfig;
import io.jkube.kit.build.api.auth.RegistryAuthHandler;
import io.jkube.kit.common.KitLogger;

/**
 * @author roland
 * @since 21.10.18
 */
public class FromConfigRegistryAuthHandler implements RegistryAuthHandler {
    private final RegistryAuthConfig registryAuthConfig;
    private final KitLogger log;

    public FromConfigRegistryAuthHandler(RegistryAuthConfig registryAuthConfig, KitLogger log) {
        this.registryAuthConfig = registryAuthConfig;
        this.log = log;
    }

    @Override
    public String getId() {
        return "config";
    }

    @Override
    public AuthConfig create(RegistryAuthConfig.Kind kind, String user, String registry, Function<String, String> decryptor) {
        // Get configuration from global plugin config

        if (registryAuthConfig.getUsername(kind) != null) {
            if (registryAuthConfig.getPassword(kind) == null) {
                throw new IllegalArgumentException("No 'password' given while using <authConfig> in configuration for mode " + kind);
            }
            log.debug("AuthConfig: credentials from plugin config");
            return AuthConfig.fromRegistryAuthConfig(registryAuthConfig, kind, decryptor);
        }
        return null;
    }

}
