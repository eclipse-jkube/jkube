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
package org.eclipse.jkube.kit.service.jib;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.jkube.kit.build.api.assembly.BuildDirs;
import org.eclipse.jkube.kit.common.Assembly;
import org.eclipse.jkube.kit.common.AssemblyFileEntry;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.ImageName;
import org.eclipse.jkube.kit.common.Arguments;
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration;

import com.google.cloud.tools.jib.api.CacheDirectoryCreationException;
import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.Credential;
import com.google.cloud.tools.jib.api.InvalidImageReferenceException;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.LogEvent;
import com.google.cloud.tools.jib.api.RegistryException;
import com.google.cloud.tools.jib.api.RegistryImage;
import com.google.cloud.tools.jib.api.TarImage;
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath;
import com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer;
import com.google.cloud.tools.jib.api.buildplan.FilePermissions;
import com.google.cloud.tools.jib.api.buildplan.ImageFormat;
import com.google.cloud.tools.jib.api.buildplan.Port;
import com.google.cloud.tools.jib.event.events.ProgressEvent;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

import static com.google.cloud.tools.jib.api.buildplan.FileEntriesLayer.DEFAULT_FILE_PERMISSIONS_PROVIDER;

public class JibServiceUtil {

    private JibServiceUtil() {
    }

    private static final long JIB_EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS = 10L;
    private static final String BUSYBOX = "busybox:latest";

    /**
     * Build container image using JIB
     *
     * @param jibContainerBuilder jib container builder object
     * @param image tarball for image
     * @param logger kit logger
     * @throws InterruptedException in case thread is interrupted
     */
    public static void buildContainer(JibContainerBuilder jibContainerBuilder, TarImage image, JibLogger logger)
            throws InterruptedException {

        final ExecutorService jibBuildExecutor = Executors.newCachedThreadPool();
        try {
            jibContainerBuilder.setCreationTime(Instant.now());
            jibContainerBuilder.containerize(Containerizer.to(image)
                .setAllowInsecureRegistries(true)
                .setExecutorService(jibBuildExecutor)
                .addEventHandler(LogEvent.class, logger)
                .addEventHandler(ProgressEvent.class, logger.progressEventHandler()));
            logger.updateFinished();
        } catch (CacheDirectoryCreationException | IOException | ExecutionException | RegistryException ex) {
            throw new IllegalStateException("Unable to build the image tarball: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw ex;
        } finally {
            jibBuildExecutor.shutdown();
            jibBuildExecutor.awaitTermination(JIB_EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    public static JibContainerBuilder containerFromImageConfiguration(
        ImageConfiguration imageConfiguration, String pullRegistry, Credential pullRegistryCredential) throws InvalidImageReferenceException {
        final JibContainerBuilder containerBuilder = Jib
          .from(toRegistryImage(getBaseImage(imageConfiguration, pullRegistry), pullRegistryCredential))
          .setFormat(ImageFormat.Docker);
        return populateContainerBuilderFromImageConfiguration(containerBuilder, imageConfiguration);
    }

    public static String getFullImageName(ImageConfiguration imageConfiguration, String tag) {
        ImageName imageName;
        if (tag != null) {
            imageName = new ImageName(imageConfiguration.getName(), tag);
        } else {
            imageName = new ImageName(imageConfiguration.getName());
        }
        return imageName.getFullName();
    }

    /**
     * Push Image to registry using JIB
     *
     * @param imageConfiguration ImageConfiguration
     * @param pushCredentials    push credentials
     * @param tarArchive         tar archive built during build goal
     * @param log                Logger
     */
    public static void jibPush(ImageConfiguration imageConfiguration, Credential pushCredentials, File tarArchive, JibLogger log) {
        String imageName = getFullImageName(imageConfiguration, null);
        List<String> additionalTags = imageConfiguration.getBuildConfiguration().getTags();
        try {
            pushImage(TarImage.at(tarArchive.toPath()), additionalTags, imageName, pushCredentials, log);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread Interrupted", e);
        }
    }

    private static void pushImage(TarImage baseImage, List<String> additionalTags, String targetImageName, Credential credential, JibLogger logger)
        throws InterruptedException {

        final ExecutorService jibBuildExecutor = Executors.newCachedThreadPool();
        try {
            submitPushToJib(baseImage, toRegistryImage(targetImageName, credential), additionalTags, jibBuildExecutor, logger);
        } catch (RegistryException | CacheDirectoryCreationException | InvalidImageReferenceException | IOException | ExecutionException e) {
            throw new IllegalStateException("Exception occurred while pushing the image: " + targetImageName + ", " + e.getMessage(), e);
        } finally {
            jibBuildExecutor.shutdown();
            jibBuildExecutor.awaitTermination(JIB_EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    private static JibContainerBuilder populateContainerBuilderFromImageConfiguration(JibContainerBuilder containerBuilder, ImageConfiguration imageConfiguration) {
        final Optional<BuildConfiguration> bic =
                Optional.ofNullable(Objects.requireNonNull(imageConfiguration).getBuildConfiguration());
        bic.map(BuildConfiguration::getEntryPoint)
                .map(Arguments::asStrings)
                .ifPresent(containerBuilder::setEntrypoint);
        bic.map(BuildConfiguration::getEnv)
                .ifPresent(containerBuilder::setEnvironment);
        bic.map(BuildConfiguration::getPorts).map(List::stream)
                .map(s -> s.map(Integer::parseInt).map(Port::tcp))
                .map(s -> s.collect(Collectors.toSet()))
                .ifPresent(containerBuilder::setExposedPorts);
        bic.map(BuildConfiguration::getLabels)
                .map(Map::entrySet)
                .ifPresent(labels -> labels.forEach(l -> {
                    if (l.getKey() != null && l.getValue() != null) {
                        containerBuilder.addLabel(l.getKey(), l.getValue());
                    }
                }));
        bic.map(BuildConfiguration::getCmd)
                .map(Arguments::asStrings)
                .ifPresent(containerBuilder::setProgramArguments);
        bic.map(BuildConfiguration::getUser)
                .ifPresent(containerBuilder::setUser);
        bic.map(BuildConfiguration::getVolumes).map(List::stream)
                .map(s -> s.map(AbsoluteUnixPath::get))
                .map(s -> s.collect(Collectors.toSet()))
                .ifPresent(containerBuilder::setVolumes);
        bic.map(BuildConfiguration::getWorkdir)
                .filter(((Predicate<String>) String::isEmpty).negate())
                .map(AbsoluteUnixPath::get)
                .ifPresent(containerBuilder::setWorkingDirectory);
        return containerBuilder;
    }

    private static void submitPushToJib(TarImage baseImage, RegistryImage targetImage, List<String> additionalTags, ExecutorService jibBuildExecutor, JibLogger logger) throws InterruptedException, ExecutionException, RegistryException, CacheDirectoryCreationException, IOException {
        Jib
          .from(baseImage)
          .setCreationTime(Instant.now())
          .containerize(createJibContainerizer(targetImage, additionalTags, jibBuildExecutor, logger));
        logger.updateFinished();
    }

    private static Containerizer createJibContainerizer(RegistryImage targetImage, List<String> additionalTags, ExecutorService jibBuildExecutor, JibLogger logger) {
        Containerizer containerizer = Containerizer.to(targetImage)
            .setAllowInsecureRegistries(true)
            .setExecutorService(jibBuildExecutor)
            .addEventHandler(LogEvent.class, logger)
            .addEventHandler(ProgressEvent.class, logger.progressEventHandler());
        if (additionalTags != null) {
            additionalTags.forEach(containerizer::withAdditionalTag);
        }
        return containerizer;
    }

    private static RegistryImage toRegistryImage(String imageReference, Credential credential) throws InvalidImageReferenceException {
        RegistryImage registryImage = RegistryImage.named(imageReference);
        if (credential != null && !credential.getUsername().isEmpty() && !credential.getPassword().isEmpty()) {
            registryImage.addCredential(credential.getUsername(), credential.getPassword());
        }
        return registryImage;
    }

    public static String getBaseImage(ImageConfiguration imageConfiguration, String optionalRegistry) {
        String baseImage = Optional.ofNullable(imageConfiguration)
                .map(ImageConfiguration::getBuildConfiguration)
                .map(BuildConfiguration::getFrom)
                .filter(((Predicate<String>) String::isEmpty).negate())
                .orElse(BUSYBOX);
        return new ImageName(baseImage).getFullName(optionalRegistry);
    }

    @Nonnull
    public static List<FileEntriesLayer> layers(BuildDirs buildDirs, Map<Assembly, List<AssemblyFileEntry>> layers) {
        final List<FileEntriesLayer> fileEntriesLayers = new ArrayList<>();
        for (Map.Entry<Assembly, List<AssemblyFileEntry>> layer : layers.entrySet()) {
            final FileEntriesLayer.Builder fel = FileEntriesLayer.builder();
            final String layerId = layer.getKey().getId();
            final Path outputPath;
            if (StringUtils.isBlank(layerId)) {
                outputPath = buildDirs.getOutputDirectory().toPath();
            } else {
                fel.setName(layerId);
                outputPath = new File(buildDirs.getOutputDirectory(), layerId).toPath();
            }
            for (AssemblyFileEntry afe : layer.getValue()) {
                final Path source = afe.getSource().toPath();
                final AbsoluteUnixPath target = AbsoluteUnixPath.get(StringUtils.prependIfMissing(
                    FilenameUtils.separatorsToUnix(outputPath.relativize(afe.getDest().toPath()).normalize().toString()), "/"));
                final FilePermissions permissions = StringUtils.isNotBlank(afe.getFileMode()) ?
                    FilePermissions.fromOctalString(StringUtils.right(afe.getFileMode(), 3)) :
                    DEFAULT_FILE_PERMISSIONS_PROVIDER.get(source, target);
                fel.addEntry(source, target, permissions);
            }
            fileEntriesLayers.add(fel.build());
        }
        return fileEntriesLayers;
    }

}
