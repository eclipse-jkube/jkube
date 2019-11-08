/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jkube.kit.build.service.docker.helper;

import com.google.cloud.tools.jib.api.AbsoluteUnixPath;
import com.google.cloud.tools.jib.api.CacheDirectoryCreationException;
import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.Credential;
import com.google.cloud.tools.jib.api.ImageFormat;
import com.google.cloud.tools.jib.api.ImageReference;
import com.google.cloud.tools.jib.api.InvalidImageReferenceException;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainer;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.LayerConfiguration;
import com.google.cloud.tools.jib.api.LogEvent;
import com.google.cloud.tools.jib.api.Port;
import com.google.cloud.tools.jib.api.RegistryException;
import com.google.cloud.tools.jib.api.RegistryImage;
import com.google.cloud.tools.jib.api.TarImage;
import com.google.cloud.tools.jib.event.events.ProgressEvent;
import com.google.cloud.tools.jib.event.events.TimerEvent;
import com.google.cloud.tools.jib.event.progress.ProgressEventHandler;
import com.google.cloud.tools.jib.plugins.common.TimerEventHandler;
import com.google.cloud.tools.jib.plugins.common.logging.ConsoleLogger;
import com.google.cloud.tools.jib.plugins.common.logging.ConsoleLoggerBuilder;
import com.google.cloud.tools.jib.plugins.common.logging.ProgressDisplayGenerator;
import com.google.cloud.tools.jib.plugins.common.logging.SingleThreadedExecutor;
import io.jkube.kit.build.api.auth.AuthConfig;
import io.jkube.kit.build.maven.MavenBuildContext;
import io.jkube.kit.build.service.docker.BuildService;
import io.jkube.kit.build.service.docker.ImageConfiguration;
import io.jkube.kit.build.service.docker.JibBuildService;
import io.jkube.kit.build.service.docker.RegistryService;
import io.jkube.kit.common.KitLogger;
import io.jkube.kit.common.util.EnvUtil;
import io.jkube.kit.common.util.FatJarDetector;
import io.jkube.kit.config.image.ImageName;
import io.jkube.kit.config.image.build.BuildConfiguration;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class JibBuildServiceUtil {

    private JibBuildServiceUtil() {}

    private static final String DEFAULT_JAR_NAME = "/app.jar";
    private static final String DEFAULT_USER_NAME = "fabric8/";
    private static ConsoleLogger consoleKitLogger;

    /**
     * Builds a container image using JIB
     * @param buildConfiguration jib build configuration
     * @param log logging object
     * @param offline whether offline build or not
     * @return JibContainer
     * @throws InvalidImageReferenceException invalid image reference exception
     * @throws RegistryException registry exception
     * @throws ExecutionException execution exception
     */
    public static JibContainer buildImage(JibBuildService.JibBuildConfiguration buildConfiguration, KitLogger log, boolean offline) throws InvalidImageReferenceException, RegistryException, ExecutionException  {
        ImageConfiguration imageConfiguration = buildConfiguration.getImageConfiguration();
        Credential credential = buildConfiguration.getCredential();
        String outputDir = buildConfiguration.getOutputDir();
        String targetDir = buildConfiguration.getTargetDir();
        Path fatJar = buildConfiguration.getFatJar();
        ImageFormat imageFormat = buildConfiguration.getImageFormat() != null ? buildConfiguration.getImageFormat() : ImageFormat.Docker;

        return buildImage(imageConfiguration, imageFormat, credential, fatJar, targetDir, outputDir, log, offline);
    }

    /**
     * Builds a container image using Jib from all the following parameters:
     *
     * @param imageConfiguration image configuration
     * @param imageFormat Image format whether docker or OCI
     * @param credential registry auth config
     * @param fatJar path to fat jar
     * @param targetDir target directory
     * @param outputDir output directory
     * @param log log
     * @param isOfflineMode whether to do offline mode or not.
     * @return jib container
     * @throws InvalidImageReferenceException invalid image reference
     * @throws RegistryException error with registry
     * @throws ExecutionException problems while making tarball
     */
    protected static JibContainer buildImage(ImageConfiguration imageConfiguration, ImageFormat imageFormat, Credential credential, Path fatJar, String targetDir, String outputDir, KitLogger log, boolean isOfflineMode) throws InvalidImageReferenceException, RegistryException, ExecutionException {
        String targetImage = imageConfiguration.getName();

        JibContainerBuilder containerBuilder = getContainerBuilderFromImageConfiguration(imageConfiguration);
        containerBuilder = getJibContainerBuilderFromFatJarPath(fatJar, targetDir, containerBuilder);
        containerBuilder.setFormat(imageFormat);

        String imageTarName = ImageReference.parse(targetImage).getRepository().concat(".tar");
        TarImage tarImage = TarImage.named(targetImage).saveTo(Paths.get(outputDir + "/" + imageTarName));
        RegistryImage registryImage = getRegistryImage(imageConfiguration, credential);
        try {
            JibContainer jibContainer = isOfflineMode ? buildContainer(containerBuilder, tarImage, log, isOfflineMode) : buildContainer(containerBuilder, registryImage, log);
            log.info("Image %s successfully built" + (isOfflineMode ? "." : " and pushed."), targetImage);
            return jibContainer;
        } catch (RegistryException re) {
            log.info("Building Image Tarball at %s.", imageTarName);
            buildContainer(containerBuilder, tarImage, log, false);
            log.info(" %s successfully built.", Paths.get(outputDir + "/" + imageTarName));
            throw new RegistryException(re);
        } catch (ExecutionException e) {
            buildContainer(containerBuilder, tarImage, log, true);
            log.info("%s successfully built.", Paths.get(outputDir + "/" + imageTarName));
            throw new ExecutionException(e);
        }
    }

    public static JibContainer buildContainer(JibContainerBuilder jibContainerBuilder, TarImage image, KitLogger logger, boolean offline) {
        try {
            if (offline) {
                logger.info("Building image tarball in the offline mode.");
            }
            return jibContainerBuilder.containerize(Containerizer.to(image)
                    .setOfflineMode(offline));

        } catch (CacheDirectoryCreationException | IOException | InterruptedException | ExecutionException | RegistryException ex) {
            logger.error("Unable to build the image tarball: %s", ex.getMessage());
            throw new IllegalStateException(ex);
        }
    }

    public static JibContainer buildContainer(JibContainerBuilder jibContainerBuilder, RegistryImage image, KitLogger logger) throws RegistryException, ExecutionException {
        try {
            consoleKitLogger = getConsoleKitLogger(logger);

            return jibContainerBuilder.containerize(Containerizer.to(image)
                    .addEventHandler(LogEvent.class, JibBuildServiceUtil::log)
                    .addEventHandler(
                            TimerEvent.class,
                            new TimerEventHandler(message -> consoleKitLogger.log(LogEvent.Level.DEBUG, message)))
                    .addEventHandler(
                            ProgressEvent.class,
                            new ProgressEventHandler(
                                    update ->
                                            consoleKitLogger.setFooter(
                                                    ProgressDisplayGenerator.generateProgressDisplay(
                                                            update.getProgress(), update.getUnfinishedLeafTasks())))));
        } catch (CacheDirectoryCreationException | IOException | InterruptedException e) {
            logger.error("Unable to build the image in the offline mode: %s", e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    public static void log(LogEvent event) {
        consoleKitLogger.log(event.getLevel(), event.getMessage());
    }

    public static ConsoleLogger getConsoleKitLogger(KitLogger logger) {
        ConsoleLoggerBuilder consoleKitLoggerBuilder = ConsoleLoggerBuilder
                .rich(new SingleThreadedExecutor(), true)
                .lifecycle(logger::info);
        if (logger.isDebugEnabled()) {
            consoleKitLoggerBuilder
                    .debug(logger::debug)
                    .info(logger::info);
        }
        return consoleKitLoggerBuilder.build();
    }

    public static JibBuildService.JibBuildConfiguration getJibBuildConfiguration(BuildService.BuildContext dockerBuildContext, MavenBuildContext mojoParameters, ImageConfiguration imageConfiguration, KitLogger log) throws MojoExecutionException, Exception {

        RegistryService.RegistryConfig registryConfig = dockerBuildContext.getRegistryConfig();
        BuildConfiguration buildImageConfiguration = imageConfiguration.getBuildConfiguration();

        String targetDir = buildImageConfiguration.getAssemblyConfiguration().getTargetDir();

        String outputDir = EnvUtil.prepareAbsoluteOutputDirPath(mojoParameters.getOutputDirectory(), mojoParameters.getProject().getBasedir().getAbsolutePath(), "", "").getAbsolutePath();

        if(targetDir == null) {
            targetDir = "/deployments";
        }

        AuthConfig authConfig = registryConfig.getAuthConfigFactory()
                .createAuthConfig(true, true, registryConfig.getAuthConfig(),
                        registryConfig.getSettings(), null, registryConfig.getRegistry());

        JibBuildService.JibBuildConfiguration.Builder jibBuildConfigurationBuilder = new JibBuildService.JibBuildConfiguration.Builder(log)
                .imageConfiguration(imageConfiguration)
                .imageFormat(ImageFormat.Docker)
                .targetDir(targetDir)
                .outputDir(outputDir)
                .buildDirectory(mojoParameters.getProject().getBuild().getDirectory());
        if(authConfig != null) {
            jibBuildConfigurationBuilder.credential(Credential.from(authConfig.getUsername(), authConfig.getPassword()));
        }
        return jibBuildConfigurationBuilder.build();
    }

    private static RegistryImage getRegistryImage(ImageConfiguration imageConfiguration, Credential credential) throws InvalidImageReferenceException {
        String username = "", password = "";
        String targetImage = imageConfiguration.getName();
        ImageReference imageReference = ImageReference.parse(targetImage);

        if (imageConfiguration.getBuildConfiguration().getTags() != null) {
            // Pick first not null tag
            String tag = null;
            for (String currentTag : imageConfiguration.getBuildConfiguration().getTags()) {
                if (currentTag != null) {
                    tag = currentTag;
                    break;
                }
            }
            targetImage = new ImageName(imageConfiguration.getName(), tag).getFullName();
        }

        if (credential != null) {
            username = credential.getUsername();
            password = credential.getPassword();

            if (targetImage.contains(DEFAULT_USER_NAME)) {
                targetImage = targetImage.replaceFirst(DEFAULT_USER_NAME, username + "/");
            }
        }

        String registry = imageConfiguration.getRegistry();
        if (registry != null) {
            imageReference = ImageReference.parse(new ImageName(targetImage).getFullName(registry));
        }

        return RegistryImage.named(imageReference).addCredential(username, password);
    }

    private static JibContainerBuilder getContainerBuilderFromImageConfiguration(ImageConfiguration imageConfiguration) throws InvalidImageReferenceException {
        if (imageConfiguration.getBuildConfiguration() == null) {
            return null;
        }

        BuildConfiguration buildImageConfiguration = imageConfiguration.getBuildConfiguration();
        JibContainerBuilder jibContainerBuilder = Jib.from(buildImageConfiguration.getFrom());
        if (buildImageConfiguration.getEnv() != null && !buildImageConfiguration.getEnv().isEmpty()) {
            jibContainerBuilder.setEnvironment(buildImageConfiguration.getEnv());
        }

        if (buildImageConfiguration.getPorts() != null && !buildImageConfiguration.getPorts().isEmpty()) {
            jibContainerBuilder.setExposedPorts(getPortSet(buildImageConfiguration.getPorts()));
        }

        if (buildImageConfiguration.getLabels() != null && !buildImageConfiguration.getLabels().isEmpty()) {
            jibContainerBuilder.setLabels(buildImageConfiguration.getLabels());
        }

        if (buildImageConfiguration.getEntryPoint() != null) {
            jibContainerBuilder.setEntrypoint(buildImageConfiguration.getEntryPoint().asStrings());
        }

        if (buildImageConfiguration.getWorkdir() != null) {
            jibContainerBuilder.setWorkingDirectory(AbsoluteUnixPath.get(buildImageConfiguration.getWorkdir()));
        }

        if (buildImageConfiguration.getUser() != null) {
            jibContainerBuilder.setUser(buildImageConfiguration.getUser());
        }

        if (buildImageConfiguration.getVolumes() != null) {
            buildImageConfiguration.getVolumes()
                    .forEach(volumePath -> jibContainerBuilder.addVolume(AbsoluteUnixPath.get(volumePath)));
        }

        jibContainerBuilder.setCreationTime(Instant.now());
        return jibContainerBuilder;
    }

    private static JibContainerBuilder getJibContainerBuilderFromFatJarPath(Path fatJar, String targetDir, JibContainerBuilder containerBuilder) {
        if (fatJar != null) {
            String fatJarName = fatJar.getFileName().toString();
            String jarPath = targetDir + "/" + (fatJarName.isEmpty() ? DEFAULT_JAR_NAME: fatJarName);
            containerBuilder = containerBuilder
                    .addLayer(LayerConfiguration.builder().addEntry(fatJar, AbsoluteUnixPath.get(jarPath)).build());
        }
        return containerBuilder;
    }

    private static Set<Port> getPortSet(List<String> ports) {

        Set<Port> portSet = new HashSet<Port>();
        for(String port : ports) {
            portSet.add(Port.tcp(Integer.parseInt(port)));
        }

        return portSet;
    }

    public static Path getFatJar(String buildDir, KitLogger log) {
        FatJarDetector fatJarDetector = new FatJarDetector(buildDir);
        try {
            FatJarDetector.Result result = fatJarDetector.scan();
            if(result != null) {
                return result.getArchiveFile().toPath();
            }

        } catch (MojoExecutionException e) {
            log.error("MOJO Execution exception occurred: %s", e);
            throw new UnsupportedOperationException(e);
        }
        return null;
    }
}