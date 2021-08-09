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

import java.util.Collections;

import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;

import groovy.lang.Closure;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({ "serial", "unused" })
public class KubernetesExtensionTest {

  @Test
  public void getRuntimeMode_withDefaults_shouldReturnKubernetes() {
    // Given
    final KubernetesExtension partial = mock(KubernetesExtension.class);
    when(partial.getRuntimeMode()).thenCallRealMethod();
    // When
    final RuntimeMode result = partial.getRuntimeMode();
    // Then
    assertThat(result).isEqualTo(RuntimeMode.KUBERNETES);
  }

  @Test
  public void images_withCallableClosure_shouldSetImagesInClosureExecution() {
    // Given
    final KubernetesExtension extension = new TestKubernetesExtension();
    // When
    extension.images(new Closure<Void>(extension) {
      public Void doCall(Object... args) {
        extension.image(new Closure<Void>(new ImageConfiguration()) {
          public Void doCall(Object... args) {
            setProperty("name", "closure/image:name");
            return null;
          }
        });
        return null;
      }
    });
    // Then
    assertThat(extension.images).singleElement()
        .hasFieldOrPropertyWithValue("name", "closure/image:name");
  }

  @Test
  public void images_withMapListClosure_shouldSetImagesUsingConfigObjectParsing() {
    // Given
    final KubernetesExtension extension = new TestKubernetesExtension();
    // When
    extension.images(new Closure<Void>(extension) {
      public Void doCall(Object... args) {
        setProperty("image1", new Closure<Void>(new ImageConfiguration()) {
          public Void doCall(Object... args) {
            setProperty("name", "closure/image1:name");
            return null;
          }
        });
        setProperty("image2", new Closure<Void>(new ImageConfiguration()) {
          public Void doCall(Object... args) {
            setProperty("name", "closure/image2:name");
            return null;
          }
        });
        return null;
      }
    });
    // Then
    assertThat(extension.images).hasSize(2)
        .extracting(ImageConfiguration::getName)
        .containsExactly("closure/image1:name", "closure/image2:name");
  }

  @Test
  public void images_withListOfClosures_shouldSetImages() {
    // Given
    final KubernetesExtension extension = new TestKubernetesExtension();
    // When
    extension.images(Collections.singletonList(new Closure<Void>(extension) {
      public Void doCall(Object... args) {
        setProperty("name", "closure/image:name");
        return null;
      }
    }));
    // Then
    assertThat(extension.images).singleElement()
        .hasFieldOrPropertyWithValue("name", "closure/image:name");
  }
}
