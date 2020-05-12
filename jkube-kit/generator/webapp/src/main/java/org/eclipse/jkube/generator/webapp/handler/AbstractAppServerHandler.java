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

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.eclipse.jkube.generator.api.DefaultImageLookup;
import org.eclipse.jkube.generator.api.GeneratorContext;
import org.eclipse.jkube.generator.webapp.AppServerHandler;
import org.eclipse.jkube.kit.common.JavaProject;

/**
 * @author kameshs
 */
public abstract class AbstractAppServerHandler implements AppServerHandler {

    protected final DefaultImageLookup imageLookup;
    protected final GeneratorContext generatorContext;
    private final String name;

    protected AbstractAppServerHandler(String name, GeneratorContext generatorContext) {
        this.generatorContext = generatorContext;
        this.name = name;
        this.imageLookup = new DefaultImageLookup(this.getClass());
    }

    @Override
    public String getName() {
        return name;
    }

    protected JavaProject getProject() {
        return generatorContext.getProject();
    }

    /**
     * Scan the project's output directory for certain files.
     *
     * @param patterns one or more patterns which fit to Maven's include syntax
     * @return list of files found
     */
    protected String[] scanFiles(String... patterns) throws IOException {
        if (getProject().getBuildDirectory().exists()) {
            try (Stream<Path> fileStream = Files.walk(getProject().getBuildDirectory().toPath())) {
                return fileStream
                    .filter(path -> {
                        for (String pattern : patterns) {
                            // Trim wildcard suffix since last wildcard character
                            pattern = pattern.substring(pattern.lastIndexOf('*') + 1);
                            if (path.toUri().toString().contains(pattern)) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .map(Path::toUri).map(URI::toString)
                    .toArray(String[]::new);
            }
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