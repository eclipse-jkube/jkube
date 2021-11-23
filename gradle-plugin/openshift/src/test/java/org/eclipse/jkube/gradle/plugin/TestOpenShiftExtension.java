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

import java.io.File;

import org.eclipse.jkube.kit.common.JavaProject;
import org.gradle.api.internal.provider.DefaultProperty;
import org.gradle.api.provider.Property;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

public class TestOpenShiftExtension extends OpenShiftExtension {

  public Boolean isOffline;
  public String buildRecreate;
  public Boolean isForcePull;
  public Boolean isFailOnNoKubernetesJson;
  public Boolean isSkipPush;

  public TestOpenShiftExtension() {
    javaProject = mock(JavaProject.class, RETURNS_DEEP_STUBS);
  }

  @Override
  public Property<Boolean> getOffline() {
    return new DefaultProperty<>(Boolean.class).value(isOffline);
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
    return new DefaultProperty<>(String.class).value(buildRecreate);
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
    return new DefaultProperty<>(Boolean.class).value(isForcePull);
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

  @Override
  public Property<Boolean> getProcessTemplatesLocally() {
    return new DefaultProperty<>(Boolean.class);
  }

  @Override
  public Property<Boolean> getIgnoreRunningOAuthClients() {
    return new DefaultProperty<>(Boolean.class);
  }

  @Override
  public Property<File> getResourceTargetDirectory() {
    return new DefaultProperty<>(File.class);
  }

  @Override
  public Property<File> getResourceSourceDirectory() {
    return new DefaultProperty<>(File.class);
  }

  @Override
  public Property<String> getResourceEnvironment() {
    return new DefaultProperty<>(String.class);
  }

  @Override
  public Property<Boolean> getUseProjectClassPath() {
    return new DefaultProperty<>(Boolean.class);
  }

  @Override
  public Property<File> getWorkDirectory() {
    return new DefaultProperty<>(File.class);
  }

  @Override
  public Property<Boolean> getSkipResourceValidation() {
    return new DefaultProperty<>(Boolean.class);
  }

  @Override
  public Property<Boolean> getFailOnValidationError() {
    return new DefaultProperty<>(Boolean.class);
  }

  @Override
  public Property<String> getProfile() {
    return new DefaultProperty<>(String.class);
  }

  @Override
  public Property<String> getNamespace() {
    return new DefaultProperty<>(String.class);
  }

  @Override
  public Property<Boolean> getMergeWithDekorate() {
    return new DefaultProperty<>(Boolean.class);
  }

  @Override
  public Property<Boolean> getInterpolateTemplateParameters() {
    return new DefaultProperty<>(Boolean.class);
  }

  @Override
  public Property<Boolean> getSkip() {
    return new DefaultProperty<>(Boolean.class);
  }

  @Override
  public Property<Boolean> getLogFollow() {
    return new DefaultProperty<>(Boolean.class);
  }

  @Override
  public Property<String> getLogContainerName() {
    return new DefaultProperty<>(String.class);
  }

  @Override
  public Property<String> getLogPodName() {
    return new DefaultProperty<>(String.class);
  }

  @Override
  public Property<File> getKubernetesManifest() {
    return new DefaultProperty<>(File.class);
  }

  @Override
  public Property<String> getSourceDirectory() {
    return new DefaultProperty<>(String.class);
  }

  @Override
  public Property<String> getOutputDirectory() {
    return new DefaultProperty<>(String.class);
  }

  @Override
  public Property<Boolean> getRecreate() {
    return new DefaultProperty<>(Boolean.class);
  }

  @Override
  public Property<Boolean> getSkipApply() {
    return new DefaultProperty<>(Boolean.class);
  }

  @Override
  public Property<Boolean> getCreateNewResources() {
    return new DefaultProperty<>(Boolean.class);
  }

  @Override
  public Property<Boolean> getRollingUpgrades() {
    return new DefaultProperty<>(Boolean.class);
  }

  @Override
  public Property<Boolean> getRollingUpgradePreserveScale() {
    return new DefaultProperty<>(Boolean.class);
  }

  @Override
  public Property<Boolean> getFailOnNoKubernetesJson() {
    return new DefaultProperty<>(Boolean.class).value(isFailOnNoKubernetesJson);
  }

  @Override
  public Property<Boolean> getServicesOnly() {
    return new DefaultProperty<>(Boolean.class);
  }

  @Override
  public Property<Boolean> getIgnoreServices() {
    return new DefaultProperty<>(Boolean.class);
  }

  @Override
  public Property<Boolean> getDeletePodsOnReplicationControllerUpdate() {
    return new DefaultProperty<>(Boolean.class);
  }

  @Override
  public Property<File> getJsonLogDir() {
    return new DefaultProperty<>(File.class);
  }

  @Override
  public Property<Integer> getServiceUrlWaitTimeSeconds() {
    return new DefaultProperty<>(Integer.class);
  }

  @Override
  public Property<Boolean> getSkipPush() {
    return new DefaultProperty<>(Boolean.class).value(isSkipPush);
  }

  @Override
  public Property<String> getPushRegistry() {
    return new DefaultProperty<>(String.class);
  }

  @Override
  public Property<Boolean> getSkipTag() {
    return new DefaultProperty<>(Boolean.class);
  }

  @Override
  public Property<Integer> getPushRetries() {
    return new DefaultProperty<>(Integer.class);
  }

  @Override
  public Property<Integer> getLocalDebugPort() {
    return new DefaultProperty<>(Integer.class);
  }

  @Override
  public Property<Boolean> getDebugSuspend() {
    return new DefaultProperty<>(Boolean.class);
  }

  @Override
  public Property<File> getOpenShiftManifest() {
    return new DefaultProperty<>(File.class);
  }

  @Override
  public Property<String> getOpenshiftPullSecret() {
    return new DefaultProperty<>(String.class);
  }

  @Override
  public Property<String> getS2iBuildNameSuffix() {
    return new DefaultProperty<>(String.class);
  }

  @Override
  public Property<Boolean> getS2iImageStreamLookupPolicyLocal() {
    return new DefaultProperty<>(Boolean.class);
  }

  @Override
  public Property<String> getBuildOutputKind() {
    return new DefaultProperty<>(String.class);
  }

  @Override
  public Property<String> getOpenshiftPushSecret() {
    return new DefaultProperty<>(String.class);
  }

  @Override
  public Property<File> getImageStreamManifest() {
    return new DefaultProperty<>(File.class);
  }

  @Override
  public Property<File> getKubernetesTemplate() {
    return new DefaultProperty<>(File.class);
  }

  @Override
  public Property<Boolean> getSkipResource() {
    return new DefaultProperty<>(Boolean.class);
  }

  @Override
  public Property<Boolean> getSkipBuild() {
    return new DefaultProperty<>(Boolean.class);
  }
}
