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
package org.eclipse.jkube.kit.build.service.docker.helper;

import com.google.common.collect.ImmutableSet;
import org.eclipse.jkube.kit.build.api.model.Container;
import org.eclipse.jkube.kit.config.image.ImageConfiguration;
import org.eclipse.jkube.kit.config.image.RunImageConfiguration;
import org.eclipse.jkube.kit.config.image.ImageName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author marcus
 * @since 1.0.0
 */
public class ContainerNamingUtil {

    private static final String INDEX_PLACEHOLDER = "%i";

    public static final String DEFAULT_CONTAINER_NAME_PATTERN = "%n-%i";

    // class with only static methods
    private ContainerNamingUtil() { }

    public static String formatContainerName(final ImageConfiguration image,
                                             final String defaultContainerNamePattern,
                                             final Date buildTimestamp,
                                             final Collection<Container> existingContainers) {

        String containerNamePattern = extractContainerNamePattern(image, defaultContainerNamePattern);
        Set<String> existingContainersNames = extractContainerNames(existingContainers);

        final String partiallyApplied =
            replacePlaceholders(
                containerNamePattern,
                image.getName(),
                image.getAlias(),
                buildTimestamp);


        if (partiallyApplied.contains(INDEX_PLACEHOLDER)) {
            for (long i = 1; i < Long.MAX_VALUE; i++) {
                final String withIndexApplied = partiallyApplied.replaceAll(INDEX_PLACEHOLDER, String.valueOf(i));
                if (!existingContainersNames.contains(withIndexApplied)) {
                    return withIndexApplied;
                }
            }
            throw new IllegalStateException("Could not find any free container name for pattern " + partiallyApplied);
        } else {
            return partiallyApplied;
        }
    }

    /**
     * Keep only the entry with the higest index if an indexed naming scheme for container has been chosen.
     * @param image the image from which to the the container pattern
     * @param defaultContainerNamePattern the default container name pattern
     * @param buildTimestamp the timestamp for the build
     * @param containers the list of existing containers
     * @return a list with potentially lower indexed entries removed
     */
    public static Collection<Container> getContainersToStop(final ImageConfiguration image,
                                                            final String defaultContainerNamePattern,
                                                            final Date buildTimestamp,
                                                            final Collection<Container> containers) {

        String containerNamePattern = extractContainerNamePattern(image, defaultContainerNamePattern);

        // Only special treatment for indexed container names
        if (!containerNamePattern.contains(INDEX_PLACEHOLDER)) {
            return containers;
        }

        final String partiallyApplied =
            replacePlaceholders(
                containerNamePattern,
                image.getName(),
                image.getAlias(),
                buildTimestamp);

        return keepOnlyLastIndexedContainer(containers, partiallyApplied);

    }

    // ========================================================================================================

    private static String replacePlaceholders(String containerNamePattern, String imageName, String nameAlias, Date buildTimestamp) {

        Map<String, FormatParameterReplacer.Lookup> lookups = new HashMap<>();

        lookups.put("a", () -> nameAlias);
        lookups.put("n", () -> cleanImageName(imageName));
        lookups.put("t", () -> String.valueOf(buildTimestamp.getTime()));
        lookups.put("i", () -> INDEX_PLACEHOLDER);

        return new FormatParameterReplacer(lookups).replace(containerNamePattern);
    }


    // Filter out any older indexed containernames, keeping only the last one (i.e. with the highest index)
    private static Collection<Container> keepOnlyLastIndexedContainer(Collection<Container> existingContainers, final String partiallyApplied) {

        Collection<Container> result = new ArrayList<>(existingContainers);

        // No index place holder, so nothing to filters
        if (!partiallyApplied.contains(INDEX_PLACEHOLDER)) {
            return result;
        }

        Map<String,Container> containerMap = existingContainers.stream().collect(Collectors.toMap(Container::getName, Function.identity()));

        Container last = null;
        for (long i = 1; i < Long.MAX_VALUE; i++) {
            final String withIndexApplied = partiallyApplied.replaceAll(INDEX_PLACEHOLDER, String.valueOf(i));
            Container mapped = containerMap.get(withIndexApplied);
            if (mapped != null) {
                result.remove(mapped);
                last = mapped;
            } else {
                // Readd last one removed (if any)
                if (last != null) {
                    result.add(last);
                }
                return result;
            }
        }
        throw new IllegalStateException("Internal error: Cannot find a free container index slot in " + existingContainers);
    }

    private static Set<String> extractContainerNames(final Collection<Container> existingContainers) {
        final ImmutableSet.Builder<String> containerNamesBuilder = ImmutableSet.builder();
        for (final Container container : existingContainers) {
            containerNamesBuilder.add(container.getName());
        }
        return containerNamesBuilder.build();
    }

    private static String extractContainerNamePattern(ImageConfiguration image, String defaultContainerNamePattern) {
        RunImageConfiguration runConfig = image.getRunConfiguration();
        if (runConfig != null && runConfig.getContainerNamePattern() != null) {
            return runConfig.getContainerNamePattern();
        }
        return defaultContainerNamePattern != null ? defaultContainerNamePattern : DEFAULT_CONTAINER_NAME_PATTERN;
    }

    private static String cleanImageName(final String imageName) {
        return new ImageName(imageName).getSimpleName().replaceAll("[^a-zA-Z0-9_.-]+", "_");
    }
}
