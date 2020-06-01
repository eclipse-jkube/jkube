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
package org.eclipse.jkube.enricher.generic;

import org.eclipse.jkube.kit.config.resource.JKubeAnnotations;
import org.eclipse.jkube.kit.config.resource.OpenShiftAnnotations;
import org.eclipse.jkube.kit.config.resource.PlatformMode;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GitEnricherTest {

    private static final String GIT_REMOTE_URL = "https://github.com:jkubeio/eclipse-jkube-demo-project.git";
    private static final String GIT_BRANCH = "master";
    private static final String GIT_COMMIT_ID = "058bed285de43aac80b5bf9433b9a3a9c3915e19";

    @Test
    public void testAnnotationsAddedInKubernetesPlatformMode() {
        // Given
        Map<String, String> annotations;

        // When
        annotations = GitEnricher.getAnnotations(PlatformMode.kubernetes, GIT_REMOTE_URL, GIT_BRANCH, GIT_COMMIT_ID);

        // Then
        assertJkubeAnnotations(annotations);
    }

    @Test
    public void testAnnotationsAddedInOpenShiftPlatformMode() {
        // Given
        Map<String, String> annotations;

        // When
        annotations = GitEnricher.getAnnotations(PlatformMode.openshift, GIT_REMOTE_URL, GIT_BRANCH, GIT_COMMIT_ID);

        // Then
        assertJkubeAnnotations(annotations);
        assertEquals(GIT_BRANCH, annotations.get(OpenShiftAnnotations.VCS_REF.value()));
        assertEquals(GIT_REMOTE_URL, annotations.get(OpenShiftAnnotations.VCS_URI.value()));
    }

    @Test
    public void testAnnotationsAddedWithAllNullValues() {
        // Given
        Map<String, String> annotations;

        // When
        annotations = GitEnricher.getAnnotations(PlatformMode.kubernetes, null, null, null);

        // Then
        assertTrue(annotations.isEmpty());
    }

    @Test
    public void testAnnotationsAddedWithNullCommitValues() {
        // Given
        Map<String, String> annotations;

        // When
        annotations = GitEnricher.getAnnotations(PlatformMode.kubernetes, GIT_REMOTE_URL, GIT_BRANCH, null);

        // Then
        assertJkubeAnnotationsRemoteUrlAndBranch(annotations);
    }

    private void assertJkubeAnnotations(Map<String, String> annotations) {
        assertJkubeAnnotationsRemoteUrlAndBranch(annotations);
        assertEquals(GIT_COMMIT_ID, annotations.get(JKubeAnnotations.GIT_COMMIT.value()));
    }

    private void assertJkubeAnnotationsRemoteUrlAndBranch(Map<String, String> annotations) {
        assertEquals(GIT_REMOTE_URL, annotations.get(JKubeAnnotations.GIT_URL.value()));
        assertEquals(GIT_BRANCH, annotations.get(JKubeAnnotations.GIT_BRANCH.value()));
    }
}
