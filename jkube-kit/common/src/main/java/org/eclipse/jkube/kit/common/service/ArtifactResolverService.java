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
package org.eclipse.jkube.kit.common.service;

import java.io.File;

/**
 * Allows retrieving artifacts from a Maven repo.
 */
public interface ArtifactResolverService {

  File resolveArtifact(String groupId, String artifactId, String version, String type);

}
