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
package org.eclipse.jkube.generator.webapp.handler;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
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
     * Scan the project's build directory for certain files.
     *
     * Patterns will be matched using {@link java.nio.file.FileSystem#getPathMatcher(String)}.
     *
     * @param patterns one or more patterns to match files in the build directory.
     * @return list of files found matching the provided pattern.
     */
    private String[] scanFiles(String... patterns) throws IOException {
        if (getProject().getBuildDirectory().exists()) {
            try (Stream<Path> fileStream = Files.walk(getProject().getBuildDirectory().toPath())) {
                return fileStream
                    .filter(path -> {
                        for (String pattern : patterns) {
                            if (FileSystems.getDefault().getPathMatcher(pattern).matches(path)) {
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
     * Check whether one of the given file patterns can be found in the project build directory.
     *
     * Patterns will be matched using {@link java.nio.file.FileSystem#getPathMatcher(String)}.
     *
     * @param patterns patterns to check.
     * @return true if at least one file matches any of the provided patterns.
     */
    protected boolean hasOneOf(String... patterns) throws IOException {
        return scanFiles(patterns).length > 0;
    }
}