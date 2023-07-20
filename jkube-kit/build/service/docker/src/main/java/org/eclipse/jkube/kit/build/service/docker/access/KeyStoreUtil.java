/*
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

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

/**
 * Utility class for building up a keystore which can be used in
 * SSL communication.
 *
 * @author roland
 * @since 20.10.14
 */
public class KeyStoreUtil {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private KeyStoreUtil() { }

    /**
     * Create a key stored holding certificates and secret keys from the given Docker key cert
     *
     * @param certPath directory holding the keys (key.pem) and certs (ca.pem, cert.pem)
     * @return a keystore where the private key is secured with "docker"
     *
     * @throws IOException is reading of the the PEMs failed
     * @throws GeneralSecurityException when the files in a wrong format
     */
    public static KeyStore createDockerKeyStore(String certPath) throws IOException, GeneralSecurityException {
        PrivateKey privKey = loadPrivateKey(certPath + "/key.pem");
        Certificate[] certs = loadCertificates(certPath + "/cert.pem");

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null);

        keyStore.setKeyEntry("docker", privKey, "docker".toCharArray(), certs);
        addCA(keyStore, certPath + "/ca.pem");
        return keyStore;
    }

    static PrivateKey loadPrivateKey(String keyPath) throws IOException, GeneralSecurityException {
        try (Reader reader = new FileReader(keyPath);
             PEMParser parser = new PEMParser(reader)) {
            Object readObject;
            while ((readObject = parser.readObject()) != null) {
                if (readObject instanceof PEMKeyPair) {
                    PEMKeyPair keyPair = (PEMKeyPair) readObject;
                    return generatePrivateKey(keyPair.getPrivateKeyInfo());
                } else if (readObject instanceof PrivateKeyInfo) {
                    return generatePrivateKey((PrivateKeyInfo) readObject);
                }
            }
        }
        throw new GeneralSecurityException("Cannot generate private key from file: " + keyPath);
    }

    private static PrivateKey generatePrivateKey(PrivateKeyInfo keyInfo) throws IOException {
        return new JcaPEMKeyConverter().getPrivateKey(keyInfo);
    }

    private static void addCA(KeyStore keyStore, String caPath) throws IOException, KeyStoreException,
                                                                       CertificateException {
        for (Certificate cert : loadCertificates(caPath)) {
            X509Certificate crt = (X509Certificate) cert;
            String alias = crt.getSubjectX500Principal().getName();
            keyStore.setCertificateEntry(alias, crt);
        }
    }

    private static Certificate[] loadCertificates(String certPath) throws IOException, CertificateException {
        try (InputStream is = new FileInputStream(certPath)) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
            Collection<? extends Certificate> certs = certificateFactory.generateCertificates(is);
            return certs.toArray(new Certificate[certs.size()]);
        }
    }
}
