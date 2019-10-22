package io.jkube.kit.build.api;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.function.Function;

import io.jkube.kit.common.KitLogger;
import io.jkube.kit.config.image.build.BuildConfiguration;


/**
 * @author roland
 * @since 16.10.18
 */
public interface BuildContext {

    String getSourceDirectory();

    File getBasedir();

    String getOutputDirectory();

    Properties getProperties();

    Function<String, String> createInterpolator(String filter);

    File createImageContentArchive(String imageName, BuildConfiguration buildConfig, KitLogger log) throws IOException;

    RegistryContext getRegistryContext();

    File inSourceDir(String path);

    File inOutputDir(String path);

    File inDir(String dir, String path);
}
