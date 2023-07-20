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
package org.eclipse.jkube.kit.build.service.docker.helper;

import org.eclipse.jkube.kit.build.service.docker.QueryService;
import org.eclipse.jkube.kit.build.service.docker.access.DockerAccessException;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author roland
 */
public class StartOrderResolver {

    public static final int MAX_RESOLVE_RETRIES = 10;

    private final QueryService queryService;

    private final List<ImageConfiguration> secondPass;
    private final Set<String> processedImages;

    public static List<ImageConfiguration> resolve(QueryService queryService, List<ImageConfiguration> convertToResolvables) {
        return new StartOrderResolver(queryService).resolve(convertToResolvables);
    }

    private StartOrderResolver(QueryService queryService) {
        this.queryService = queryService;

        this.secondPass = new ArrayList<>();
        this.processedImages = new HashSet<>();
    }


    // Check images for volume / link dependencies and return it in the right order.
    // Only return images which should be run
    // Images references via volumes but with no run configuration are started once to create
    // an appropriate container which can be linked into the image
    private List<ImageConfiguration> resolve(List<ImageConfiguration> images) {
        List<ImageConfiguration> resolved = new ArrayList<>();
        // First pass: Pick all data images and all without dependencies
        for (ImageConfiguration config : images) {
            List<String> volumesOrLinks = extractDependentImagesFor(config);
            if (volumesOrLinks == null) {
                // A data image only or no dependency. Add it to the list of data image which can be always
                // created first.
                updateProcessedImages(config);
                resolved.add(config);
            } else {
                secondPass.add(config);
            }
        }

        // Next passes: Those with dependencies are checked whether they already have been visited.
        return secondPass.isEmpty() ? resolved : resolveRemaining(resolved);
    }

    private List<ImageConfiguration> resolveRemaining(List<ImageConfiguration> ret) {
        int retries = MAX_RESOLVE_RETRIES;
        String error = null;
        try {
            do {
                resolveImageDependencies(ret);
            } while (!secondPass.isEmpty() && retries-- > 0);
        } catch (DockerAccessException | ResolveSteadyStateException e) {
            error = "Cannot resolve image dependencies for start order\n" + remainingImagesDescription();
        }
        if (retries == 0 && !secondPass.isEmpty()) {
            error = "Cannot resolve image dependencies after " + MAX_RESOLVE_RETRIES + " passes\n"
                    + remainingImagesDescription();
        }
        if (error != null) {
            throw new IllegalStateException(error);
        }
        return ret;
    }

    private void updateProcessedImages(ImageConfiguration config) {
        processedImages.add(config.getName());
        if (config.getAlias() != null) {
            processedImages.add(config.getAlias());
        }
    }

    private String remainingImagesDescription() {
        StringBuilder ret = new StringBuilder();
        ret.append("Unresolved images:\n");
        for (ImageConfiguration config : secondPass) {
            ret.append("* ")
               .append(config.getAlias())
               .append(" depends on ")
               .append(String.join(",", config.getDependencies().toArray(new String[0])))
               .append("\n");
        }
        return ret.toString();
    }

    private void resolveImageDependencies(List<ImageConfiguration> resolved) throws DockerAccessException, ResolveSteadyStateException {
        boolean changed = false;
        Iterator<ImageConfiguration> iterator = secondPass.iterator();

        while (iterator.hasNext()) {
            ImageConfiguration config = iterator.next();
            if (hasRequiredDependencies(config)) {
                updateProcessedImages(config);
                resolved.add(config);
                changed = true;
                iterator.remove();
            }
        }

        if (!changed) {
            throw new ResolveSteadyStateException();
        }
    }

    private boolean hasRequiredDependencies(ImageConfiguration config) throws DockerAccessException {
        List<String> dependencies = extractDependentImagesFor(config);
        if (dependencies == null) {
            return false;
        }

        for (String dependency : dependencies) {
            // make sure the container exists, it's state will be verified elsewhere
            if (processedImages.contains(dependency) ||
                // Check also whether a *container* with this name exists in which case it is interpreted
                // as an external dependency which is already running
                queryService.hasContainer(dependency)) {
                continue;
            }
            return false;
        }

        return true;
    }

    private List<String> extractDependentImagesFor(ImageConfiguration config) {
        LinkedHashSet<String> ret = new LinkedHashSet<>(config.getDependencies());
        return ret.isEmpty() ? null : new ArrayList<>(ret);
    }

    // Exception indicating a steady state while resolving start order of images
    private static class ResolveSteadyStateException extends Exception { }

}
