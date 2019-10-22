package io.jkube.kit.build.api.auth;

import java.io.IOException;
import java.util.function.Function;

/**
 * @author roland
 * @since 21.10.18
 */
public interface RegistryAuthHandler {

    String getId();

    AuthConfig create(RegistryAuthConfig.Kind kind, String user, String registry, Function<String, String> decryptor);

    interface Extender {
        String getId();
        AuthConfig extend(AuthConfig given, String registry) throws IOException;
    }
}
















