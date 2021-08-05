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
package org.eclipse.jkube.gradle.plugin.task;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;

import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.text.CaseUtils;
import org.eclipse.jkube.gradle.plugin.KubernetesExtension;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gradle.api.provider.Property;

public class KubernetesConfigViewTask extends AbstractJKubeTask {

  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());
  static {
    MAPPER.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
  }

  @SuppressWarnings("CdiInjectionPointsInspection")
  @Inject
  public KubernetesConfigViewTask(Class<? extends KubernetesExtension> extensionClass) {
    super(extensionClass);
    super.setGroup("help");
    setDescription("Prints the applicable configuration for Kubernetes Gradle Plugin");
  }

  @Override
  public void run() {
    kitLogger.info("Kubernetes Maven Plugin configuration:");
    try {
      final Map<String, Object> effectiveConfig = new LinkedHashMap<>();
      for (Method method : KubernetesExtension.class.getMethods()) {
        if (method.getParameters().length == 0 && Property.class.isAssignableFrom(method.getReturnType())) {
          effectiveConfig.put(CaseUtils.toCamelCase(method.getName().replaceFirst("^get", ""), false),
              ((Property) method.invoke(kubernetesExtension)).getOrElse(null));
        }
      }
      for (Field field : KubernetesExtension.class.getDeclaredFields()) {
        if (isPublicField(field) && field.get(kubernetesExtension) != null) {
          effectiveConfig.put(field.getName(), field.get(kubernetesExtension));
        }
      }
      kitLogger.info("%n%s", MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(effectiveConfig));
    } catch (InvocationTargetException | IllegalAccessException | JsonProcessingException ex) {
      kitLogger.error("Error when reading configuration: %s", ex.getMessage());
    }
  }

  @Override
  public void setGroup(@Nullable String group) {
    // Task group is set in constructor and shouldn't be changed.
  }

  private static boolean isPublicField(Field field) {
    return Modifier.isPublic(field.getModifiers()) && !Modifier.isStatic(field.getModifiers()) && !field.isSynthetic();
  }
}
