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
package org.eclipse.jkube.kit.resource.helm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Optional;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class HelmRepository {

  private String name;
  private String url;
  private String username;
  private String password;
  private HelmRepoType type;


  public String getTypeAsString() {
    return Optional.ofNullable(type).map(HelmRepoType::toString).orElse(null);
  }

  // Plexus deserialization specific setters
  public void setType(String type) {
    this.type = HelmRepoType.parseString(type);
  }

  public enum HelmRepoType {
    CHARTMUSEUM,
    ARTIFACTORY,
    NEXUS,
    OCI;

    public static HelmRepoType parseString(String repoType) {
      return Optional.ofNullable(repoType).map(String::toUpperCase).map(HelmRepoType::valueOf).orElse(null);
    }
  }

  @Override
  public String toString() {
    return "[" + name + " / " + url + "]";
  }
}
