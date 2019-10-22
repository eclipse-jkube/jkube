package io.jkube.kit.build.service.docker;

import io.jkube.kit.build.api.model.VolumeCreateConfig;
import io.jkube.kit.build.service.docker.access.DockerAccess;
import io.jkube.kit.build.service.docker.access.DockerAccessException;
import io.jkube.kit.build.service.docker.config.VolumeConfiguration;

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
