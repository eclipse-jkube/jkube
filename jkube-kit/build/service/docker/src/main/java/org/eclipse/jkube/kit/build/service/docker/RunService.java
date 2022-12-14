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

import org.eclipse.jkube.kit.build.api.model.ExecDetails;
import org.eclipse.jkube.kit.build.api.model.Container;
import org.eclipse.jkube.kit.build.api.model.ContainerDetails;
import org.eclipse.jkube.kit.build.api.model.Network;
import org.eclipse.jkube.kit.build.api.model.NetworkCreateConfig;
import org.eclipse.jkube.kit.build.core.GavLabel;
import org.eclipse.jkube.kit.build.service.docker.access.ContainerCreateConfig;
import org.eclipse.jkube.kit.build.service.docker.access.ContainerHostConfig;
import org.eclipse.jkube.kit.build.service.docker.access.ContainerNetworkingConfig;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccessException;
import org.eclipse.jkube.kit.build.service.docker.access.ExecException;
import org.eclipse.jkube.kit.build.api.model.PortMapping;
import org.eclipse.jkube.kit.build.service.docker.access.log.LogOutputSpecFactory;
import org.eclipse.jkube.kit.config.image.NetworkConfig;
import org.eclipse.jkube.kit.config.image.RestartPolicy;
import org.eclipse.jkube.kit.config.image.RunImageConfiguration;
import org.eclipse.jkube.kit.config.image.RunVolumeConfiguration;
import org.eclipse.jkube.kit.build.service.docker.config.VolumeConfiguration;
import org.eclipse.jkube.kit.build.service.docker.helper.ContainerNamingUtil;
import org.eclipse.jkube.kit.build.service.docker.helper.StartOrderResolver;
import org.eclipse.jkube.kit.build.service.docker.wait.WaitTimeoutException;
import org.eclipse.jkube.kit.build.service.docker.wait.WaitUtil;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.common.util.EnvUtil;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.common.Arguments;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import static org.eclipse.jkube.kit.build.service.docker.helper.VolumeBindingUtil.resolveRelativeVolumeBindings;


/**
 * Service class for helping in running containers.
 *
 * @author roland
 * @since 16/06/15
 */
public class RunService {

    // logger delegated from top
    private final KitLogger log;

    // Action to be used when doing a shutdown
    private final ContainerTracker tracker;

    // DAO for accessing the docker daemon
    private final DockerAccess docker;

    private final QueryService queryService;

    private final LogOutputSpecFactory logConfig;

    public RunService(DockerAccess docker,
                      QueryService queryService,
                      ContainerTracker tracker,
                      LogOutputSpecFactory logConfig,
                      KitLogger log) {
        this.docker = docker;
        this.queryService = queryService;
        this.tracker = tracker;
        this.log = log;
        this.logConfig = logConfig;
    }

    /**
     * Create and start a Exec container with the given image configuration.
     * @param containerId container id to run exec command against
     * @param command command to execute
     * @param imageConfiguration configuration of the container's image
     * @return the exec container id
     *
     * @throws DockerAccessException if access to the docker backend fails
     * @throws ExecException if any problem faced during exec
     */
    public String execInContainer(String containerId, String command, ImageConfiguration imageConfiguration)
        throws DockerAccessException, ExecException {
        Arguments arguments = new Arguments();
        arguments.setExec(Arrays.asList(EnvUtil.splitOnSpaceWithEscape(command)));
        String execContainerId = docker.createExecContainer(containerId, arguments);
        docker.startExecContainer(execContainerId, logConfig.createSpec(containerId, imageConfiguration));

        ExecDetails execContainer = docker.getExecContainer(execContainerId);
        Integer exitCode = execContainer.getExitCode();
        if (exitCode != null && exitCode != 0) {
            ContainerDetails container = docker.getContainer(containerId);
            throw new ExecException(execContainer, container);
        }
        return execContainerId;
    }

    /**
     * Create and start a container with the given image configuration.
     * @param imageConfig image configuration holding the run information and the image name
     * @param portMapping container port mapping
     * @param gavLabel label to tag the started container with
     * @param baseDir base directory
     * @param properties properties to fill in with dynamically assigned ports
     * @param defaultContainerNamePattern pattern to use for naming containers. Can be null in which case a default pattern is used
     * @param buildTimestamp date which should be used as the timestamp when calculating container names
     * @return the container id
     *
     * @throws DockerAccessException if access to the docker backend fails
     */
    public String createAndStartContainer(ImageConfiguration imageConfig,
                                          PortMapping portMapping,
                                          GavLabel gavLabel,
                                          Properties properties,
                                          File baseDir,
                                          String defaultContainerNamePattern,
                                          Date buildTimestamp) throws DockerAccessException {
        RunImageConfiguration runConfig = imageConfig.getRunConfiguration();
        String imageName = imageConfig.getName();

        Collection<Container> existingContainers = queryService.getContainersForImage(imageName, true);
        String containerName = ContainerNamingUtil.formatContainerName(imageConfig, defaultContainerNamePattern, buildTimestamp, existingContainers);

        ContainerCreateConfig config = createContainerConfig(imageName, runConfig, portMapping, gavLabel, properties, baseDir);

        String id = docker.createContainer(config, containerName);
        startContainer(imageConfig, id, gavLabel);

        if (portMapping.needsPropertiesUpdate()) {
            updateMappedPortsAndAddresses(id, portMapping);
        }

        return id;
    }

    /**
     * Stop a container immediately by id.
     * @param containerId the container to stop
     * @param imageConfig image configuration for this container
     * @param keepContainer whether to keep container or to remove them after stopping
     * @param removeVolumes whether to remove volumes after stopping
     * @throws DockerAccessException docker access exception
     * @throws ExecException exec exception
     */
    public void stopContainer(String containerId,
                              ImageConfiguration imageConfig,
                              boolean keepContainer,
                              boolean removeVolumes)
        throws DockerAccessException, ExecException {
        ContainerTracker.ContainerShutdownDescriptor descriptor =
                new ContainerTracker.ContainerShutdownDescriptor(imageConfig, containerId);
        shutdown(descriptor, keepContainer, removeVolumes);
    }

    /**
     * Lookup up whether a certain has been already started and registered. If so, stop it
     *
     * @param containerId the container to stop
     * @param keepContainer whether to keep container or to remove them after stoppings
     * @param removeVolumes whether to remove volumes after stopping
     *
     * @throws DockerAccessException docker access exception
     * @throws ExecException exec exception
     */
    public void stopPreviouslyStartedContainer(String containerId,
                                               boolean keepContainer,
                                               boolean removeVolumes)
        throws DockerAccessException, ExecException {
        ContainerTracker.ContainerShutdownDescriptor descriptor = tracker.removeContainer(containerId);
        if (descriptor != null) {
            shutdown(descriptor, keepContainer, removeVolumes);
        }
    }

    /**
     * Stop all registered container
     *
     * @param keepContainer whether to keep container or to remove them after stopping
     * @param removeVolumes whether to remove volumes after stopping
     * @param removeCustomNetworks whether to remove custom networks
     * @param gavLabel group artifact version label
     * @throws DockerAccessException if during stopping of a container sth fails
     * @throws ExecException if any problem during exec
     */
    public void stopStartedContainers(boolean keepContainer,
                                      boolean removeVolumes,
                                      boolean removeCustomNetworks,
                                      GavLabel gavLabel)
        throws DockerAccessException, ExecException {
        Set<Network> networksToRemove = new HashSet<>();
        for (ContainerTracker.ContainerShutdownDescriptor descriptor : tracker.removeShutdownDescriptors(gavLabel)) {
            collectCustomNetworks(networksToRemove, descriptor, removeCustomNetworks);
            shutdown(descriptor, keepContainer, removeVolumes);
        }
        removeCustomNetworks(networksToRemove);
    }

    private void collectCustomNetworks(Set<Network> networksToRemove, ContainerTracker.ContainerShutdownDescriptor descriptor, boolean removeCustomNetworks) throws DockerAccessException {
        final NetworkConfig config = descriptor.getImageConfiguration().getRunConfiguration().getNetworkingConfig();
        if (removeCustomNetworks && config.isCustomNetwork()) {
           networksToRemove.add(queryService.getNetworkByName(config.getCustomNetwork()));
        }
    }

    /**
     * Lookup a container that has been started
     *
     * @param lookup a container by id or alias
     * @return the container id if the container exists, <code>null</code> otherwise.
     */
    public String lookupContainer(String lookup) {
        return tracker.lookupContainer(lookup);
    }

    /**
     * Get the proper order for images to start
     *
     * @param queryService query service
     * @param images list of images for which the order should be created
     * @return list of images in the right startup order
     */
    public List<ImageConfiguration> getImagesConfigsInOrder(QueryService queryService, List<ImageConfiguration> images) {
        return StartOrderResolver.resolve(queryService, convertToResolvables(images));
    }

    /**
     * Create port mapping for a specific configuration as it can be used when creating containers
     *
     * @param runConfig the cun configuration
     * @param properties properties to lookup variables
     * @return the portmapping
     */
    public PortMapping createPortMapping(RunImageConfiguration runConfig, Properties properties) {
        try {
            return new PortMapping(runConfig.getPorts(), properties);
        } catch (IllegalArgumentException exp) {
            throw new IllegalArgumentException("Cannot parse port mapping", exp);
        }
    }

    /**
     * Add a shutdown hook in order to stop all registered containers
     *
     * @param keepContainer whether to keep container or not
     * @param removeVolumes whether to remove volumes or not
     * @param removeCustomNetworks whether to remove custom networks or not
     */
    public void addShutdownHookForStoppingContainers(final boolean keepContainer, final boolean removeVolumes, final boolean removeCustomNetworks) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    stopStartedContainers(keepContainer, removeVolumes, removeCustomNetworks, null);
                } catch (DockerAccessException | ExecException e) {
                    log.error("Error while stopping containers: %s", e.getMessage());
                }
            }
        });
    }

    private List<ImageConfiguration> convertToResolvables(List<ImageConfiguration> images) {
        List<ImageConfiguration> ret = new ArrayList<>();
        for (ImageConfiguration config : images) {
            if (config.getRunConfiguration().skip()) {
                log.info("%s: Skipped running", config.getDescription());
            } else {
                ret.add(config);
            }
        }
        return ret;
    }

    // visible for testing
    ContainerCreateConfig createContainerConfig(String imageName, RunImageConfiguration runConfig, PortMapping mappedPorts,
                                                GavLabel gavLabel, Properties mavenProps, File baseDir)
            throws DockerAccessException {
        try {
            ContainerCreateConfig config = new ContainerCreateConfig(imageName)
                    .hostname(runConfig.getHostname())
                    .domainname(runConfig.getDomainname())
                    .user(runConfig.getUser())
                    .workingDir(runConfig.getWorkingDir())
                    .entrypoint(runConfig.getEntrypoint())
                    .exposedPorts(mappedPorts.getContainerPorts())
                    .environment(runConfig.getEnvPropertyFile(), runConfig.getEnv(), mavenProps)
                    .labels(mergeLabels(runConfig.getLabels(), gavLabel))
                    .command(runConfig.getCmd())
                    .hostConfig(createContainerHostConfig(runConfig, mappedPorts, baseDir));
            RunVolumeConfiguration volumeConfig = runConfig.getVolumeConfiguration();
            if (volumeConfig != null) {
                resolveRelativeVolumeBindings(baseDir, volumeConfig);
                config.binds(volumeConfig.getBind());
            }

            NetworkConfig networkConfig = runConfig.getNetworkingConfig();
            if(networkConfig.isCustomNetwork() && networkConfig.hasAliases()) {
                ContainerNetworkingConfig networkingConfig =
                    new ContainerNetworkingConfig()
                        .aliases(networkConfig);
                config.networkingConfig(networkingConfig);
            }

            return config;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("Failed to create contained configuration for [%s]", imageName), e);
        }
    }

    private Map<String, String> mergeLabels(Map<String, String> labels, GavLabel runIdLabel) {
        Map<String,String> ret = new HashMap<>();
        if (labels != null) {
            ret.putAll(labels);
        }
        if (runIdLabel != null) {
            ret.put(GavLabel.KEY, runIdLabel.getValue());
        }
        return ret;
    }

    ContainerHostConfig createContainerHostConfig(RunImageConfiguration runConfig, PortMapping mappedPorts, File baseDir)
            throws DockerAccessException {
        RestartPolicy restartPolicy = runConfig.getRestartPolicy();


        List<String> links = findContainerIdsForLinks(runConfig.getLinks(),
                                                      runConfig.getNetworkingConfig().isCustomNetwork());

        ContainerHostConfig config = new ContainerHostConfig()
                .extraHosts(runConfig.getExtraHosts())
                .links(links)
                .portBindings(mappedPorts)
                .privileged(runConfig.getPrivileged())
                .shmSize(runConfig.getShmSize())
                .dns(runConfig.getDns())
                .dnsSearch(runConfig.getDnsSearch())
                .capAdd(runConfig.getCapAdd())
                .capDrop(runConfig.getCapDrop())
                .securityOpts(runConfig.getSecurityOpts())
                .memory(runConfig.getMemory())
                .memorySwap(runConfig.getMemorySwap())
                .restartPolicy(restartPolicy.getName(), restartPolicy.getRetry())
                .logConfig(runConfig.getLog())
                .tmpfs(runConfig.getTmpfs())
                .ulimits(runConfig.getUlimits())
                .cpuShares(runConfig.getCpuShares())
                .cpus(runConfig.getCpus())
                .cpuSet(runConfig.getCpuSet())
                .readonlyRootfs(runConfig.getReadOnly())
                .autoRemove(runConfig.getAutoRemove());

        addVolumeConfig(config, runConfig, baseDir);
        addNetworkingConfig(config, runConfig);

        return config;
    }

    private void addNetworkingConfig(ContainerHostConfig config, RunImageConfiguration runConfig) throws DockerAccessException {
        NetworkConfig networkConfig = runConfig.getNetworkingConfig();
        if (networkConfig.isStandardNetwork()) {
            String alias = networkConfig.getContainerAlias();
            String containerId = alias != null ? findContainerId(alias, false) : null;
            config.networkMode(networkConfig.getStandardMode(containerId));
        } else if (networkConfig.isCustomNetwork()) {
            config.networkMode(networkConfig.getCustomNetwork());
        }
    }

    private void addVolumeConfig(ContainerHostConfig config, RunImageConfiguration runConfig, File baseDir) throws DockerAccessException {
        RunVolumeConfiguration volConfig = runConfig.getVolumeConfiguration();
        if (volConfig != null) {
            resolveRelativeVolumeBindings(baseDir, volConfig);
            config.binds(volConfig.getBind())
                  .volumesFrom(findVolumesFromContainers(volConfig.getFrom()));
        }
    }

    private List<String> findContainerIdsForLinks(List<String> links, boolean leaveUnresolvedIfNotFound) throws DockerAccessException {
        List<String> ret = new ArrayList<>();
        for (String[] link : EnvUtil.splitOnLastColon(links)) {
            String id = findContainerId(link[0], false);
            if (id != null) {
                ret.add(queryService.getContainerName(id) + ":" + link[1]);
            } else if (leaveUnresolvedIfNotFound) {
                ret.add(link[0] + ":" + link[1]);
            } else {
                throw new DockerAccessException("No container found for image/alias '%s', unable to link", link[0]);
            }
        }
        return !ret.isEmpty() ? ret : null;
    }

    // visible for testing
    private List<String> findVolumesFromContainers(List<String> images) throws DockerAccessException {
        List<String> list = new ArrayList<>();
        if (images != null) {
            for (String image : images) {
                String id = findContainerId(image, true);
                if (id == null) {
                    throw new DockerAccessException("No container found for image/alias '%s', unable to mount volumes", image);
                }

                list.add(queryService.getContainerName(id));
            }
        }
        return list;
    }


    // checkAllContainers: false = only running containers are considered
    private String findContainerId(String imageNameOrAlias, boolean checkAllContainers) throws DockerAccessException {
        String id = lookupContainer(imageNameOrAlias);

        // check for external container. The image name is interpreted as a *container name* for that case ...
        if (id == null) {
            Container container = queryService.getContainer(imageNameOrAlias);
            if (container != null && (checkAllContainers || container.isRunning())) {
                id = container.getId();
            }
        }
        return id;
    }

    private void startContainer(ImageConfiguration imageConfig, String id, GavLabel gavLabel) throws DockerAccessException {
        log.info("%s: Start container %s",imageConfig.getDescription(), id);
        docker.startContainer(id);
        tracker.registerContainer(id, imageConfig, gavLabel);
    }

    private void updateMappedPortsAndAddresses(String containerId, PortMapping mappedPorts) throws DockerAccessException {
        Container container = queryService.getMandatoryContainer(containerId);
        if (container.isRunning()) {
            mappedPorts.updateProperties(container.getPortBindings());
        } else {
            log.warn("Container %s is not running anymore, can not extract dynamic ports",containerId);
        }
    }

    private void shutdown(ContainerTracker.ContainerShutdownDescriptor descriptor, boolean keepContainer, boolean removeVolumes)
        throws DockerAccessException, ExecException {

        String containerId = descriptor.getContainerId();
        if (descriptor.getPreStop() != null) {
            try {
                execInContainer(containerId, descriptor.getPreStop(), descriptor.getImageConfiguration());
            } catch (DockerAccessException e) {
                log.error("%s", e.getMessage());
            } catch (ExecException e) {
                if (descriptor.isBreakOnError()) {
                    throw e;
                } else {
                    log.warn("Cannot run preStop: %s", e.getMessage());
                }
            }
        }

        int killGracePeriod = adjustGracePeriod(descriptor.getKillGracePeriod());
        log.debug("shutdown will wait max of %d seconds before removing container", killGracePeriod);

        long waited;
        if (killGracePeriod == 0) {
            docker.stopContainer(containerId, 0);
            waited = 0;
        } else {
            waited = shutdownAndWait(containerId, killGracePeriod);
        }
        if (!keepContainer) {
            removeContainer(descriptor, removeVolumes, containerId);
        }

        log.info("%s: Stop%s container %s after %s ms",
                descriptor.getDescription(),
                (keepContainer ? "" : " and removed"),
                containerId.substring(0, 12), waited);
    }

    public void createCustomNetworkIfNotExistant(String customNetwork) throws DockerAccessException {
        if (!queryService.hasNetwork(customNetwork)) {
            docker.createNetwork(new NetworkCreateConfig(customNetwork));
        } else {
            log.debug("Custom Network " + customNetwork + " found");
        }
    }

    public void removeCustomNetworks(Collection<Network> networks) throws DockerAccessException {
        for (Network network : networks) {
            docker.removeNetwork(network.getId());
        }
    }

    private int adjustGracePeriod(int gracePeriod) {
        int killGracePeriodInSeconds = (gracePeriod + 500) / 1000;
        if (gracePeriod != 0 && killGracePeriodInSeconds == 0) {
            log.warn("A kill grace period of %d ms leads to no wait at all since its rounded to seconds. " +
                     "Please use at least 500 as value for wait.kill", gracePeriod);
        }

        return killGracePeriodInSeconds;
    }

    private void removeContainer(ContainerTracker.ContainerShutdownDescriptor descriptor, boolean removeVolumes, String containerId)
        throws DockerAccessException {
        int shutdownGracePeriod = descriptor.getShutdownGracePeriod();
        if (shutdownGracePeriod != 0) {
            log.debug("Shutdown: Wait %d ms before removing container", shutdownGracePeriod);
            WaitUtil.sleep(shutdownGracePeriod);
        }
        // Remove the container
        docker.removeContainer(containerId, removeVolumes);
    }

    private long shutdownAndWait(final String containerId, final int killGracePeriodInSeconds) throws DockerAccessException {
        long waited;
        try {
            waited = WaitUtil.wait(killGracePeriodInSeconds, new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    docker.stopContainer(containerId, killGracePeriodInSeconds);
                    return null;
                }
            });
        } catch (ExecutionException e) {
            if (e.getCause() instanceof DockerAccessException) {
                throw (DockerAccessException) e.getCause();
            } else {
                throw new DockerAccessException(e, "failed to stop container id [%s]", containerId);
            }
        } catch (WaitTimeoutException e) {
            waited = e.getWaited();
            log.warn("Stop container id [%s] timed out after %s ms", containerId, waited);
        }

        return waited;
    }

    /**
     * Creates a Volume if a volume is referred to during startup in bind mount mapping and
     * a VolumeConfiguration exists
     *
     * @param hub Service hub
     * @param binds volume binds present in ImageConfiguration
     * @param volumes VolumeConfigs present
     * @return List of volumes created
     * @throws DockerAccessException docker access exception
     */
    public List<String> createVolumesAsPerVolumeBinds(DockerServiceHub hub, List<String> binds, List<VolumeConfiguration> volumes)
            throws DockerAccessException {

        Map<String, Integer> indexMap = new HashMap<>();
        List<String> volumesCreated = new ArrayList<>();

        for (int index = 0; index < volumes.size(); index++) {
            indexMap.put(volumes.get(index).getName(), index);
        }

        for (String bind : binds) {
            if (bind.contains(":")) {
                String name = bind.substring(0, bind.indexOf(':'));
                Integer volumeConfigIndex = indexMap.get(name);
                if (volumeConfigIndex != null) {
                    VolumeConfiguration volumeConfig = volumes.get(volumeConfigIndex);
                    hub.getVolumeService().createVolume(volumeConfig);
                    volumesCreated.add(volumeConfig.getName());
                }
            }
        }

        return volumesCreated;
    }
}
