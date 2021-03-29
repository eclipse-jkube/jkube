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
package org.eclipse.jkube.kit.resource.helm;

public class HelmRepository {
  private String name;
  private String url;
  private String username;
  private String password;
  private HelmRepoType type;

  public enum HelmRepoType {
    CHARTMUSEUM,
    ARTIFACTORY,
    NEXUS
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public HelmRepoType getType() {
    return type;
  }

  public void setType(HelmRepoType type) {
    this.type = type;
  }


  @Override
  public String toString() {
    return "[" + name + " / " + url + "]";
  }
}
