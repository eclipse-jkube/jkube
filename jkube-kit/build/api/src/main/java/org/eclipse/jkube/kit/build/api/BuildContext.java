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
package org.eclipse.jkube.kit.build.api;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;


/**
 * @author roland
 * @since 16.10.18
 */
public interface BuildContext {

    String getSourceDirectory();

    File getBasedir();

    String getOutputDirectory();

    Properties getProperties();

    File createImageContentArchive(String imageName, BuildConfiguration buildConfig, KitLogger log) throws IOException;

    File inSourceDir(String path);

    File inOutputDir(String path);

    File inDir(String dir, String path);
}
