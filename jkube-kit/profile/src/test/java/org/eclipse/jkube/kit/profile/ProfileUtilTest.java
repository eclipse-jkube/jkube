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
package org.eclipse.jkube.kit.profile;

import org.eclipse.jkube.kit.config.resource.ProcessorConfig;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author roland
 */
class ProfileUtilTest {

    @Test
    void fromClasspath() throws IOException {
        List<Profile> profiles = ProfileUtil.readAllFromClasspath("one", "");
        assertThat(profiles).singleElement().isNotNull();
    }

    @Test
    void lookup() throws IOException, URISyntaxException {
        File dir = getProfileDir();
        Profile profile = ProfileUtil.lookup("simple", dir);
        assertThat(profile).hasFieldOrPropertyWithValue("name", "simple")
            .extracting(c -> c.getEnricherConfig().getConfig().get("base").get("url"))
            .isEqualTo("http://jolokia.org");

        profile = ProfileUtil.lookup("one", dir);
        assertThat(profile.getGeneratorConfig().use("foobar")).isTrue();

        assertThat(ProfileUtil.lookup("three", dir)).isNull();
    }

    public File getProfileDir() throws URISyntaxException {
        return new File(getClass().getResource("/jkube/config/profiles-lookup-dir/profiles.yaml").toURI()).getParentFile();
    }

    @Test
    void findProfile_whenValidProfileArg_returnsValidProfile() throws URISyntaxException, IOException {
        assertThat(ProfileUtil.findProfile("simple", getProfileDir()))
          .isNotNull()
          .hasFieldOrPropertyWithValue("name", "simple")
            .satisfies(profile -> assertThat(profile.getEnricherConfig())
                .returns(true, c -> c.use("base"))
                .returns(false, c -> c.use("blub")))
            .satisfies(profile -> assertThat(profile.getGeneratorConfig())
                .returns(false, c -> c.use("java.app"))
                .returns(true, c -> c.use("spring.swarm")));
    }

    @Test
    void findProfile_whenNonExistentProfileArg_throwsException () throws URISyntaxException {

        List<File> profileDirs = Collections.singletonList(getProfileDir());

        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> ProfileUtil.findProfile("not-there", profileDirs));
        assertThat(illegalArgumentException).hasMessageContaining("not-there");
    }

    @Test
    void findProfile_whenProfileUsedWithInvalidParent_thenThrowsException() throws URISyntaxException {
        // Given
        File profileDir = getProfileDir();
        // When
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> ProfileUtil.findProfile("invalid-parent", profileDir));
        // Then
        assertThat(illegalArgumentException).hasMessage("No parent profile 'i-dont-exist' defined");
    }

    @Test
    void mergeProfiles() throws Exception {
        Profile profile = ProfileUtil.findProfile("merge-1", getProfileDir());
        assertThat(profile)
            .satisfies(p -> assertThat(p.getEnricherConfig())
                .returns(false, c -> c.use("jkube-project-label"))
                .returns(true, c -> c.use("jkube-image")));
    }

    @Test
    void blendProfiles() throws Exception {

        ProcessorConfig origConfig = new ProcessorConfig(Arrays.asList("i1", "i2"), Collections.singleton("spring.swarm"), null);
        ProcessorConfig mergeConfig = ProfileUtil.blendProfileWithConfiguration(ProfileUtil.ENRICHER_CONFIG,
            "simple",
            Collections.singletonList(getProfileDir()),
            origConfig);

        assertThat(mergeConfig)
            .satisfies(config -> assertThat(config)
                .returns(true, c -> c.use("base"))
                .returns(true, c -> c.use("i1"))
                .returns("http://jolokia.org", c -> c.getConfig().get("base").get("url")));

        mergeConfig = ProfileUtil.blendProfileWithConfiguration(ProfileUtil.GENERATOR_CONFIG,
            "simple",
            Collections.singletonList(getProfileDir()),
            origConfig);

        assertThat(mergeConfig)
            .satisfies(config -> assertThat(config)
                .returns(true, c -> c.use("i2"))
                .returns(false, c -> c.use("spring.swarm")));
    }

    @Test
    void shouldExtend() throws Exception {
        Profile aProfile = ProfileUtil.findProfile("minimal", getProfileDir());

        assertThat(aProfile).isNotNull()
            .hasFieldOrPropertyWithValue("name", "minimal")
            .hasFieldOrPropertyWithValue("parentProfile", "simple")
            .satisfies(profile -> assertThat(profile.getEnricherConfig())
                .returns(true, c -> c.use("jkube-name"))
                .returns(true, c -> c.use("default.service")))
            .satisfies(profile -> assertThat(profile.getGeneratorConfig())
                .returns(true, c -> c.use("spring.swarm"))
                .returns(false, c -> c.use("java.app")));
    }
}
