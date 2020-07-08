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

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

import java.util.HashMap;
import java.util.Map;

public class SystemMock extends MockUp<System> {
  private final Map<String, String> properties = new HashMap<>();

  public SystemMock put(String key, String value) {
    properties.put(key, value);
    return this;
  }

  @Mock
  public String getProperty(Invocation invocation, String key) {
    return properties.getOrDefault(key, invocation.proceed(key));
  }

  @Mock
  public String getProperty(Invocation invocation, String key, String def) {
    return properties.getOrDefault(key, def);
  }

}
