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
package org.eclipse.jkube.api;

import java.io.File;

/**
 * JKube plugins are used by other services to perform additional actions.
 * <p>
 * Currently, JKubePlugins are used in the following services:
 * <ul>
 *   <li><strong>BuildService:</strong> {@link #addExtraFiles(File)}</li>
 * </ul>
 * <p>
 * JKubePlugins are automatically loaded by JKube by declaring a dependency to a module that
 * contains a descriptor file at <code>META-INF/jkube/plugin</code> with class names line by line,
 * for example:
 * <pre>{@code
 * com.example.project.MyJKubePlugin
 * }</pre>
 */
public interface JKubePlugin {

  String JKUBE_EXTRA_DIRECTORY = "jkube-extra";

  /**
   * Method called by JKube's build services with a single {@link File} argument.
   * <p>
   * The {@link File} argument points to a directory <code>jkube-extra</code> which can be easily
   * referenced by a Dockerfile or an Assembly configuration.
   * <p>
   * JKube plugins should create their own subdirectories under the provided <code>jkube-extra</code>
   * file location to avoid a clash with other JKube plugins.
   *
   * @param targetDir location of the <code>jkube-extra</code> directory where plugin can copy files to.
   */
  default void addExtraFiles(File targetDir) {
  }

}
