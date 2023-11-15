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

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.jkube.kit.common.Maintainer;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

/**
 * Represents the <a href="https://github.com/kubernetes/helm">Helm</a>
 * <a href="https://github.com/kubernetes/helm/blob/master/pkg/proto/hapi/chart/metadata.pb.go#L50">Chart.yaml file</a>
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@JsonInclude(NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Chart {
  @JsonProperty
  private String apiVersion;
  @JsonProperty
  private String name;
  @JsonProperty
  private String version;
  @JsonProperty
  private String kubeVersion;
  @JsonProperty
  private String description;
  @JsonProperty
  private String type;
  @JsonProperty
  private List<String> keywords;
  @JsonProperty
  private String home;
  @JsonProperty
  private List<String> sources;
  @JsonProperty
  private List<HelmDependency> dependencies;
  @JsonProperty
  private List<Maintainer> maintainers;
  @JsonProperty
  private String icon;
  @JsonProperty
  private String appVersion;
  @JsonProperty
  private Boolean deprecated;
  @JsonProperty
  private Map<String, String> annotations;
  @JsonProperty
  private String engine;

  @Override
  public String toString() {
    return "Chart{" +
      "name='" + name + '\'' +
      ", home='" + home + '\'' +
      ", version='" + version + '\'' +
      '}';
  }

}
