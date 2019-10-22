/**
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package io.jkube.kit.profile;

import io.jkube.kit.config.resource.ProcessorConfig;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author roland
 * @since 24/07/16
 */
public class ProfileUtilTest {

    @Test
    public void simple() throws IOException {
        InputStream is = getClass().getResourceAsStream("/jkube/config/profiles-lookup-dir/profiles.yaml");
        assertNotNull(is);
        List<Profile> profiles = ProfileUtil.fromYaml(is);
        assertNotNull(profiles);
        assertEquals(profiles.size(),3);
        Profile profile = profiles.get(0);
        assertEquals("simple", profile.getName());
        ProcessorConfig config = profile.getEnricherConfig();
        assertTrue(config.use("base"));
        assertFalse(config.use("blub"));
        config = profile.getGeneratorConfig();
        assertFalse(config.use("java.app"));
        assertTrue(config.use("spring.swarm"));
    }

    @Test
    public void multiple() throws IOException {
        InputStream is = getClass().getResourceAsStream("/jkube/config/ProfileUtilTest-multiple.yml");
        assertNotNull(is);
        List<Profile> profiles = ProfileUtil.fromYaml(is);
        assertEquals(2,profiles.size());
    }

    @Test
    public void fromClasspath() throws IOException {
        List<Profile> profiles = ProfileUtil.readAllFromClasspath("one", "");
        assertEquals(1, profiles.size());
        assertNotNull(profiles.get(0));
    }

    @Test
    public void lookup() throws IOException, URISyntaxException {
        File dir = getProfileDir();
        Profile profile = ProfileUtil.lookup("simple", dir);
        assertEquals("simple", profile.getName());
        assertEquals("http://jolokia.org", profile.getEnricherConfig().getConfig("base","url"));

        profile = ProfileUtil.lookup("one", dir);
        assertTrue(profile.getGeneratorConfig().use("foobar"));

        assertNull(ProfileUtil.lookup("three", dir));
    }

    public File getProfileDir() throws URISyntaxException {
        return new File(getClass().getResource("/jkube/config/profiles-lookup-dir/profiles.yaml").toURI()).getParentFile();
    }

    @Test
    public void findProfile() throws URISyntaxException, IOException {
        assertNotNull(ProfileUtil.findProfile("simple", getProfileDir()));
        try {
            ProfileUtil.findProfile("not-there", getProfileDir());
            fail();
        } catch (IllegalArgumentException exp) {
            assertTrue(exp.getMessage().contains("not-there"));
        }

    }

    @Test
    public void mergeProfiles() throws Exception {
        Profile profile = ProfileUtil.findProfile("merge-1", getProfileDir());
        assertFalse(profile.getEnricherConfig().use("jkube-project-label"));
        assertTrue(profile.getEnricherConfig().use("jkube-image"));
    }

    @Test
    public void blendProfiles() throws Exception {

        ProcessorConfig origConfig = new ProcessorConfig(Arrays.asList("i1", "i2"), Collections.singleton("spring.swarm"), null);
        ProcessorConfig mergeConfig = ProfileUtil.blendProfileWithConfiguration(ProfileUtil.ENRICHER_CONFIG,
                                                                                "simple",
                                                                                getProfileDir(),
                                                                                origConfig);
        assertTrue(mergeConfig.use("base"));
        assertTrue(mergeConfig.use("i1"));
        assertEquals(mergeConfig.getConfig("base", "url"),"http://jolokia.org");


        mergeConfig = ProfileUtil.blendProfileWithConfiguration(ProfileUtil.GENERATOR_CONFIG,
                                                                "simple",
                                                                getProfileDir(),
                                                                origConfig);
        assertTrue(mergeConfig.use("i2"));
        assertFalse(mergeConfig.use("spring.swarm"));

    }

    @Test
    public void shouldExtend() throws Exception {
        Profile aProfile = ProfileUtil.findProfile("minimal", getProfileDir());

        assertEquals("minimal", aProfile.getName());
        assertEquals("simple", aProfile.getParentProfile());

        assertTrue(aProfile.getEnricherConfig().use("jkube-name"));
        assertTrue(aProfile.getEnricherConfig().use("default.service"));
        assertTrue(aProfile.getGeneratorConfig().use("spring.swarm"));
        assertFalse(aProfile.getGeneratorConfig().use("java.app"));
    }
}
