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
package org.eclipse.jkube.kit.build.service.docker;

import org.eclipse.jkube.kit.config.image.build.ImagePullPolicy;
import org.junit.Test;

import java.util.Properties;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ImagePullManagerTest {
  @Test
  public void testCreateImagePullManagerWithNotNullImagePullPolicy() {
    // Given + When
    ImagePullManager imagePullManager = ImagePullManager.createImagePullManager("Always", null, new Properties());

    // Then
    assertThat(imagePullManager)
        .hasFieldOrPropertyWithValue("imagePullPolicy", ImagePullPolicy.Always)
        .extracting("cacheStore").isNotNull();
  }

  @Test
  public void testCreateImagePullManagerWithAutoPullModeOnce() {
    // Given + When
    ImagePullManager imagePullManager = ImagePullManager.createImagePullManager(null, "always", new Properties());

    // Then
    assertThat(imagePullManager)
        .hasFieldOrPropertyWithValue("imagePullPolicy", ImagePullPolicy.Always)
        .extracting("cacheStore").isNotNull();
  }

  @Test
  public void testCreateImagePullManagerWithAutoPullModeOff() {
    // Given + When
    ImagePullManager imagePullManager = ImagePullManager.createImagePullManager(null, "off", new Properties());

    // Then
    assertThat(imagePullManager)
        .hasFieldOrPropertyWithValue("imagePullPolicy", ImagePullPolicy.Never)
        .extracting("cacheStore").isNotNull();
  }

  @Test
  public void testCreateImagePullManagerWithAutoPullModeAlways() {
    // Given + When
    ImagePullManager imagePullManager = ImagePullManager.createImagePullManager(null, "always", new Properties());

    // Then
    assertThat(imagePullManager)
        .hasFieldOrPropertyWithValue("imagePullPolicy", ImagePullPolicy.Always)
        .extracting("cacheStore").isNotNull();
  }

  @Test
  public void testCreateImagePullManagerWithNullImagePullPolicyNullAutoPullMode() {
    // Given + When
    ImagePullManager imagePullManager = ImagePullManager.createImagePullManager(null, null, new Properties());

    // Then
    assertThat(imagePullManager)
        .hasFieldOrPropertyWithValue("imagePullPolicy", ImagePullPolicy.IfNotPresent)
        .extracting("cacheStore").isNotNull();
  }

}