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
package org.eclipse.jkube.kit.config.service;

import org.eclipse.jkube.kit.config.resource.ResourceConfig;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface UndeployService {

  void undeploy(List<File> resourceDir, ResourceConfig resourceConfig, File... manifestFiles) throws IOException;
}
