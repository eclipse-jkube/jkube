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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jkube.kit.build.service.docker.config.DockerMachineConfiguration;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.ResourceFileType;
import org.eclipse.jkube.kit.common.util.ResourceUtil;
import org.eclipse.jkube.kit.config.access.ClusterConfiguration;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.build.JKubeBuildStrategy;
import org.eclipse.jkube.kit.config.image.build.RegistryAuthConfiguration;
import org.eclipse.jkube.kit.config.resource.MappingConfig;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.eclipse.jkube.kit.config.resource.ResourceConfig;
import org.eclipse.jkube.kit.config.resource.RuntimeMode;

import groovy.lang.Closure;
import org.gradle.api.provider.Property;

import static org.eclipse.jkube.gradle.plugin.GroovyUtil.closureTo;
import static org.eclipse.jkube.gradle.plugin.GroovyUtil.invokeOrParseClosureList;

/**
 * <pre>
 * {@code
 * kubernetes {
 *     useColor = false
 *     offline = false
 *     buildStrategy = 'jib'
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
 *                 assembly {
 *                   layers = [
 *                     {
 *                       id = "custom-assembly-for-micronaut"
 *                       files = [
 *                         {
 *                           source = file('.')
 *                           outputDirectory = '.'
 *                         },
 *                         [source: file('other'), outputDirectory: file('output')]
 *                       ]
 *                     }
 *                   ]
 *                 }
 *             }
 *         }
 *     }
 * }
 *
 * openshift {
 *  useColor = true
 * }
 * }
 * </pre>
 *
 * Nested config
 */
@SuppressWarnings({"java:S1104", "java:S1845"})
public abstract class KubernetesExtension {

  private static final String DEFAULT_RESOURCE_SOURCE_DIR = "src/main/jkube";
  private static final String DEFAULT_RESOURCE_TARGET_DIR = "META-INF/jkube";
  public static final boolean DEFAULT_OFFLINE = false;

  public abstract Property<Boolean> getOffline();

  public abstract Property<Boolean> getUseColor();

  public abstract Property<Integer> getMaxConnections();

  public abstract Property<String> getFilter();

  public abstract Property<String> getApiVersion();

  public abstract Property<String> getBuildRecreate();

  public abstract Property<String> getImagePullPolicy();

  public abstract Property<String> getAutoPull();

  public abstract Property<String> getDockerHost();

  public abstract Property<String> getCertPath();

  public abstract Property<String> getMinimalApiVersion();

  public abstract Property<Boolean> getSkipMachine();

  public abstract Property<Boolean> getForcePull();

  public abstract Property<Boolean> getSkipExtendedAuth();

  public abstract Property<String> getPullRegistry();

  public abstract Property<String> getBuildSourceDirectory();

  public abstract Property<String> getBuildOutputDirectory();

  public abstract Property<String> getRegistry();

  public abstract Property<File> getResourceTargetDirectory();

  public abstract Property<File> getResourceSourceDirectory();

  public abstract Property<String> getResourceEnvironment();

  public abstract Property<Boolean> getUseProjectClassPath();

  public abstract Property<File> getWorkDirectory();

  public abstract Property<Boolean> getSkipResourceValidation();

  public abstract Property<Boolean> getFailOnValidationError();

  public abstract Property<String> getProfile();

  public abstract Property<String> getNamespace();

  public abstract Property<Boolean> getMergeWithDekorate();

  public abstract Property<Boolean> getInterpolateTemplateParameters();

  public abstract Property<Boolean> getSkip();

  public JKubeBuildStrategy buildStrategy;

  public ClusterConfiguration access;

  public ResourceConfig resources;

  public ProcessorConfig enricher;

  public ProcessorConfig generator;

  public List<ImageConfiguration> images;

  public DockerMachineConfiguration machine;

  public RegistryAuthConfiguration authConfig;

  public List<MappingConfig> mappings;

  public ResourceFileType resourceFileType;

  public RuntimeMode getRuntimeMode() {
    return RuntimeMode.KUBERNETES;
  }

  public JKubeBuildStrategy getBuildStrategy() {
    return buildStrategy != null ? buildStrategy : JKubeBuildStrategy.docker;
  }

  public PlatformMode getPlatformMode() {
    return PlatformMode.kubernetes;
  }

  public ResourceFileType getResourceFileType() {
    return resourceFileType != null ? resourceFileType : ResourceFileType.yaml;
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

  public void generator(Closure<?> closure) {
    generator = closureTo(closure, ProcessorConfig.class);
  }

  /**
   * Provide support for image configurations as:
   *
   * <pre>
   * {@code
   * images {
   *   image1 {...}
   *   image2 {...}
   * }
   * }
   * </pre>
   *
   * @param closure The closure to unmarshal containing a Map of ImageConfiguration
   */
  public void images(Closure<?> closure) {
    invokeOrParseClosureList(closure, ImageConfiguration.class).ifPresent(i -> this.images = i);
  }

  /**
   * Provide support for image configurations as:
   *
   * <pre>
   * {@code
   * images ([
   *   {...},
   *   {...}
   * ])
   * }
   * </pre>
   *
   * @param closures The list of closures to unmarshal with individual ImageConfiguration
   */
  public void images(List<Closure<?>> closures) {
    images = closures.stream().map(c -> closureTo(c, ImageConfiguration.class)).collect(Collectors.toList());
  }

  /**
   * Provide support for image configurations as:
   *
   * <pre>
   * {@code
   * images {
   *   image {...}
   *   image {...}
   * }
   * }
   * </pre>
   *
   * @param closure The closure to unmarshal with an ImageConfiguration
   */
  public void image(Closure<?> closure) {
    if (images == null) {
      images = new ArrayList<>();
    }
    images.add(closureTo(closure, ImageConfiguration.class));
  }

  public void machine(Closure<?> closure) {
    machine = closureTo(closure, DockerMachineConfiguration.class);
  }

  public void authConfig(Closure<?> closure) {
    authConfig = closureTo(closure, RegistryAuthConfiguration.class);
  }

  public void mappings(Closure<?> closure) {
    invokeOrParseClosureList(closure, MappingConfig.class).ifPresent(i -> this.mappings = i);
  }

  public void mappings(List<Closure<?>> closures) {
    mappings = closures.stream().map(c -> closureTo(c, MappingConfig.class)).collect(Collectors.toList());
  }

  public void mapping(Closure<?> closure) {
    if (mappings == null) {
      mappings = new ArrayList<>();
    }
    mappings.add(closureTo(closure, MappingConfig.class));
  }

  public File getResourceSourceDirectoryOrDefault(JavaProject javaProject) {
    return getResourceSourceDirectory().getOrElse(new File(javaProject.getBaseDirectory(), DEFAULT_RESOURCE_SOURCE_DIR));
  }

  public File getResourceTargetDirectoryOrDefault(JavaProject javaProject) {
    return getResourceTargetDirectory().getOrElse(new File(javaProject.getBuildDirectory(), DEFAULT_RESOURCE_TARGET_DIR));
  }

  public boolean getUseProjectClassPathOrDefault() {
    return getUseProjectClassPath().getOrElse(false);
  }

  public boolean getFailOnValidationErrorOrDefault() {
    return getFailOnValidationError().getOrElse(false);
  }

  public boolean getMergeWithDekorateOrDefault() {
    return getMergeWithDekorate().getOrElse(false);
  }

  public boolean getInterpolateTemplateParametersOrDefault() {
    return getInterpolateTemplateParameters().getOrElse(true);
  }

  public boolean getSkipResourceValidationOrDefault() {
    return getSkipResourceValidation().getOrElse(false);
  }

  public File getResourceDirectory(JavaProject javaProject) {
    return ResourceUtil.getFinalResourceDir(
      getResourceSourceDirectoryOrDefault(javaProject),
      getResourceEnvironment().getOrNull()
    );
  }

}
