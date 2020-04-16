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
package org.eclipse.jkube.kit.build.service.docker;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.eclipse.jkube.kit.build.service.docker.auth.AuthConfigFactory;
import org.eclipse.jkube.kit.common.RegistryServerConfiguration;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class RegistryConfig implements Serializable {

  private static final long serialVersionUID = -7864593044203613241L;

  private String registry;
  private List<RegistryServerConfiguration> settings;
  private AuthConfigFactory authConfigFactory;
  private boolean skipExtendedAuth;
  private Map authConfig;
  private transient UnaryOperator<String> passwordDecryptionMethod;

}
