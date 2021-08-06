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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import groovy.lang.Closure;
import groovy.lang.GString;
import groovy.lang.Script;
import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;
import org.gradle.api.internal.project.ProjectScript;

public class GroovyUtil {

  private static final ObjectMapper closureMapper = new ObjectMapper();

  private GroovyUtil() {}

  @SuppressWarnings("unchecked")
  public static <T> T closureTo(Closure<?> closure, Class<T> targetType) {
    final ConfigObject result = sanitize(parse(closure));
    if (ConfigObject.class.isAssignableFrom(targetType)) {
      return (T)result;
    }
    return closureMapper.convertValue(result, targetType);
  }

  @SuppressWarnings("unchecked")
  public static <T> List<T> namedListClosureTo(Closure<?> closure, Class<T> targetListType) {
    final ConfigObject co = parse(closure);
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
      result.put(Objects.toString(e.getKey()), sanitize(e.getValue()));
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private static Object sanitize(Object object) {
    if (object instanceof Closure) {
      return closureTo((Closure<?>) object, ConfigObject.class);
    } else if (object instanceof ConfigObject) {
      return sanitize((ConfigObject) object);
    } else if (object instanceof Collection) {
      return ((Collection<Object>) object).stream().map(GroovyUtil::sanitize).collect(Collectors.toList());
    } else if (object instanceof GString) {
      return ((GString) object).toString();
    }
    return object;
  }

  private static ConfigObject parse(Closure<?> closure) {
    final ConfigSlurper slurper = new ConfigSlurper();
    final Map<String, Object> binding = new HashMap<>();
    if (closure.getThisObject() instanceof ProjectScript) {
      binding.put("project", ((ProjectScript) closure.getThisObject()).getScriptTarget());
    }
    slurper.setBinding(binding);
    return slurper.parse(new ClosureScript(closure));
  }

  private static final class ClosureScript extends Script {
    private final Closure<?> closure;

    public ClosureScript(Closure<?> closure) {
      this.closure = closure;
    }

    @Override
    public Object run() {
      final Closure<?> copy = closure.rehydrate(this, closure.getOwner(), closure.getThisObject());
      copy.setResolveStrategy(Closure.DELEGATE_FIRST);
      copy.setDelegate(this);
      return copy.call();
    }
  }
}
