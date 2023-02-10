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
package org.eclipse.jkube.kit.remotedev;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.Getter;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.Base64Util;
import org.eclipse.jkube.kit.common.util.IoUtil;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class RemoteDevelopmentContext {

  private static final String REMOTE_DEV_PROPERTIES_FILE = "/META-INF/jkube/remote-dev.properties";

  @Getter
  private final KitLogger logger;
  @Getter
  private final KubernetesClient kubernetesClient;
  private final RemoteDevelopmentConfig remoteDevelopmentConfig;
  private final AtomicInteger sshPort;
  private final AtomicReference<String> user;
  private final KeyPair clientKeys;
  @Getter
  private final String sshRsaPublicKey;
  private final Properties properties;
  @Getter
  private final Map<LocalService, Service> managedServices;

  public RemoteDevelopmentContext(
    KitLogger kitLogger, KubernetesClient kubernetesClient, RemoteDevelopmentConfig remoteDevelopmentConfig) {
    this.logger = Objects.requireNonNull(kitLogger, "KitLogger is required");
    this.kubernetesClient = Objects.requireNonNull(kubernetesClient, "KubernetesClient is required");
    this.remoteDevelopmentConfig = Objects.requireNonNull(remoteDevelopmentConfig,
      "JKube's remoteDevelopmentConfig is required and should contain the definitions for local and remote services");
    sshPort = new AtomicInteger(-1);
    user = new AtomicReference<>();
    clientKeys = initClientKeys();
    sshRsaPublicKey = initSShRsaPublicKey(clientKeys);
    properties = new Properties();
    managedServices = new ConcurrentHashMap<>();
    try {
      properties.load(RemoteDevelopmentContext.class.getResourceAsStream(REMOTE_DEV_PROPERTIES_FILE));
    } catch(IOException ex) {
      throw new IllegalStateException("Unable to load remote development properties", ex);
    }
  }

  RemoteDevelopmentConfig getRemoteDevelopmentConfig() {
    return remoteDevelopmentConfig;
  }

  void reset() {
    sshPort.set(-1);
    user.set(null);
  }

  int getSshPort() {
    return sshPort.updateAndGet(v -> v == -1 ? IoUtil.getFreeRandomPort() : v);
  }

  String getUser() {
    return user.get();
  }

  void setUser(String user) {
    this.user.set(user);
  }

  KeyPair getClientKeys() {
    return clientKeys;
  }

  String getRemoteDevPodImage() {
    return properties.getProperty("jkube.remote-dev.pod.container.image");
  }

  int getRemoteDevPodPort() {
    return Integer.parseInt(properties.getProperty("jkube.remote-dev.pod.container.port"));
  }

  private static KeyPair initClientKeys() {
    try {
      final KeyPairGenerator kgp = KeyPairGenerator.getInstance("RSA");
      kgp.initialize(2048);
      return kgp.generateKeyPair();
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("RSA algorithm not available", ex);
    }
  }

  private static String initSShRsaPublicKey(KeyPair rsaKeyPair) {
    try (
      ByteArrayOutputStream byteOs = new ByteArrayOutputStream();
      DataOutputStream dos = new DataOutputStream(byteOs)
    ) {
      final RSAPublicKey rsaPublicKey = (RSAPublicKey) rsaKeyPair.getPublic();
      dos.writeInt("ssh-rsa".getBytes().length);
      dos.write("ssh-rsa".getBytes());
      dos.writeInt(rsaPublicKey.getPublicExponent().toByteArray().length);
      dos.write(rsaPublicKey.getPublicExponent().toByteArray());
      dos.writeInt(rsaPublicKey.getModulus().toByteArray().length);
      dos.write(rsaPublicKey.getModulus().toByteArray());
      return "ssh-rsa " +
        Base64Util.encodeToString(byteOs.toByteArray()) +
        " jkube@localhost";
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
