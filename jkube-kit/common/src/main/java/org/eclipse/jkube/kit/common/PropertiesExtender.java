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

import java.net.URL;
import java.util.Properties;

import lombok.Getter;
import lombok.Setter;

// This class expends Properties class
// with URL of properties file path.
public class PropertiesExtender extends Properties {
  @Getter
  @Setter
  private URL propertiesFile;
}
