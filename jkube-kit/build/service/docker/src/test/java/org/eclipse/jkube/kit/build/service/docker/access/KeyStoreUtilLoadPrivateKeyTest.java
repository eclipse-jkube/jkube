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
package org.eclipse.jkube.kit.build.service.docker.access;

import java.security.PrivateKey;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.eclipse.jkube.kit.build.service.docker.access.KeyStoreUtilTest.*;
import static org.junit.Assert.assertNotNull;

@RunWith(Parameterized.class)
public class KeyStoreUtilLoadPrivateKeyTest {

  @Parameterized.Parameter
  public String loadPrivateKeyTestName;

  @Parameterized.Parameter(1)
  public String privateKeyFile;

  @Parameterized.Parameters(name = "{index}: {0}")
  public static Object[][] data() {
    return new Object[][] {
        { "loadPrivateKeyDefault", "keys/pkcs1.pem" },
        { "loadPrivateKeyPKCS8", "keys/pkcs8.pem" },
        { "loadPrivateKeyECDSA", "keys/ecdsa.pem" }
    };
  }

  @Test
  public void loadPrivateKey() throws Exception {
    PrivateKey privateKey = KeyStoreUtil.loadPrivateKey(getFile(privateKeyFile));
    assertNotNull(privateKey);
  }
}
