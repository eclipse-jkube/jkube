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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jkube.kit.build.core.GavLabel;
import org.eclipse.jkube.kit.config.JKubeConfiguration;
import org.eclipse.jkube.kit.build.core.assembly.AssemblyFiles;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccess;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccessException;
import org.eclipse.jkube.kit.build.service.docker.access.ExecException;
import org.eclipse.jkube.kit.build.service.docker.access.PortMapping;
import org.eclipse.jkube.kit.build.service.docker.access.log.LogDispatcher;
import org.eclipse.jkube.kit.build.service.docker.config.WatchImageConfiguration;
import org.eclipse.jkube.kit.build.service.docker.config.WatchMode;
import org.eclipse.jkube.kit.build.service.docker.helper.StartContainerExecutor;
import org.eclipse.jkube.kit.build.service.docker.helper.StartOrderResolver;
import org.eclipse.jkube.kit.build.service.docker.helper.Task;
import org.eclipse.jkube.kit.common.KitLogger;

/**
 * Watch service for monitoring changes and restarting containers
 */
public class WatchService {

    private final ArchiveService archiveService;
    private final BuildService buildService;
    private final DockerAccess dockerAccess;
    private final QueryService queryService;
    private final RunService runService;
    private final KitLogger log;

    public WatchService(ArchiveService archiveService, BuildService buildService, DockerAccess dockerAccess, QueryService queryService, RunService
            runService, KitLogger log) {
        this.archiveService = archiveService;
        this.buildService = buildService;
        this.dockerAccess = dockerAccess;
        this.queryService = queryService;
        this.runService = runService;
        this.log = log;
    }

    public synchronized void watch(WatchContext context, JKubeConfiguration buildContext, List<ImageConfiguration> images)
        throws IOException {

        // Important to be be a single threaded scheduler since watch jobs must run serialized
        ScheduledExecutorService executor = null;
        try {
            executor = Executors.newSingleThreadScheduledExecutor();

            for (StartOrderResolver.Resolvable resolvable : runService.getImagesConfigsInOrder(queryService, images)) {
                final ImageConfiguration imageConfig = (ImageConfiguration) resolvable;

                String imageId = queryService.getImageId(imageConfig.getName());
                String containerId = runService.lookupContainer(imageConfig.getName());

                ImageWatcher watcher = new ImageWatcher(imageConfig, context, imageId, containerId);
                long interval = watcher.getInterval();

                WatchMode watchMode = watcher.getWatchMode(imageConfig);
                log.info("Watching " + imageConfig.getName() + (watchMode != null ? " using " + watchMode.getDescription() : ""));

                ArrayList<String> tasks = new ArrayList<>();

                if (imageConfig.getBuildConfiguration() != null &&
                        imageConfig.getBuildConfiguration().getAssemblyConfiguration() != null) {
                    if (watcher.isCopy()) {
                        String containerBaseDir = imageConfig.getBuildConfiguration().getAssemblyConfiguration().getTargetDir();
                        schedule(executor, createCopyWatchTask(watcher, context.getBuildContext(), containerBaseDir), interval);
                        tasks.add("copying artifacts");
                    }

                    if (watcher.isBuild()) {
                        schedule(executor, createBuildWatchTask(watcher, context.getBuildContext(), watchMode == WatchMode.both, buildContext), interval);
                        tasks.add("rebuilding");
                    }
                }

                if (watcher.isRun() && watcher.getContainerId() != null) {
                    schedule(executor, createRestartWatchTask(watcher), interval);
                    tasks.add("restarting");
                }

                if (!tasks.isEmpty()) {
                    log.info("%s: Watch for %s", imageConfig.getDescription(), String.join(" and ", tasks));
                }
            }
            log.info("Waiting ...");
            if (!context.isKeepRunning()) {
                runService.addShutdownHookForStoppingContainers(context.isKeepContainer(), context.isRemoveVolumes(), context.isAutoCreateCustomNetworks());
            }
            wait();
        } catch (InterruptedException e) {
            log.warn("Interrupted");
            Thread.currentThread().interrupt();
        } finally {
            if (executor != null) {
                executor.shutdownNow();
            }
        }
    }

    private void schedule(ScheduledExecutorService executor, Runnable runnable, long interval) {
        executor.scheduleAtFixedRate(runnable, 0, interval, TimeUnit.MILLISECONDS);
    }

    private Runnable createCopyWatchTask(final ImageWatcher watcher,
                                         final JKubeConfiguration mojoParameters, final String containerBaseDir) throws IOException {
        final ImageConfiguration imageConfig = watcher.getImageConfiguration();

        final AssemblyFiles files = archiveService.getAssemblyFiles(imageConfig, mojoParameters);
        return new Runnable() {
            @Override
            public void run() {
                List<AssemblyFiles.Entry> entries = files.getUpdatedEntriesAndRefresh();
                if (entries != null && entries.size() > 0) {
                    try {
                        log.info("%s: Assembly changed. Copying changed files to container ...", imageConfig.getDescription());

                        File changedFilesArchive = archiveService.createChangedFilesArchive(entries, files.getAssemblyDirectory(),
                                imageConfig.getName(), mojoParameters);
                        dockerAccess.copyArchive(watcher.getContainerId(), changedFilesArchive, containerBaseDir);
                        callPostExec(watcher);
                    } catch (IOException | ExecException e) {
                        log.error("%s: Error when copying files to container %s: %s",
                                  imageConfig.getDescription(), watcher.getContainerId(), e.getMessage());
                    }
                }
            }
        };
    }

    private void callPostExec(ImageWatcher watcher) throws DockerAccessException, ExecException {
        if (watcher.getPostExec() != null) {
            String containerId = watcher.getContainerId();
            runService.execInContainer(containerId, watcher.getPostExec(), watcher.getImageConfiguration());
        }
    }

    private Runnable createBuildWatchTask(final ImageWatcher watcher,
                                          final JKubeConfiguration mojoParameters, final boolean doRestart, final JKubeConfiguration buildContext)
            throws IOException {
        final ImageConfiguration imageConfig = watcher.getImageConfiguration();
        final AssemblyFiles files = archiveService.getAssemblyFiles(imageConfig, mojoParameters);
        if (files != null && files.isEmpty()) {
            log.error("No assembly files for %s. Are you sure you invoked together with the `package` goal?", imageConfig.getDescription());
            throw new IOException("No files to watch found for " + imageConfig);
        }

        return new Runnable() {
            @Override
            public void run() {
                List<AssemblyFiles.Entry> entries = files.getUpdatedEntriesAndRefresh();
                if (entries != null && !entries.isEmpty()) {
                    try {
                        log.info("%s: Assembly changed. Rebuild ...", imageConfig.getDescription());

                        if (watcher.getWatchContext().getImageCustomizer() != null) {
                            log.info("%s: Customizing the image ...", imageConfig.getDescription());
                            watcher.getWatchContext().getImageCustomizer().execute(imageConfig);
                        }

                        buildService.buildImage(imageConfig, null, buildContext);

                        String name = imageConfig.getName();
                        watcher.setImageId(queryService.getImageId(name));
                        if (doRestart) {
                            restartContainer(watcher);
                        }
                    } catch (Exception e) {
                        log.error("%s: Error when rebuilding - %s", imageConfig.getDescription(), e);
                    }
                }
            }
        };
    }

    private Runnable createRestartWatchTask(final ImageWatcher watcher) {

        final String imageName = watcher.getImageName();

        return new Runnable() {
            @Override
            public void run() {

                try {
                    String currentImageId = queryService.getImageId(imageName);
                    String oldValue = watcher.getAndSetImageId(currentImageId);
                    if (!currentImageId.equals(oldValue)) {
                        restartContainer(watcher);
                    }
                } catch (Exception e) {
                    log.warn("%s: Error when restarting image - %s", watcher.getImageConfiguration().getDescription(), e);
                }
            }
        };
    }

    private void restartContainer(ImageWatcher watcher) throws Exception {
        Task<ImageWatcher> restarter = watcher.getWatchContext().getContainerRestarter();
        if (restarter == null) {
            restarter = defaultContainerRestartTask();
        }

        // Restart
        restarter.execute(watcher);
    }

    private Task<ImageWatcher> defaultContainerRestartTask() {
        return watcher -> {
            // Stop old one
            ImageConfiguration imageConfig = watcher.getImageConfiguration();
            PortMapping mappedPorts = runService.createPortMapping(imageConfig.getRunConfiguration(), watcher.getWatchContext().getBuildContext().getProject().getProperties());
            String id = watcher.getContainerId();

            String optionalPreStop = getPreStopCommand(imageConfig);
            if (optionalPreStop != null) {
                runService.execInContainer(id, optionalPreStop, watcher.getImageConfiguration());
            }
            runService.stopPreviouslyStartedContainer(id, false, false);

            // Start new one
            StartContainerExecutor helper = new StartContainerExecutor.Builder()
                    .dispatcher(watcher.watchContext.dispatcher)
                    .follow(watcher.watchContext.follow)
                    .log(log)
                    .portMapping(mappedPorts)
                    .gavLabel(watcher.watchContext.getGavLabel())
                    .projectProperties(watcher.watchContext.buildContext.getProject().getProperties())
                    .basedir(watcher.watchContext.buildContext.getProject().getBaseDirectory())
                    .imageConfig(imageConfig)
                    .serviceHub(watcher.watchContext.hub)
                    .logOutputSpecFactory(watcher.watchContext.serviceHubFactory.getLogOutputSpecFactory())
                    .showLogs(watcher.watchContext.showLogs)
                    .containerNamePattern(watcher.watchContext.containerNamePattern)
                    .buildTimestamp(watcher.watchContext.buildTimestamp)
                    .build();

            String containerId = helper.startContainers();

            watcher.setContainerId(containerId);
        };
    }

    private String getPreStopCommand(ImageConfiguration imageConfig) {
        if (imageConfig.getRunConfiguration() != null &&
                imageConfig.getRunConfiguration().getWaitConfiguration() != null &&
                imageConfig.getRunConfiguration().getWaitConfiguration().getExec() != null) {
            return imageConfig.getRunConfiguration().getWaitConfiguration().getExec().getPreStop();
        }
        return null;
    }

    // ===============================================================================================================

    // Helper class for holding state and parameter when watching images
    public class ImageWatcher {

        private final ImageConfiguration imageConfig;
        private final WatchContext watchContext;
        private final WatchMode mode;
        private final AtomicReference<String> imageIdRef, containerIdRef;
        private final long interval;
        private final String postGoal;
        private String postExec;

        public ImageWatcher(ImageConfiguration imageConfig, WatchContext watchContext, String imageId, String containerIdRef) {
            this.imageConfig = imageConfig;
            this.watchContext = watchContext;
            this.imageIdRef = new AtomicReference<>(imageId);
            this.containerIdRef = new AtomicReference<>(containerIdRef);

            this.interval = getWatchInterval(imageConfig);
            this.mode = getWatchMode(imageConfig);
            this.postGoal = getPostGoal(imageConfig);
            this.postExec = getPostExec(imageConfig);
        }

        public String getContainerId() {
            return containerIdRef.get();
        }

        public long getInterval() {
            return interval;
        }

        public String getPostGoal() {
            return postGoal;
        }

        public boolean isCopy() {
            return mode.isCopy();
        }

        public boolean isBuild() {
            return mode.isBuild();
        }

        public boolean isRun() {
            return mode.isRun();
        }

        public ImageConfiguration getImageConfiguration() {
            return imageConfig;
        }

        public void setImageId(String imageId) {
            imageIdRef.set(imageId);
        }

        public void setContainerId(String containerId) {
            containerIdRef.set(containerId);
        }

        public String getImageName() {
            return imageConfig.getName();
        }

        public String getAndSetImageId(String currentImageId) {
            return imageIdRef.getAndSet(currentImageId);
        }

        public String getPostExec() {
            return postExec;
        }

        public WatchContext getWatchContext() {
            return watchContext;
        }

        // =========================================================

        private int getWatchInterval(ImageConfiguration imageConfig) {
            WatchImageConfiguration watchConfig = imageConfig.getWatchConfiguration();
            int interval = watchConfig != null ? watchConfig.getInterval() : watchContext.getWatchInterval();
            return interval < 100 ? 100 : interval;
        }

        private String getPostExec(ImageConfiguration imageConfig) {
            WatchImageConfiguration watchConfig = imageConfig.getWatchConfiguration();
            return watchConfig != null && watchConfig.getPostExec() != null ?
                    watchConfig.getPostExec() : watchContext.getWatchPostExec();
        }

        private String getPostGoal(ImageConfiguration imageConfig) {
            WatchImageConfiguration watchConfig = imageConfig.getWatchConfiguration();
            return watchConfig != null && watchConfig.getPostGoal() != null ?
                    watchConfig.getPostGoal() : watchContext.getWatchPostGoal();

        }

        private WatchMode getWatchMode(ImageConfiguration imageConfig) {
            WatchImageConfiguration watchConfig = imageConfig.getWatchConfiguration();
            WatchMode mode = watchConfig != null ? watchConfig.getMode() : null;
            return mode != null ? mode : watchContext.getWatchMode();
        }
    }

    // ===========================================================

    /**
     * Context class to hold the watch configuration
     */
    public static class WatchContext implements Serializable {

        private JKubeConfiguration buildContext;

        private WatchMode watchMode;

        private int watchInterval;

        private boolean keepRunning;

        private String watchPostGoal;

        private String watchPostExec;

        private GavLabel gavLabel;

        private boolean keepContainer;

        private boolean removeVolumes;

        private boolean autoCreateCustomNetworks;

        private Task<ImageConfiguration> imageCustomizer;

        private Task<ImageWatcher> containerRestarter;

        private transient ServiceHub hub;
        private transient ServiceHubFactory serviceHubFactory;
        private transient LogDispatcher dispatcher;
        private boolean follow;
        private String showLogs;

        private Date buildTimestamp;

        private String containerNamePattern;

        public JKubeConfiguration getBuildContext() {
            return buildContext;
        }

        public WatchMode getWatchMode() {
            return watchMode;
        }

        public int getWatchInterval() {
            return watchInterval;
        }

        public boolean isKeepRunning() {
            return keepRunning;
        }

        public String getWatchPostGoal() {
            return watchPostGoal;
        }

        public String getWatchPostExec() {
            return watchPostExec;
        }

        public GavLabel getGavLabel() {
            return gavLabel;
        }

        public boolean isKeepContainer() {
            return keepContainer;
        }

        public boolean isRemoveVolumes() {
            return removeVolumes;
        }

        public boolean isAutoCreateCustomNetworks() {
            return autoCreateCustomNetworks;
        }

        public Task<ImageConfiguration> getImageCustomizer() {
            return imageCustomizer;
        }

        public Task<ImageWatcher> getContainerRestarter() {
            return containerRestarter;
        }

        public Date getBuildTimestamp() {
            return buildTimestamp;
        }

        public String getContainerNamePattern() {
            return containerNamePattern;
        }

        public static class Builder {

            private WatchContext context;

            public Builder() {
                this.context = new WatchContext();
            }

            public Builder(WatchContext context) {
                this.context = context;
            }

            public Builder buildContext(JKubeConfiguration buildContext) {
                context.buildContext = buildContext;
                return this;
            }

            public Builder watchMode(WatchMode watchMode) {
                context.watchMode = watchMode;
                return this;
            }

            public Builder watchInterval(int watchInterval) {
                context.watchInterval = watchInterval;
                return this;
            }

            public Builder keepRunning(boolean keepRunning) {
                context.keepRunning = keepRunning;
                return this;
            }

            public Builder watchPostGoal(String watchPostGoal) {
                context.watchPostGoal = watchPostGoal;
                return this;
            }

            public Builder watchPostExec(String watchPostExec) {
                context.watchPostExec = watchPostExec;
                return this;
            }

            public Builder pomLabel(GavLabel gavLabel) {
                context.gavLabel = gavLabel;
                return this;
            }

            public Builder keepContainer(boolean keepContainer) {
                context.keepContainer = keepContainer;
                return this;
            }

            public Builder removeVolumes(boolean removeVolumes) {
                context.removeVolumes = removeVolumes;
                return this;
            }

            public Builder imageCustomizer(Task<ImageConfiguration> imageCustomizer) {
                context.imageCustomizer = imageCustomizer;
                return this;
            }

            public Builder containerRestarter(Task<ImageWatcher> containerRestarter) {
                context.containerRestarter = containerRestarter;
                return this;
            }

            public Builder autoCreateCustomNetworks(boolean autoCreateCustomNetworks) {
                context.autoCreateCustomNetworks = autoCreateCustomNetworks;
                return this;
            }

            public Builder follow(boolean follow) {
                context.follow = follow;
                return this;
            }

            public Builder showLogs(String showLogs) {
                context.showLogs = showLogs;
                return this;
            }

            public Builder hub(ServiceHub hub){
                context.hub = hub;
                return this;
            }

            public Builder serviceHubFactory(ServiceHubFactory serviceHubFactory){
                context.serviceHubFactory = serviceHubFactory;
                return this;
            }

            public Builder dispatcher(LogDispatcher dispatcher){
                context.dispatcher = dispatcher;
                return this;
            }

            public Builder buildTimestamp(Date buildTimestamp) {
                context.buildTimestamp = buildTimestamp;
                return this;
            }

            public Builder containerNamePattern(String containerNamePattern) {
                context.containerNamePattern = containerNamePattern;
                return this;
            }


            public WatchContext build() {
                return context;
            }
        }
    }
}