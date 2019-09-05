package io.jshift.kit.build.maven.assembly;


import io.jshift.kit.build.maven.MavenBuildContext;
import io.jshift.kit.common.util.EnvUtil;

import java.io.File;

/**
 * Helper object grouping together all working and output
 * directories.
 *
 * @author roland
 * @since 27/02/15
 */
class BuildDirs {

    private final String buildTopDir;
    private final MavenBuildContext params;

    /**
     * Constructor building up the the output directories
     *
     * @param imageName image name for the image to build
     * @param params mojo params holding base and global outptput dir
     */
    BuildDirs(String imageName, MavenBuildContext params) {
        this.params = params;
        // Replace tag separator with a slash to avoid problems
        // with OSs which gets confused by colons.
        this.buildTopDir = imageName != null ? imageName.replace(':', '/') : null;
    }

    File getOutputDirectory() {
        return getDir("build");
    }

    File getWorkingDirectory() {
        return getDir("work");
    }

    File getTemporaryRootDirectory() {
        return getDir("tmp");
    }

    void createDirs() {
        for (String workDir : new String[] { "build", "work", "tmp" }) {
            File dir = getDir(workDir);
            if (!dir.exists()) {
                if(!dir.mkdirs()) {
                    throw new IllegalArgumentException("Cannot create directory " + dir.getAbsolutePath());
                }
            }
        }
    }

    private File getDir(String dir) {
        return EnvUtil.prepareAbsoluteOutputDirPath(params.getOutputDirectory(),
                params.getProject().getBasedir() != null ? params.getProject().getBasedir().toString() : null, buildTopDir, dir);
    }
}
