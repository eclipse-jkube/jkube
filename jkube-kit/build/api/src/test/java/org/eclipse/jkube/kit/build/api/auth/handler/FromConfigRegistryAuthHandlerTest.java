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

import java.io.IOException;
import java.util.Base64;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.eclipse.jkube.kit.build.api.auth.AuthConfig;
import org.eclipse.jkube.kit.build.api.auth.RegistryAuth;
import org.eclipse.jkube.kit.build.api.auth.RegistryAuthConfig;
import org.eclipse.jkube.kit.common.KitLogger;
import mockit.Mocked;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;



import static org.assertj.core.api.Assertions.assertThat;  /*assertj assertThat import*/
import static org.junit.jupiter.api.Assertions.assertThrows;   /*assertThrows import*/

/**
 * @author roland
 * @since 23.10.18
 */
public class FromConfigRegistryAuthHandlerTest {

    @Mocked
    private KitLogger log;

    /*@Rule
    public ExpectedException expectedException = ExpectedException.none();*/      //removed expectedException @Rule

    @Test
    public void testFromPluginConfiguration() throws IOException {
        FromConfigRegistryAuthHandler handler = new FromConfigRegistryAuthHandler(setupAuthConfigFactoryWithConfigData(), log);

        AuthConfig config = handler.create(RegistryAuthConfig.Kind.PUSH, null, null, s -> s);
        verifyAuthConfig(config, "roland", "secret", "roland@jolokia.org");
    }

    protected RegistryAuthConfig setupAuthConfigFactoryWithConfigData() {
        return RegistryAuthConfig.builder()
                .skipExtendedAuthentication(false)
                .putDefaultConfig(RegistryAuth.USERNAME, "roland")
                .putDefaultConfig(RegistryAuth.PASSWORD, "secret")
                .putDefaultConfig(RegistryAuth.EMAIL, "roland@jolokia.org")
                .build();
    }

    private RegistryAuthConfig setupAuthConfigFactoryWithConfigDataForKind(RegistryAuthConfig.Kind kind) {
        return RegistryAuthConfig.builder()
                .skipExtendedAuthentication(false)
                .addKindConfig(kind, RegistryAuth.USERNAME, "roland")
                .addKindConfig(kind, RegistryAuth.PASSWORD, "secret")
                .addKindConfig(kind, RegistryAuth.EMAIL, "roland@jolokia.org")
                .build();
    }

    @Test
    public void testFromPluginConfigurationPull() {
        FromConfigRegistryAuthHandler handler = new FromConfigRegistryAuthHandler(setupAuthConfigFactoryWithConfigDataForKind(RegistryAuthConfig.Kind.PULL), log);

        AuthConfig config = handler.create(RegistryAuthConfig.Kind.PULL, null, null, s -> s);
        verifyAuthConfig(config, "roland", "secret", "roland@jolokia.org");
    }


    @Test
    public void testFromPluginConfigurationFailed() {
        FromConfigRegistryAuthHandler handler = new FromConfigRegistryAuthHandler(
            RegistryAuthConfig.builder().putDefaultConfig(RegistryAuth.USERNAME, "admin").build(), log);

        //expectedException.expect(IllegalArgumentException.class);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,() -> throws IllegalArgumentException("bad password"),"not thrown bad password exception");
        
        //() -> throw IllegalArgumentException("Throw an Illegal Argument Exception"));          
        //expectedexception implementation substituted with assertThrows #task1
      
        
        //expectedException.expectMessage(containsString("password"));
        assertThat(exception.getMessage(),containsString("password"));    //assertThat from assertJ used for in place of containsString of hamcrest #task2
       
        
        handler.create(RegistryAuthConfig.Kind.PUSH, null, null, s -> s);
    }

    private void verifyAuthConfig(AuthConfig config, String username, String password, String email) {
        JsonObject params = new Gson().fromJson(new String(Base64.getDecoder().decode(config.toHeaderValue(log).getBytes())), JsonObject.class);
        assertEquals(username, params.get("username").getAsString());
        assertEquals(password, params.get("password").getAsString());
        if (email != null) {
            assertEquals(email, params.get("email").getAsString());
        }
    }

}
