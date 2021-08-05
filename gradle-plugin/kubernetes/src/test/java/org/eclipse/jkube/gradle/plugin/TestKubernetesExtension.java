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

import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.gradle.api.internal.provider.DefaultProperty;
import org.gradle.api.provider.Property;

public class TestKubernetesExtension extends KubernetesExtension {

  @Override
  public Property<Boolean> getOffline() {
    return new DefaultProperty<>(Boolean.class).value(true);
  }

  @Override
  public Property<Boolean> getUseColor() {
    return new DefaultProperty<>(Boolean.class);
  }

  @Override
  public Property<Integer> getMaxConnections() {
    return new DefaultProperty<>(Integer.class);
  }

  @Override
  public Property<String> getFilter() {
    return new DefaultProperty<>(String.class);
  }

  @Override
  public Property<String> getApiVersion() {
    return new DefaultProperty<>(String.class);
  }

  @Override
  public Property<String> getBuildRecreate() {
    return new DefaultProperty<>(String.class);
  }

  @Override
  public Property<String> getImagePullPolicy() {
    return new DefaultProperty<>(String.class);
  }

  @Override
  public Property<String> getAutoPull() {
    return new DefaultProperty<>(String.class);
  }

  @Override
  public Property<String> getDockerHost() {
    return new DefaultProperty<>(String.class);
  }

  @Override
  public Property<String> getCertPath() {
    return new DefaultProperty<>(String.class);
  }

  @Override
  public Property<String> getMinimalApiVersion() {
    return new DefaultProperty<>(String.class);
  }

  @Override
  public Property<Boolean> getSkipMachine() {
    return new DefaultProperty<>(Boolean.class);
  }

  @Override
  public Property<Boolean> getForcePull() {
    return new DefaultProperty<>(Boolean.class);
  }

  @Override
  public Property<Boolean> getSkipExtendedAuth() {
    return new DefaultProperty<>(Boolean.class);
  }

  @Override
  public Property<String> getPullRegistry() {
    return new DefaultProperty<>(String.class);
  }

  @Override
  public Property<String> getBuildSourceDirectory() {
    return new DefaultProperty<>(String.class);
  }

  @Override
  public Property<String> getBuildOutputDirectory() {
    return new DefaultProperty<>(String.class);
  }

  @Override
  public Property<String> getRegistry() {
    return new DefaultProperty<>(String.class);
  }
}
