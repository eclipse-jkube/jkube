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
package org.eclipse.jkube.kit.build.api.auth.handler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.build.api.auth.RegistryAuthConfig;
import org.eclipse.jkube.kit.common.KitLogger;
import mockit.Mocked;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;


/**
 * @author roland
 */
public class OpenShiftRegistryAuthHandlerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mocked
    KitLogger log;

    OpenShiftRegistryAuthHandler handler;

    @Before
    public void setup() {
        RegistryAuthConfig registryAuthConfig = RegistryAuthConfig.builder()
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

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->  handler.create(RegistryAuthConfig.Kind.PUSH, "roland", null, s -> s));
            assertThat(exception).hasMessageContaining("~/.kube/config");
        });

    }


    private void executeWithTempHomeDir(Consumer<File> executor) throws IOException {
        String userHome = System.getProperty("user.home");
        try {
            File tempDir = temporaryFolder.newFolder("d-m-p");
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
        JsonObject params = new Gson().fromJson(new String(Base64.getDecoder().decode(config.toHeaderValue(log).getBytes())), JsonObject.class);
        assertEquals(username,params.get("username").getAsString());
        assertEquals(password,params.get("password").getAsString());
        if (email != null) {
            assertEquals(email, params.get("email").getAsString());
        }
    }


}
