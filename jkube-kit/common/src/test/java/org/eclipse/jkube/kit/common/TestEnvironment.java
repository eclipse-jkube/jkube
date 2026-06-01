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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Test implementation of Environment for use in unit tests.
 *
 * <p>Usage example:</p>
 * <pre>
 * TestEnvironment env = new TestEnvironment();
 * env.put("AWS_ACCESS_KEY_ID", "test-key");
 * env.put("AWS_SECRET_ACCESS_KEY", "test-secret");
 *
 * // Pass env to classes under test via their package-private constructors
 * SomeClass instance = new SomeClass(env);
 * </pre>
 */
public class TestEnvironment implements Environment {

  private final Map<String, String> variables;

  /**
   * Creates a new test environment with an empty set of variables.
   */
  public TestEnvironment() {
    this.variables = new HashMap<>();
  }

  /**
   * Creates a new test environment with the given variables.
   *
   * @param variables initial environment variables
   */
  public TestEnvironment(Map<String, String> variables) {
    this.variables = new HashMap<>(variables);
  }

  @Override
  public String getEnv(String name) {
    return variables.get(name);
  }

  @Override
  public Map<String, String> getEnvMap() {
    return Collections.unmodifiableMap(variables);
  }

  /**
   * Sets an environment variable for testing purposes.
   *
   * @param key   the environment variable name
   * @param value the environment variable value
   * @return this instance for method chaining
   */
  public TestEnvironment put(String key, String value) {
    variables.put(key, value);
    return this;
  }

  /**
   * Removes an environment variable.
   *
   * @param key the environment variable name to remove
   * @return this instance for method chaining
   */
  public TestEnvironment remove(String key) {
    variables.remove(key);
    return this;
  }

  /**
   * Sets multiple environment variables at once.
   *
   * @param variables map of variable names to values
   * @return this instance for method chaining
   */
  public TestEnvironment putAll(Map<String, String> variables) {
    this.variables.putAll(variables);
    return this;
  }

  /**
   * Clears all environment variables.
   *
   * @return this instance for method chaining
   */
  public TestEnvironment clear() {
    variables.clear();
    return this;
  }

  /**
   * Gets all environment variables as a map.
   *
   * @return a copy of the environment variables map
   */
  public Map<String, String> getAll() {
    return new HashMap<>(variables);
  }
}

