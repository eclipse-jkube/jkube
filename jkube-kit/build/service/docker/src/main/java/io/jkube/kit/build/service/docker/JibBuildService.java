package io.jkube.kit.build.service.docker;

import com.google.cloud.tools.jib.api.Credential;
import com.google.cloud.tools.jib.api.ImageFormat;
import com.google.cloud.tools.jib.api.InvalidImageReferenceException;
import com.google.cloud.tools.jib.api.JibContainer;
import com.google.cloud.tools.jib.api.RegistryException;
import io.jkube.kit.build.maven.MavenBuildContext;
import io.jkube.kit.build.service.docker.helper.DeepCopy;
import io.jkube.kit.build.service.docker.helper.JibBuildServiceUtil;
import io.jkube.kit.common.KitLogger;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class JibBuildService {

    private KitLogger log;
    private BuildService.BuildContext dockerBuildContext;
    private MavenBuildContext mojoParameters;

    public JibBuildService(BuildService.BuildContext dockerBuildContext, MavenBuildContext mojoParameters, KitLogger log) {
        Objects.requireNonNull(dockerBuildContext, "dockerBuildContext");
        this.dockerBuildContext = dockerBuildContext;
        this.mojoParameters = mojoParameters;
        this.log = log;
    }

    public void buildImage(ImageConfiguration imageConfiguration, boolean isOfflineMode) {
        try {
            doJibBuild(JibBuildServiceUtil.getJibBuildConfiguration(dockerBuildContext, mojoParameters, imageConfiguration, log), isOfflineMode);
        } catch (Exception ex) {
            throw new UnsupportedOperationException(ex);
        }
    }

    public JibContainer doJibBuild(JibBuildService.JibBuildConfiguration jibBuildConfiguration, boolean isOfflineMode) throws InvalidImageReferenceException, RegistryException, ExecutionException {
        return JibBuildServiceUtil.buildImage(jibBuildConfiguration, log, isOfflineMode);
    }

    public static class JibBuildConfiguration {
        private ImageConfiguration imageConfiguration;

        private ImageFormat imageFormat;

        private Credential credential;

        private Path fatJarPath;

        private String targetDir;

        private String outputDir;

        private JibBuildConfiguration() {}

        public ImageConfiguration getImageConfiguration() { return imageConfiguration; }

        public String getTargetDir() {
            return targetDir;
        }

        public String getOutputDir() {
            return outputDir;
        }

        public Credential getCredential() {
            return credential;
        }

        public Path getFatJar() {
            return fatJarPath;
        }

        public ImageFormat getImageFormat() {
            return imageFormat;
        }

        public static class Builder {
            private final JibBuildConfiguration configutil;
            private final KitLogger logger;

            public Builder(KitLogger logger) {
                this(null, logger);
            }

            public Builder(JibBuildConfiguration that, KitLogger logger) {
                this.logger = logger;
                if (that == null) {
                    this.configutil = new JibBuildConfiguration();
                } else {
                    this.configutil = DeepCopy.copy(that);
                }
            }

            public Builder imageConfiguration(ImageConfiguration imageConfiguration) {
                configutil.imageConfiguration = imageConfiguration;
                return this;
            }

            public Builder imageFormat(ImageFormat imageFormat) {
                configutil.imageFormat = imageFormat;
                return this;
            }

            public Builder credential(Credential credential) {
                configutil.credential = credential;
                return this;
            }

            public Builder buildDirectory(String buildDir) {
                configutil.fatJarPath = JibBuildServiceUtil.getFatJar(buildDir, logger);
                return this;
            }

            public Builder targetDir(String targetDir) {
                configutil.targetDir = targetDir;
                return this;
            }

            public Builder outputDir(String outputDir) {
                configutil.outputDir = outputDir;
                return this;
            }

            public JibBuildConfiguration build() {
                return configutil;
            }
        }
    }
}
