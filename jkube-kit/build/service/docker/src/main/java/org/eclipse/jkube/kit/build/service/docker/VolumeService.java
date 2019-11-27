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

import org.eclipse.jkube.kit.build.api.model.VolumeCreateConfig;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccessException;
import org.eclipse.jkube.kit.build.service.docker.config.VolumeConfiguration;

import java.lang.String;

/**
 *  Service Class for helping control Volumes
 *
 *  @author Tom Burton
 *  @version Dec 15, 2016
 */
public class VolumeService {
   // DAO for accessing the docker daemon
   private DockerAccess docker;

   VolumeService(DockerAccess dockerAccess) {
      this.docker = dockerAccess;
   }

   public String createVolume(VolumeConfiguration vc) throws DockerAccessException {
      VolumeCreateConfig config =
          new VolumeCreateConfig(vc.getName())
              .driver(vc.getDriver())
              .opts(vc.getOpts())
              .labels(vc.getLabels());

      return docker.createVolume(config);
   }

   public void removeVolume(String volumeName) throws DockerAccessException {
      docker.removeVolume(volumeName);
   }
}
