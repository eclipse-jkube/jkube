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
package org.eclipse.jkube.gradle.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import groovy.lang.Closure;
import groovy.lang.Script;
import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;

public class GroovyUtil {

  private static final ObjectMapper closureMapper = new ObjectMapper();

  private GroovyUtil() {}

  @SuppressWarnings("unchecked")
  public static <T> T closureTo(Closure<?> closure, Class<T> targetType) {
    final ConfigObject result = sanitize(new ConfigSlurper().parse(new ClosureScript(closure)));
    if (ConfigObject.class.isAssignableFrom(targetType)) {
      return (T)result;
    }
    return closureMapper.convertValue(result, targetType);
  }

  @SuppressWarnings("unchecked")
  public static <T> List<T> namedListClosureTo(Closure<?> closure, Class<T> targetListType) {
    final ConfigObject co = new ConfigSlurper().parse(new ClosureScript(closure));
    final List<T> ret = new ArrayList<>();
    for (Object entry : co.entrySet()) {
      final Map.Entry<String, ?> e = (Map.Entry<String, ?>)entry;
      Object value = e.getValue();
      if (value instanceof Closure) {
        value = closureTo((Closure<?>)e.getValue(), ConfigObject.class);
      }
      if (!(value instanceof ConfigObject)) {
        throw new IllegalArgumentException("Invalid configuration entry: " + e.getKey());
      }
      ret.add(closureMapper.convertValue(sanitize((ConfigObject) value), targetListType));
    }
    return ret;
  }

  @SuppressWarnings("unchecked")
  private static ConfigObject sanitize(ConfigObject configObject) {
    final ConfigObject result = configObject.clone();
    for (Object entry : configObject.entrySet()) {
      final Map.Entry<String, ?> e = (Map.Entry<String, ?>)entry;
      if (e.getValue() instanceof Closure) {
        result.put(Objects.toString(e.getKey()), closureTo((Closure<?>)e.getValue(), ConfigObject.class));
      } else {
        result.put(Objects.toString(e.getKey()), e.getValue());
      }
    }
    return result;
  }

  private static final class ClosureScript extends Script {
    private final Closure<?> closure;

    public ClosureScript(Closure<?> closure) {
      this.closure = closure;
    }

    @Override
    public Object run() {
      final Closure<?> copy = closure.rehydrate(this, this, closure.getThisObject());
      copy.setResolveStrategy(Closure.DELEGATE_FIRST);
      copy.setDelegate(this);
      return copy.call();
    }
  }
}
