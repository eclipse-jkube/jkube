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
package org.eclipse.jkube.kit.build.service.docker.auth;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

/**
 * Utility class for modifying environment variables in tests.
 * Uses reflection to modify the environment map.
 */
public class EnvironmentVariablesTestUtil {

  /**
   * Sets an environment variable for testing purposes.
   * This uses reflection to modify the environment map.
   *
   * @param key   the environment variable name
   * @param value the environment variable value
   */
  @SuppressWarnings("unchecked")
  public static void setEnvironmentVariable(String key, String value) {
    try {
      Map<String, String> env = System.getenv();
      Class<?> cl = env.getClass();
      Field field = cl.getDeclaredField("m");
      field.setAccessible(true);
      Map<String, String> writableEnv = (Map<String, String>) field.get(env);
      writableEnv.put(key, value);
    } catch (Exception e) {
      try {
        // Fallback approach for different JVM implementations
        Class<?>[] classes = Collections.class.getDeclaredClasses();
        Map<String, String> env = System.getenv();
        for (Class<?> cl : classes) {
          if (cl.isInstance(env)) {
            Field field = cl.getDeclaredField("m");
            field.setAccessible(true);
            Object obj = field.get(env);
            Map<String, String> map = (Map<String, String>) obj;
            map.put(key, value);
            break;
          }
        }
      } catch (Exception e2) {
        throw new RuntimeException("Failed to set environment variable", e2);
      }
    }
  }

  /**
   * Clears an environment variable for testing purposes.
   *
   * @param key the environment variable name to clear
   */
  @SuppressWarnings("unchecked")
  public static void clearEnvironmentVariable(String key) {
    try {
      Map<String, String> env = System.getenv();
      Class<?> cl = env.getClass();
      Field field = cl.getDeclaredField("m");
      field.setAccessible(true);
      Map<String, String> writableEnv = (Map<String, String>) field.get(env);
      writableEnv.remove(key);
    } catch (Exception e) {
      try {
        // Fallback approach
        Class<?>[] classes = Collections.class.getDeclaredClasses();
        Map<String, String> env = System.getenv();
        for (Class<?> cl : classes) {
          if (cl.isInstance(env)) {
            Field field = cl.getDeclaredField("m");
            field.setAccessible(true);
            Object obj = field.get(env);
            Map<String, String> map = (Map<String, String>) obj;
            map.remove(key);
            break;
          }
        }
      } catch (Exception e2) {
        throw new RuntimeException("Failed to clear environment variable", e2);
      }
    }
  }

  /**
   * Sets multiple environment variables at once.
   *
   * @param variables map of variable names to values
   */
  public static void setEnvironmentVariables(Map<String, String> variables) {
    variables.forEach(EnvironmentVariablesTestUtil::setEnvironmentVariable);
  }
}
