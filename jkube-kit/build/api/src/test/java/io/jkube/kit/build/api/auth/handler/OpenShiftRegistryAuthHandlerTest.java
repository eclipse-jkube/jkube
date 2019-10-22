package io.jkube.kit.build.api.auth.handler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.jkube.kit.build.api.auth.AuthConfig;
import io.jkube.kit.build.api.auth.RegistryAuth;
import io.jkube.kit.build.api.auth.RegistryAuthConfig;
import io.jkube.kit.common.KitLogger;
import mockit.Mocked;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


/**
 * @author roland
 * @since 23.10.18
 */
public class OpenShiftRegistryAuthHandlerTest {


    @Mocked
    KitLogger log;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    OpenShiftRegistryAuthHandler handler;

    @Before
    public void setup() {
        RegistryAuthConfig registryAuthConfig = new RegistryAuthConfig.Builder()
                .skipExtendedAuthentication(false)
                .propertyPrefix("docker")
                .addHandlerConfig("openshift",
                                  OpenShiftRegistryAuthHandler.AUTH_USE_OPENSHIFT_AUTH,
                                  "true")
                .build();
            handler = new OpenShiftRegistryAuthHandler(registryAuthConfig, log);
    }

    @Test
    public void testOpenShiftConfigFromPluginConfig() throws Exception {

        executeWithTempHomeDir(homeDir -> {
            createOpenShiftConfig(homeDir,"openshift_simple_config.yaml");
            AuthConfig config = handler.create(RegistryAuthConfig.Kind.PUSH, "roland", null, s -> s);
            verifyAuthConfig(config,"admin","token123",null);
        });
    }

    @Test
    public void testOpenShiftConfigFromSystemProps() throws Exception {

        try {
            System.setProperty("docker.useOpenShiftAuth", "true");
            executeWithTempHomeDir(homeDir -> {
                createOpenShiftConfig(homeDir, "openshift_simple_config.yaml");
                AuthConfig config = handler.create(RegistryAuthConfig.Kind.PUSH, "roland", null, s->s);
                verifyAuthConfig(config, "admin", "token123", null);
            });
        } finally {
            System.getProperties().remove("docker.useOpenShiftAuth");
        }
    }

    @Test
    public void testOpenShiftConfigFromSystemPropsNegative() throws Exception {
        try {
            System.setProperty("docker.useOpenShiftAuth", "false");
            executeWithTempHomeDir(homeDir -> {
                createOpenShiftConfig(homeDir, "openshift_simple_config.yaml");
                AuthConfig config = handler.create(RegistryAuthConfig.Kind.PUSH, "roland", null, s->s);
                assertNull(config);
            });
        } finally {
            System.getProperties().remove("docker.useOpenShiftAuth");
        }
    }

    @Test
    public void testOpenShiftConfigNotLoggedIn() throws Exception {
        executeWithTempHomeDir(homeDir -> {
            createOpenShiftConfig(homeDir,"openshift_nologin_config.yaml");
            expectedException.expect(IllegalArgumentException.class);
            expectedException.expectMessage(containsString("~/.kube/config"));
            handler.create(RegistryAuthConfig.Kind.PUSH, "roland", null, s -> s);
        });

    }


    private void executeWithTempHomeDir(Consumer<File> executor) throws IOException {
        String userHome = System.getProperty("user.home");
        try {
            File tempDir = Files.createTempDirectory("d-m-p").toFile();
            System.setProperty("user.home", tempDir.getAbsolutePath());
            executor.accept(tempDir);
        } finally {
            System.setProperty("user.home", userHome);
        }

    }

    private void createOpenShiftConfig(File homeDir, String testConfig)  {
        try {
            File kubeDir = new File(homeDir, ".kube");
            kubeDir.mkdirs();
            File config = new File(kubeDir, "config");
            IOUtils.copy(getClass().getResourceAsStream(testConfig), new FileOutputStream(config));
        } catch (IOException e) {
            throw new RuntimeException(e);
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
