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
package org.eclipse.jkube.gradle.plugin;

import java.io.File;

import org.eclipse.jkube.kit.common.JavaProject;
import org.gradle.api.internal.provider.DefaultProperty;
import org.gradle.api.internal.provider.PropertyHost;
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
    return property(Boolean.class).value(isOffline);
  }

  @Override
  public Property<Boolean> getUseColor() {
    return property(Boolean.class);
  }

  @Override
  public Property<Integer> getMaxConnections() {
    return property(Integer.class);
  }

  @Override
  public Property<String> getFilter() {
    return property(String.class);
  }

  @Override
  public Property<String> getApiVersion() {
    return property(String.class);
  }

  @Override
  public Property<String> getBuildRecreate() {
    return property(String.class).value(buildRecreate);
  }

  @Override
  public Property<String> getImagePullPolicy() {
    return property(String.class);
  }

  @Override
  public Property<String> getAutoPull() {
    return property(String.class);
  }

  @Override
  public Property<String> getDockerHost() {
    return property(String.class);
  }

  @Override
  public Property<String> getCertPath() {
    return property(String.class);
  }

  @Override
  public Property<Boolean> getSkipMachine() {
    return property(Boolean.class);
  }

  @Override
  public Property<Boolean> getForcePull() {
    return property(Boolean.class).value(isForcePull);
  }

  @Override
  public Property<Boolean> getSkipExtendedAuth() {
    return property(Boolean.class);
  }

  @Override
  public Property<String> getPullRegistry() {
    return property(String.class);
  }

  @Override
  public Property<String> getBuildSourceDirectory() {
    return property(String.class);
  }

  @Override
  public Property<String> getBuildOutputDirectory() {
    return property(String.class);
  }

  @Override
  public Property<String> getRegistry() {
    return property(String.class);
  }

  @Override
  public Property<Boolean> getProcessTemplatesLocally() {
    return property(Boolean.class);
  }

  @Override
  public Property<Boolean> getIgnoreRunningOAuthClients() {
    return property(Boolean.class);
  }

  @Override
  public Property<File> getResourceTargetDirectory() {
    return property(File.class);
  }

  @Override
  public Property<File> getResourceSourceDirectory() {
    return property(File.class);
  }

  @Override
  public Property<String> getResourceEnvironment() {
    return property(String.class);
  }

  @Override
  public Property<Boolean> getUseProjectClassPath() {
    return property(Boolean.class);
  }

  @Override
  public Property<File> getWorkDirectory() {
    return property(File.class);
  }

  @Override
  public Property<Boolean> getSkipResourceValidation() {
    return property(Boolean.class);
  }

  @Override
  public Property<Boolean> getFailOnValidationError() {
    return property(Boolean.class);
  }

  @Override
  public Property<String> getProfile() {
    return property(String.class);
  }

  @Override
  public Property<String> getNamespace() {
    return property(String.class);
  }

  @Override
  public Property<Boolean> getMergeWithDekorate() {
    return property(Boolean.class);
  }

  @Override
  public Property<Boolean> getInterpolateTemplateParameters() {
    return property(Boolean.class);
  }

  @Override
  public Property<Boolean> getSkip() {
    return property(Boolean.class);
  }

  @Override
  public Property<Boolean> getLogFollow() {
    return property(Boolean.class);
  }

  @Override
  public Property<String> getLogContainerName() {
    return property(String.class);
  }

  @Override
  public Property<String> getLogPodName() {
    return property(String.class);
  }

  @Override
  public Property<String> getLogDate() {
    return property(String.class);
  }

  @Override
  public Property<Boolean> getLogStdout() {
    return property(Boolean.class);
  }

  @Override
  public Property<File> getKubernetesManifest() {
    return property(File.class);
  }

  @Override
  public Property<String> getSourceDirectory() {
    return property(String.class);
  }

  @Override
  public Property<String> getOutputDirectory() {
    return property(String.class);
  }

  @Override
  public Property<Boolean> getRecreate() {
    return property(Boolean.class);
  }

  @Override
  public Property<Boolean> getSkipApply() {
    return property(Boolean.class);
  }

  @Override
  public Property<Boolean> getSkipUndeploy() {
    return property(Boolean.class);
  }

  @Override
  public Property<Boolean> getCreateNewResources() {
    return property(Boolean.class);
  }

  @Override
  public Property<Boolean> getRollingUpgrades() {
    return property(Boolean.class);
  }

  @Override
  public Property<Boolean> getRollingUpgradePreserveScale() {
    return property(Boolean.class);
  }

  @Override
  public Property<Boolean> getFailOnNoKubernetesJson() {
    return property(Boolean.class).value(isFailOnNoKubernetesJson);
  }

  @Override
  public Property<Boolean> getServicesOnly() {
    return property(Boolean.class);
  }

  @Override
  public Property<Boolean> getIgnoreServices() {
    return property(Boolean.class);
  }

  @Override
  public Property<Boolean> getDeletePodsOnReplicationControllerUpdate() {
    return property(Boolean.class);
  }

  @Override
  public Property<File> getJsonLogDir() {
    return property(File.class);
  }

  @Override
  public Property<Integer> getServiceUrlWaitTimeSeconds() {
    return property(Integer.class);
  }

  @Override
  public Property<Boolean> getSkipPush() {
    return property(Boolean.class).value(isSkipPush);
  }

  @Override
  public Property<String> getPushRegistry() {
    return property(String.class);
  }

  @Override
  public Property<Boolean> getSkipTag() {
    return property(Boolean.class);
  }

  @Override
  public Property<Integer> getPushRetries() {
    return property(Integer.class);
  }

  @Override
  public Property<Integer> getLocalDebugPort() {
    return property(Integer.class);
  }

  @Override
  public Property<Boolean> getDebugSuspend() {
    return property(Boolean.class);
  }

  @Override
  public Property<File> getOpenShiftManifest() {
    return property(File.class);
  }

  @Override
  public Property<String> getOpenshiftPullSecret() {
    return property(String.class);
  }

  @Override
  public Property<String> getS2iBuildNameSuffix() {
    return property(String.class);
  }

  @Override
  public Property<Boolean> getS2iImageStreamLookupPolicyLocal() {
    return property(Boolean.class);
  }

  @Override
  public Property<String> getBuildOutputKind() {
    return property(String.class);
  }

  @Override
  public Property<String> getOpenshiftPushSecret() {
    return property(String.class);
  }

  @Override
  public Property<File> getImageStreamManifest() {
    return property(File.class);
  }

  @Override
  public Property<File> getKubernetesTemplate() {
    return property(File.class);
  }

  @Override
  public Property<Boolean> getSkipResource() {
    return property(Boolean.class);
  }

  @Override
  public Property<Boolean> getSkipBuild() {
    return property(Boolean.class);
  }

  @Override
  public Property<Integer> getWatchInterval() {
    return property(Integer.class);
  }

  @Override
  public Property<String> getWatchPostExec() {
    return property(String.class);
  }

  public static <T> Property<T> property(Class<T> type) {
    return new DefaultProperty<>(PropertyHost.NO_OP, type);
  }
}
