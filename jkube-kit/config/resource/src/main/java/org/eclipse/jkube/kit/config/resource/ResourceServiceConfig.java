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
package org.eclipse.jkube.kit.config.resource;

import java.io.File;
import java.util.List;

import lombok.Singular;
import org.eclipse.jkube.kit.common.JavaProject;
import org.eclipse.jkube.kit.common.ResourceFileType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Class to hold configuration parameters for the Resource service.
 */
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class ResourceServiceConfig {

  private JavaProject project;
  @Singular
  private List<File> resourceDirs;
  private File targetDir;
  private ResourceFileType resourceFileType;
  private ResourceConfig resourceConfig;
  private ResourceService.ResourceFileProcessor resourceFilesProcessor;
  private boolean interpolateTemplateParameters;

}
