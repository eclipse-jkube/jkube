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

import static org.junit.Assert.assertNotNull;

import java.io.FileNotFoundException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Stas Sukhanov
 * @since 08.03.2017
 */
public class KeyStoreUtilTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

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
  public void loadInvalidPrivateKey() throws Exception {
    exception.expect(GeneralSecurityException.class);
    exception.expectMessage("Cannot generate private key");
    KeyStoreUtil.loadPrivateKey(getFile("keys/invalid.pem"));
  }

  static String getFile(String path) throws FileNotFoundException {
    URL fileURL = KeyStoreUtilTest.class.getResource(path);
    if (fileURL == null)
      throw new FileNotFoundException("Required private key : '" + path + "' not found it test resource directory");
    return fileURL.getFile();
  }
}
