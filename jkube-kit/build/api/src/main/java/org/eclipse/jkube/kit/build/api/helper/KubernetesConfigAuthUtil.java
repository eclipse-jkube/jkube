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
package org.eclipse.jkube.kit.build.api.helper;


import com.fasterxml.jackson.core.type.TypeReference;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.eclipse.jkube.kit.build.api.auth.AuthConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KubernetesConfigAuthUtil {

  private static final String KUBECONFIG_ENV = "KUBECONFIG";
  private static final Path KUBECONFIG_FILE = Paths.get(".kube", "config");

  private KubernetesConfigAuthUtil() {
  }

  public static AuthConfig readKubeConfigAuth() {
    Map<String, ?> kubeConfig = readKubeConfig();
    if (kubeConfig == null) {
      return null;
    }
    String currentContextName = (String) kubeConfig.get("current-context");
    if (currentContextName == null) {
      return null;
    }

    for (Map<String, ?> contextMap : (List<Map<String, ?>>) kubeConfig.get("contexts")) {
      if (currentContextName.equals(contextMap.get("name"))) {
        return parseContext(kubeConfig, (Map<String, ?>) contextMap.get("context"));
      }
    }

    return null;
  }

  private static AuthConfig parseContext(Map<String, ?> kubeConfig, Map<String, ?> context) {
    if (context == null) {
      return null;
    }
    String userName = (String) context.get("user");
    if (userName == null) {
      return null;
    }

    List<Map<String, ?>> users = (List<Map<String, ?>>) kubeConfig.get("users");
    if (users == null) {
      return null;
    }

    for (Map<String, ?> userMap : users) {
      if (userName.equals(userMap.get("name"))) {
        return parseUser(userName, (Map<String, ?>) userMap.get("user"));
      }
    }
    return null;
  }

  private static AuthConfig parseUser(String userName, Map<String, ?> user) {
    if (user == null) {
      return null;
    }
    String token = (String) user.get("token");
    if (token == null) {
      return null;
    }

    // Strip off stuff after username
    Matcher matcher = Pattern.compile("^([^/]+).*$").matcher(userName);
    return AuthConfig.builder()
        .username(matcher.matches() ? matcher.group(1) : userName)
        .password(token)
        .build();
  }

  private static Map<String, Object> readKubeConfig()  {
    String kubeConfig = System.getenv(KUBECONFIG_ENV);
    final File applicableFile = kubeConfig == null ?
        getHomeDir().toPath().resolve(KUBECONFIG_FILE).toFile() : new File(kubeConfig);
    if (applicableFile.exists()) {
      try (FileInputStream fis = new FileInputStream(applicableFile)) {
        return Serialization.unmarshal(fis, new TypeReference<Map<String, Object>>() {});
      } catch (IOException ex) {
        // Ignore
      }
    }
    return Collections.emptyMap();
  }

  private static File getHomeDir() {
    String homeDir = System.getProperty("user.home") != null ? System.getProperty("user.home") : System.getenv("HOME");
    return new File(homeDir);
  }
}
