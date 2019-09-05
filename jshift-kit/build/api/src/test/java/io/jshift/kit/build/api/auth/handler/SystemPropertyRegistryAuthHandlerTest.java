package io.jshift.kit.build.api.auth.handler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.jshift.kit.build.api.auth.AuthConfig;
import io.jshift.kit.build.api.auth.RegistryAuth;
import io.jshift.kit.build.api.auth.RegistryAuthConfig;
import io.jshift.kit.common.KitLogger;
import mockit.Mocked;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

/**
 * @author roland
 * @since 23.10.18
 */
public class SystemPropertyRegistryAuthHandlerTest {

    @Mocked
    KitLogger log;

    SystemPropertyRegistryAuthHandler handler;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() {
        RegistryAuthConfig registryAuthConfig = new RegistryAuthConfig.Builder()
                .skipExtendedAuthentication(false)
                .propertyPrefix("docker")
                .build();
        handler = new SystemPropertyRegistryAuthHandler(registryAuthConfig, log);
    }


    @Test
    public void testEmpty() throws Exception {
        String userHome = System.getProperty("user.home");
        try {
            File tempDir = Files.createTempDirectory("d-m-p").toFile();
            System.setProperty("user.home", tempDir.getAbsolutePath());
            assertEquals(handler.create(RegistryAuthConfig.Kind.PUSH, null, "blubberbla:1611", s->s), null);
        } finally {
            System.setProperty("user.home", userHome);
        }
    }


    @Test
    public void testSystemProperty() throws Exception {
        System.setProperty("docker.push.username", "roland");
        System.setProperty("docker.push.password", "secret");
        System.setProperty("docker.push.email", "roland@jolokia.org");
        try {
            AuthConfig config = handler.create(RegistryAuthConfig.Kind.PUSH, null, null, s -> s);
            verifyAuthConfig(config,"roland","secret","roland@jolokia.org");
        } finally {
            System.clearProperty("docker.push.username");
            System.clearProperty("docker.push.password");
            System.clearProperty("docker.push.email");
        }
    }


    @Test
    public void testSystemPropertyNoPassword() throws IOException {
        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("No password provided for username secret");
        checkException("docker.username");
    }

    private void checkException(String key) throws IOException {
        System.setProperty(key, "secret");
        try {
            handler.create(RegistryAuthConfig.Kind.PUSH, null, null, s -> s);
        } finally {
            System.clearProperty(key);
        }
    }

    private void verifyAuthConfig(AuthConfig config, String username, String password, String email) {
        JsonObject params = new Gson().fromJson(new String(Base64.getDecoder().decode(config.toHeaderValue().getBytes())), JsonObject.class);
        assertEquals(username,params.get("username").getAsString());
        assertEquals(password,params.get("password").getAsString());
        if (email != null) {
            assertEquals(email, params.get("email").getAsString());
        }
    }

}
