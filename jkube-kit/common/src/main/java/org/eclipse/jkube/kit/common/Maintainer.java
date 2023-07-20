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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

/*
 * https://github.com/helm/helm/blob/v3.7.2/pkg/chart/metadata.go#L26
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@JsonInclude(NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Maintainer implements Serializable {

  private static final long serialVersionUID = -968020668786188166L;

  public Maintainer(String name, String email) {
    this(name, email, null);
  }

  @JsonProperty
  private String name;

  @JsonProperty
  private String email;

  @JsonProperty
  private String url;
}
