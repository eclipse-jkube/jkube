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

import static org.eclipse.jkube.kit.common.util.AsyncUtil.await;
import static org.eclipse.jkube.kit.common.util.AsyncUtil.get;

@Getter
public class TestOciServer implements Closeable {

  private static final Duration TIMEOUT = Duration.ofSeconds(5);
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
  private final String url;

  public TestOciServer() {
    this(DEFAULT_USER, DEFAULT_PASSWORD);
  }

  public TestOciServer(String user, String password) {
    this.user = user;
    this.password = password;
    url = get(await(this::startServer).apply(this::waitForServer), TIMEOUT);
  }

  @Override
  public void close() throws IOException {
    // No effect yet https://github.com/manusa/helm-java/blob/f44a88ed1ad351b2b5a00b5e735deb5cb35b32f7/native/internal/helm/repotest.go#L138
    getHelmLib().RepoServerStop(url);
  }

  private String startServer() {
    return getHelmLib().RepoOciServerStart(
      new RepoServerOptions(null, user, password)
    ).out;
  }

  private boolean waitForServer(String serverUrl) {
    try {
      final HttpURLConnection connection = (HttpURLConnection) new URL("http://" + serverUrl + "/v2/")
        .openConnection();
      connection.setRequestProperty("Authorization", "Basic " + Base64Util.encodeToString(String.join(":", user, password)));
      connection.connect();
      return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
    } catch (IOException e) {
      return false;
    }
  }
}
