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

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stas Sukhanov
 * @since 08.03.2017
 */
public class KeyStoreUtilTest {

  @Test
  public void createKeyStore() throws Exception {
    KeyStore keyStore = KeyStoreUtil.createDockerKeyStore(getFile("certpath"));
    KeyStore.PrivateKeyEntry pkEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry("docker",
        new KeyStore.PasswordProtection("docker".toCharArray()));
    assertNotNull(pkEntry);
    assertNotNull(pkEntry.getCertificate());
    assertNotNull(keyStore.getCertificate("cn=ca-test,o=internet widgits pty ltd,st=some-state,c=cr"));
    assertNotNull(keyStore.getCertificate("cn=ca-test-2,o=internet widgits pty ltd,st=some-state,c=cr"));
  }

  @Test
  public void loadInvalidPrivateKey() {
    GeneralSecurityException exception = assertThrows(GeneralSecurityException.class, () -> KeyStoreUtil.loadPrivateKey(getFile("keys/invalid.pem")));
    assertThat(exception).hasMessageContaining("Cannot generate private key");
  }

  static String getFile(String path) throws FileNotFoundException, URISyntaxException {
    URL fileURL = KeyStoreUtilTest.class.getResource(path);
    if (fileURL == null)
      throw new FileNotFoundException("Required private key : '" + path + "' not found it test resource directory");
    return Paths.get(fileURL.toURI()).toString();
  }
}
