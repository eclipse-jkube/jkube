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
package org.eclipse.jkube.kit.common;

import com.marcnuri.helm.jni.HelmLib;
import com.marcnuri.helm.jni.NativeLibrary;
import com.marcnuri.helm.jni.RepoServerOptions;
import lombok.Getter;
import org.eclipse.jkube.kit.common.util.Base64Util;

import java.io.Closeable;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;

@Getter
public class TestOciServer implements Closeable {

  private static final Duration TIMEOUT = Duration.ofSeconds(10);
  private static final Duration RETRY_INTERVAL = Duration.ofMillis(100);
  private static final String DEFAULT_USER = "oci-user";
  private static final String DEFAULT_PASSWORD = "oci-password";

  // initialization-on-demand holder
  private static final class HelmLibHolder {
    private static final HelmLib INSTANCE = NativeLibrary.getInstance().load();
  }

  private static HelmLib getHelmLib() {
    return HelmLibHolder.INSTANCE;
  }

  private final String user;
  private final String password;
  private String url;

  public TestOciServer() {
    this(DEFAULT_USER, DEFAULT_PASSWORD);
  }

  public TestOciServer(String user, String password) {
    this.user = user;
    this.password = password;
  }

  /**
   * Starts the OCI server and waits until it is ready to accept connections.
   * This method blocks until the server is ready or the timeout is reached.
   *
   * @throws IllegalStateException if the server fails to start within the timeout period
   */
  public void start() {
    if (url != null) {
      throw new IllegalStateException("Server is already started");
    }

    url = getHelmLib().RepoOciServerStart(
      new RepoServerOptions(null, user, password)
    ).out;

    waitForServerReady();
  }

  @Override
  public void close() throws IOException {
    if (url != null) {
      // No effect yet https://github.com/manusa/helm-java/blob/f44a88ed1ad351b2b5a00b5e735deb5cb35b32f7/native/internal/helm/repotest.go#L138
      getHelmLib().RepoServerStop(url);
      url = null;
    }
  }

  private void waitForServerReady() {
    final long startTime = System.currentTimeMillis();
    final long timeoutMillis = TIMEOUT.toMillis();
    final long retryIntervalMillis = RETRY_INTERVAL.toMillis();

    while (System.currentTimeMillis() - startTime < timeoutMillis) {
      if (isServerReady()) {
        return;
      }

      try {
        Thread.sleep(retryIntervalMillis);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Interrupted while waiting for OCI server to start", e);
      }
    }

    throw new IllegalStateException(
      String.format("OCI server at %s failed to become ready within %d milliseconds", url, timeoutMillis)
    );
  }

  private boolean isServerReady() {
    try {
      final HttpURLConnection connection = (HttpURLConnection) new URL("http://" + url + "/v2/")
        .openConnection();
      connection.setRequestProperty("Authorization", "Basic " + Base64Util.encodeToString(String.join(":", user, password)));
      connection.setConnectTimeout((int) RETRY_INTERVAL.toMillis());
      connection.setReadTimeout((int) RETRY_INTERVAL.toMillis());
      connection.connect();
      final int responseCode = connection.getResponseCode();
      connection.disconnect();
      return responseCode == HttpURLConnection.HTTP_OK;
    } catch (IOException e) {
      return false;
    }
  }
}
