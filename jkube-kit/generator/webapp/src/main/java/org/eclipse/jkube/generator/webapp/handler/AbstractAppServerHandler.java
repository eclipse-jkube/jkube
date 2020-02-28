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
package org.eclipse.jkube.generator.webapp.handler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jkube.generator.api.DefaultImageLookup;
import org.eclipse.jkube.generator.webapp.AppServerHandler;
import org.eclipse.jkube.kit.common.JKubeProject;

/**
 * @author kameshs
 */
public abstract class AbstractAppServerHandler implements AppServerHandler {

    protected final DefaultImageLookup imageLookup;
    protected final JKubeProject project;
    private final String name;

    protected AbstractAppServerHandler(String name, JKubeProject project) {
        this.project = project;
        this.name = name;
        this.imageLookup = new DefaultImageLookup(this.getClass());
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Scan the project's output directory for certain files.
     *
     * @param patterns one or more patterns which fit to Maven's include syntax
     * @return list of files found
     */
    protected String[] scanFiles(String... patterns) throws IOException {
        String buildOutputDir = project.getBuildDirectory();
        if (buildOutputDir != null && new File(buildOutputDir).exists()) {
            List<String> fileList = new ArrayList<>();
            Files.walk(new File(buildOutputDir).toPath())
                    .forEach(path -> {
                        for (String pattern : patterns) {
                            // Trim wildcard suffix since last wildcard character
                            pattern = pattern.substring(pattern.lastIndexOf("*") + 1);
                            if (path.toUri().toString().contains(pattern)) {
                                fileList.add(path.toUri().toString());
                            }
                        }
                    });
            String[] fileListArr = new String[fileList.size()];
            fileListArr = fileList.toArray(fileListArr);
            return fileListArr;
        } else {
            return new String[0];
        }

    }

    /**
     * Check whether one of the given file patterns can be found
     * in the project output directory
     *
     * @param patterns patterns to check
     * @return true if the one such file exists least
     */
    protected boolean hasOneOf(String... patterns) throws IOException {
        return scanFiles(patterns).length > 0;
    }
}