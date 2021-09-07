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
package org.eclipse.jkube.kit.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * @author roland
 */
public class PluginServiceFactoryTest {

  private PluginServiceFactory<TestContext> pluginServiceFactory;

  @Before
  public void setup() {
    pluginServiceFactory = new PluginServiceFactory<>(new TestContext());
  }

  @Test
  public void createServiceObjects_withOrdersAndExclusions_shouldReturnInCorrectOrder() {
    // Given
    final String[] descriptorPaths = new String[] { "service/test-services-default", "service/test-services" };
    // When
    final List<TestService> result = pluginServiceFactory.createServiceObjects(descriptorPaths);
    // Then
    assertThat(result)
        .isInstanceOf(ArrayList.class)
        .hasSize(4)
        .extracting("name")
        .containsExactly("three", "two", "five", "one");
  }

  @Test
  public void createServiceObjects_withNonExistentClass_shouldThrowException() {
    // When
    final IllegalStateException result = assertThrows(IllegalStateException.class, () ->
        pluginServiceFactory.createServiceObjects("service/error-services"));
    // Then
    assertThat(result)
        .hasMessageMatching(".*bla\\.blub\\.NotExist.*")
        .hasCauseExactlyInstanceOf(ClassNotFoundException.class);
  }

  @Test
  public void createServiceObjects_withBadConstructorClass_shouldThrowException() {
    // When
    final IllegalStateException result = assertThrows(IllegalStateException.class, () ->
        pluginServiceFactory.createServiceObjects("service/error-constructor-services"));
    // Then
    assertThat(result)
        .hasMessageMatching("Cannot load service .*\\.PluginServiceFactoryTest\\$BadConstructor.*")
        .hasCauseExactlyInstanceOf(NoSuchMethodException.class);
  }

  @Test
  public void createServiceObjects_withBadGenericClass_shouldThrowException() {
    //Given
    final List<String> services = pluginServiceFactory.createServiceObjects("service/test-services");
    // When
    final ClassCastException result = assertThrows(ClassCastException.class, () -> {
      final String ignored = services.get(0);
    });
    // Then
    assertThat(result)
        .hasMessageContaining("PluginServiceFactoryTest$Test")
        .hasMessageContaining("cannot be cast")
        .hasMessageContaining("String");
  }

  private static class TestContext {
  }

  interface TestService {
    String getName();
  }

  public static class Test1 implements TestService {
    public Test1(TestContext ctx) {
    }

    public String getName() {
      return "one";
    }
  }

  public static class Test2 implements TestService {
    public Test2(TestContext ctx) {
    }

    public String getName() {
      return "two";
    }
  }

  public static class Test3 implements TestService {
    public Test3(TestContext ctx) {
    }

    public String getName() {
      return "three";
    }
  }

  public static class Test4 implements TestService {
    public Test4(TestContext ctx) {
    }

    public String getName() {
      return "four";
    }
  }

  public static class Test5 implements TestService {
    public Test5(TestContext ctx) {
    }

    public String getName() {
      return "five";
    }
  }

  public static class BadConstructor {
  }
}
