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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.jkube.gradle.plugin.task.JKubeTask;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.tasks.TaskProvider;

public abstract class AbstractJKubePlugin<E extends KubernetesExtension> implements JKubePlugin {

  private final String name;
  private final Class<E> extensionClass;
  private final Set<TaskProvider<? extends JKubeTask>> registeredTasks;

  protected AbstractJKubePlugin(String name, Class<E> extensionClass) {
    this.name = name;
    this.extensionClass = extensionClass;
    this.registeredTasks = new HashSet<>();
  }

  @Override
  public final void apply(Project project) {
    project.getExtensions().create(name, extensionClass);
    jKubeApply(project);
    configureTasks(project);
  }

  protected abstract void jKubeApply(Project project);

  private void configureTasks(Project project) {
    final Map<String, Collection<Class<? extends Task>>> precedence = getTaskPrecedence();
    project.afterEvaluate(evaluatedProject -> {
      for (TaskProvider<? extends JKubeTask> taskProvider : registeredTasks) {
        taskProvider.configure(task -> {
          task.setGroup(name);
          Stream.of("build", "compile", "java", "jar")
              .map(taskByName(evaluatedProject))
              .filter(Objects::nonNull)
              // Cannot remove warning (object spread)
              .forEach(t -> task.mustRunAfter(t));
          precedence.getOrDefault(task.getName(), Collections.emptyList())
              .forEach(taskDepClass -> task.mustRunAfter(evaluatedProject.getTasks().withType(taskDepClass)));
        });
      }
    });
  }

  protected final <T extends JKubeTask> TaskProvider<T> register(Project project, String name, Class<T> type) {
    final TaskProvider<T> registeredTask = project.getTasks().register(name, type, extensionClass);
    registeredTasks.add(registeredTask);
    return registeredTask;
  }

  private static Function<String, Task> taskByName(Project evaluatedProject) {
    return taskName -> {
      try {
        return evaluatedProject.getTasks().getByName(taskName);
      } catch (UnknownTaskException ignore) {
        return null;
      }
    };
  }
}
