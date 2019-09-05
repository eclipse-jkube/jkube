package io.jshift.kit.build.api.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import io.jshift.kit.common.KitLogger;

/**
 * Factory for creating docker specific authentication configuration
 *
 * @author roland
 * @since 29.07.14
 */
public class RegistryAuthFactory {

    private RegistryAuthConfig registryAuthConfig;
    private String defaultRegistry;

    private KitLogger log;
    private final List<RegistryAuthHandler> registryAuthHandlers = new ArrayList<>();
    private final List<RegistryAuthHandler.Extender> extendedRegistryAuthHandlers = new ArrayList<>();

    private Function<String, String> decryptor;

    private RegistryAuthFactory() { }

    public AuthConfig createAuthConfig(RegistryAuthConfig.Kind kind, String user, String specificRegistry) throws IOException {
        String registry = specificRegistry != null ? specificRegistry : defaultRegistry;
        Optional<AuthConfig> ret = createRegistryAuthFromHandlers(kind, user, registry);

        if (ret.isPresent()) {
            if (registry == null || registryAuthConfig.skipExtendedAuthentication()) {
                return ret.get();
            }
            return extendRegistryAuth(registry, ret.get()).orElse(ret.get());
        }

        log.debug("RegistryAuthFactoryg: no credentials found");
        return AuthConfig.EMPTY_AUTH_CONFIG;
    }

    private Optional<AuthConfig> createRegistryAuthFromHandlers(RegistryAuthConfig.Kind kind, String user, String registry) {
        for (RegistryAuthHandler handler : registryAuthHandlers) {
            AuthConfig ret = handler.create(kind, user, registry, decryptor);
            if (ret != null) {
                return Optional.of(ret);
            }
        }
        return Optional.empty();
    }

    private Optional<AuthConfig> extendRegistryAuth(String registry, AuthConfig ret) throws IOException {
        for (RegistryAuthHandler.Extender extended : extendedRegistryAuthHandlers) {
            AuthConfig extendedRet = extended.extend(ret, registry);
            if (extendedRet != null) {
                return Optional.of(extendedRet);
            }
        }
        return Optional.empty();
    }

    // ==================================================================================================

    public static class Builder {

        private RegistryAuthFactory factory;

        public Builder() {
            factory = new RegistryAuthFactory();
        }

        public Builder addRegistryAuthHandler(RegistryAuthHandler registryAuthHandler) {
            factory.registryAuthHandlers.add(registryAuthHandler);
            return this;
        }

        public Builder addExtendedRegistryAuthHandler(RegistryAuthHandler.Extender extendedRegistryAuthHandler) {
            factory.extendedRegistryAuthHandlers.add(extendedRegistryAuthHandler);
            return this;
        }

        public Builder registryAuthConfig(RegistryAuthConfig registryAuthConfig) {
            factory.registryAuthConfig = registryAuthConfig;
            return this;
        }

        public Builder log(KitLogger log) {
            factory.log = log;
            return this;
        }

        public Builder defaultRegistry(String defaultRegistry) {
            factory.defaultRegistry = defaultRegistry;
            return this;
        }

        public Builder decryptor(Function<String, String> decryptor) {
            factory.decryptor = decryptor;
            return this;
        }
        public RegistryAuthFactory build() {
            return factory;
        }
    }
}
