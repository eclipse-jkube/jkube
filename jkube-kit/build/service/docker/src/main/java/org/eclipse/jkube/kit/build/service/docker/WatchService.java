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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jkube.kit.build.api.assembly.AssemblyFiles;
import org.eclipse.jkube.kit.build.service.docker.access.PortMapping;
import org.eclipse.jkube.kit.build.service.docker.helper.StartContainerExecutor;
import org.eclipse.jkube.kit.build.service.docker.helper.Task;
import org.eclipse.jkube.kit.build.service.docker.watch.CopyFilesTask;
import org.eclipse.jkube.kit.build.service.docker.watch.ExecTask;
import org.eclipse.jkube.kit.build.service.docker.watch.WatchContext;
import org.eclipse.jkube.kit.build.service.docker.watch.WatchException;
import org.eclipse.jkube.kit.common.AssemblyFileEntry;
import org.eclipse.jkube.kit.common.JKubeConfiguration;
import org.eclipse.jkube.kit.common.KitLogger;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.RunImageConfiguration;
import org.eclipse.jkube.kit.config.image.WaitConfiguration;
import org.eclipse.jkube.kit.config.image.WatchImageConfiguration;
import org.eclipse.jkube.kit.config.image.WatchMode;

import org.apache.commons.lang3.StringUtils;

/**
 * Watch service for monitoring changes and restarting containers
 */
public class WatchService {

    private final ArchiveService archiveService;
    private final BuildService buildService;
    private final QueryService queryService;
    private final RunService runService;
    private final KitLogger log;

    public WatchService(ArchiveService archiveService, BuildService buildService, QueryService queryService, RunService
            runService, KitLogger log) {
        this.archiveService = archiveService;
        this.buildService = buildService;
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

            for (ImageConfiguration imageConfig : runService.getImagesConfigsInOrder(queryService, images)) {

                String imageId = queryService.getImageId(imageConfig.getName());
                String containerId = runService.lookupContainer(imageConfig.getName());

                ImageWatcher watcher = new ImageWatcher(imageConfig, context, imageId, containerId);
                long interval = watcher.getInterval();

                WatchMode watchMode = watcher.getWatchMode(imageConfig);
                log.info("Watching %s %s", imageConfig.getName(), (watchMode != null ? " using " + watchMode.getDescription() : ""));

                ArrayList<String> tasks = new ArrayList<>();

                if (imageConfig.getBuildConfiguration() != null &&
                        imageConfig.getBuildConfiguration().getAssembly() != null) {
                    if (watcher.isCopy()) {
                        schedule(executor, createCopyWatchTask(watcher, context.getBuildContext()), interval);
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
                                         final JKubeConfiguration jKubeConfiguration) throws IOException {
        final ImageConfiguration imageConfig = watcher.getImageConfiguration();

        final AssemblyFiles files = archiveService.getAssemblyFiles(imageConfig, jKubeConfiguration);

        return () -> {
            List<AssemblyFileEntry> entries = files.getUpdatedEntriesAndRefresh();
                 if (!entries.isEmpty()) {
                     try {
                         log.info("%s: Assembly changed. Copying changed files to container...", imageConfig.getDescription());
                         File changedFilesArchive = archiveService.createChangedFilesArchive(entries, files.getAssemblyDirectory(),
                                 imageConfig.getName(), jKubeConfiguration);
                         copyFilesToContainer(changedFilesArchive, watcher);
                                    callPostExec(watcher);
                     } catch (IOException | WatchException e) {
                         log.error("%s: Error when copying files to container %s: %s",
                                 imageConfig.getDescription(), watcher.getContainerId(), e.getMessage());


                }
            }
        };
    }

    void copyFilesToContainer(File changedFilesArchive, ImageWatcher watcher) throws IOException, WatchException {
        final CopyFilesTask cft = watcher.getWatchContext().getContainerCopyTask();
        if (cft != null) {
            cft.copy(changedFilesArchive);
            log.info("Files successfully copied to the container.");
        } else {
            log.warn("No copy task found for copy mode. Ignoring...");
        }
    }

    void callPostExec(ImageWatcher watcher) throws IOException, WatchException {
        final ExecTask execTask = watcher.getWatchContext().getContainerCommandExecutor();
        if (StringUtils.isNotBlank(watcher.getPostExec()) && execTask != null) {
            log.info("jkube.watch.postExec: %n%s", execTask.exec(watcher.getPostExec()));
        }
    }

    Runnable createBuildWatchTask(final ImageWatcher watcher,
                                          final JKubeConfiguration mojoParameters, final boolean doRestart, final JKubeConfiguration buildContext)
            throws IOException {
        final ImageConfiguration imageConfig = watcher.getImageConfiguration();
        final AssemblyFiles files = archiveService.getAssemblyFiles(imageConfig, mojoParameters);
        if (files.isEmpty()) {
            log.error("No assembly files for %s. Are you sure you invoked together with the `package` goal?", imageConfig.getDescription());
            throw new IOException("No files to watch found for " + imageConfig);
        }

        return () -> {
            List<AssemblyFileEntry> entries = files.getUpdatedEntriesAndRefresh();
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
                    restartContainerAndCallPostGoal(watcher, doRestart);
                } catch (Exception e) {
                    log.error("%s: Error when rebuilding - %s", imageConfig.getDescription(), e);
                }
            }
        };
    }


    private void callPostGoal(ImageWatcher watcher) {
        Optional.ofNullable(watcher.getWatchContext().getPostGoalTask()).ifPresent(Runnable::run);
    }

    private Runnable createRestartWatchTask(final ImageWatcher watcher) {

        final String imageName = watcher.getImageName();

         return () -> {

                try {
                    String currentImageId = queryService.getImageId(imageName);
                    String oldValue = watcher.getAndSetImageId(currentImageId);
                    if (!currentImageId.equals(oldValue)) {
                        restartContainerAndCallPostGoal(watcher, true);
                    }
                } catch (Exception e) {
                    log.warn("%s: Error when restarting image - %s", watcher.getImageConfiguration().getDescription(), e);
                }
            };
        }


    void restartContainerAndCallPostGoal(ImageWatcher watcher, boolean isRestartRequired) throws Exception {
        if (isRestartRequired) {
            restartContainer(watcher);
        }
        callPostGoal(watcher);
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
            StartContainerExecutor helper = StartContainerExecutor.builder()
                    .dispatcher(watcher.watchContext.getDispatcher())
                    .follow(watcher.watchContext.isFollow())
                    .log(log)
                    .portMapping(mappedPorts)
                    .gavLabel(watcher.watchContext.getGavLabel())
                    .projectProperties(watcher.watchContext.getBuildContext().getProject().getProperties())
                    .basedir(watcher.watchContext.getBuildContext().getProject().getBaseDirectory())
                    .imageConfig(imageConfig)
                    .hub(watcher.watchContext.getHub())
                    .logOutputSpecFactory(watcher.watchContext.getServiceHubFactory().getLogOutputSpecFactory())
                    .showLogs(watcher.watchContext.getShowLogs())
                    .containerNamePattern(watcher.watchContext.getContainerNamePattern())
                    .buildDate(watcher.watchContext.getBuildTimestamp())
                    .build();

            String containerId = helper.startContainers();

            watcher.setContainerId(containerId);
        };
    }

    private String getPreStopCommand(ImageConfiguration imageConfig) {
        return Optional.ofNullable(imageConfig.getRunConfiguration())
            .map(RunImageConfiguration::getWait)
            .map(WaitConfiguration::getExec)
            .map(WaitConfiguration.ExecConfiguration::getPreStop)
            .orElse(null);
    }

    // ===============================================================================================================

    // Helper class for holding state and parameter when watching images
    public static class ImageWatcher {

        private final ImageConfiguration imageConfig;
        private final WatchContext watchContext;
        private final WatchMode mode;
        private final AtomicReference<String> imageIdRef;
        private final AtomicReference<String> containerIdRef;
        private final long interval;
        private final String postGoal;
        private final String postExec;

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
            int applicableInterval = watchConfig != null ?
                watchConfig.getInterval() : watchContext.getWatchInterval();
            return Math.max(applicableInterval, 100);
        }

        private String getPostExec(ImageConfiguration imageConfig) {
            WatchImageConfiguration watchConfig = imageConfig.getWatchConfiguration();
            return watchConfig != null && watchConfig.getPostExec() != null ?
                    watchConfig.getPostExec() : watchContext.getWatchPostExec();
        }

        private String getPostGoal(ImageConfiguration imageConfig) {
            WatchImageConfiguration watchConfig = imageConfig.getWatchConfiguration();
            return watchConfig != null && watchConfig.getPostGoal() != null ?
                    watchConfig.getPostGoal() : null;

        }

        private WatchMode getWatchMode(ImageConfiguration imageConfig) {
            WatchImageConfiguration watchConfig = imageConfig.getWatchConfiguration();
            WatchMode watchMode = watchConfig != null ? watchConfig.getMode() : null;
            return watchMode != null ? watchMode : watchContext.getWatchMode();

        }
    }

    // ===========================================================

}

