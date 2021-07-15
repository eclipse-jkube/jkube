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

import org.eclipse.jkube.kit.config.access.ClusterConfiguration;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;

import groovy.lang.Closure;
import org.gradle.api.provider.Property;

import java.util.List;

import static org.eclipse.jkube.gradle.plugin.GroovyUtil.closureTo;
import static org.eclipse.jkube.gradle.plugin.GroovyUtil.namedListClosureTo;

/**
 * <pre>{@code
 * kubernetes {
 *     useColor = false
 *     offline = false
 *     access {
 *         namespace = 'default'
 *     }
 *     enricher {
 *         excludes = ['jkube-expose']
 *     }
 *     resources {
 *         controllerName ='test'
 *         configMap {
 *             name = 'configMap-name'
 *             entries = [
 *                 [name: 'test', value: 'value']
 *             ]
 *         }
 *     }
 *     images {
 *         image2 { name = 'registry/image2:tag' }
 *         image1 {
 *             name = 'registry/image:tag'
 *             build {
 *                 from = 'busybox'
 *             }
 *         }
 *     }
 * }
 *
 * openshift {
 *  useColor = true
 * }
 * }</pre>
 *
 * Nested config
 */
@SuppressWarnings({"java:S1104", "java:S1845"})
public abstract class KubernetesExtension {

  public static final boolean DEFAULT_OFFLINE = false;

  public abstract Property<Boolean> getOffline();

  public abstract Property<Boolean> getUseColor();

  public ClusterConfiguration access;

  public ResourceConfig resources;

  public ProcessorConfig enricher;

  public List<ImageConfiguration> images;

  public RuntimeMode getRuntimeMode() {
    return RuntimeMode.KUBERNETES;
  }

  public void access(Closure<?> closure) {
    access = closureTo(closure, ClusterConfiguration.class);
  }

  public void resources(Closure<?> closure) {
    resources = closureTo(closure, ResourceConfig.class);
  }

  public void enricher(Closure<?> closure) {
    enricher = closureTo(closure, ProcessorConfig.class);
  }

  public void images(Closure<?> closure) {
    images = namedListClosureTo(closure, ImageConfiguration.class);
  }

}
