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
package org.eclipse.jkube.kit.common;

import java.util.Properties;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

public class SystemMock extends MockUp<System> {
  private final Properties properties = new Properties();

  public SystemMock put(String key, String value) {
    properties.put(key, value);
    return this;
  }

  @Mock
  public String getProperty(Invocation invocation, String key) {
    return properties.getProperty(key, invocation.proceed(key));
  }

  @Mock
  public String getProperty(Invocation invocation, String key, String def) {
    return properties.getProperty(key, def);
  }

  @Mock
  public Properties getProperties() {
    return properties;
  }

}
