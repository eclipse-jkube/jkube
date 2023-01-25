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
package org.eclipse.jkube.kit.build.service.docker.access;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.eclipse.jkube.kit.common.JsonFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author roland
 * @since 03/01/17
 */
class BuildConfigTest {

    @Test
    void empty() {
        BuildOptions opts = new BuildOptions();
        assertThat(opts.getOptions()).isEmpty();
    }

    @Test
    void forcerm() {
        BuildOptions optsWithoutForcerm = new BuildOptions().forceRemove(false);
        assertThat(optsWithoutForcerm.getOptions()).isEmpty();
        BuildOptions optsWithForcerm = new BuildOptions().forceRemove(true);
        assertThat(optsWithForcerm.getOptions()).containsEntry("forcerm", "1");
    }

    @Test
    void nocache() {
        BuildOptions optsWithoutCache = new BuildOptions().noCache(true);
        assertThat(optsWithoutCache.getOptions()).containsEntry("nocache", "1");
        BuildOptions optsWithCache = new BuildOptions().noCache(false);
        assertThat(optsWithCache.getOptions()).containsEntry("nocache", "0");
    }

    @Test
    void dockerfile() {
        BuildOptions opts = new BuildOptions().dockerfile("blub");
        assertThat(opts.getOptions()).containsEntry("dockerfile", "blub");
        BuildOptions optsWithNullDockerFile = new BuildOptions().dockerfile(null);
        assertThat(optsWithNullDockerFile.getOptions()).isEmpty();
    }

    @Test
    void buildArgs() {
        Map<String,String> args = Collections.singletonMap("arg1", "blub");
        BuildOptions optsWithArgs = new BuildOptions().buildArgs(args);
        assertThat(optsWithArgs.getOptions()).containsEntry("buildargs", JsonFactory.newJsonObject(args).toString());
        BuildOptions optsWithNullArgs = new BuildOptions().buildArgs(null);
        assertThat(optsWithNullArgs.getOptions()).isEmpty();
    }

    @Test
    void override() {
        BuildOptions opts = new BuildOptions(Collections.singletonMap("nocache", "1"));
        assertThat(opts.getOptions()).hasSize(1).containsEntry("nocache", "1");

        opts.noCache(false);
        assertThat(opts.getOptions()).containsEntry("nocache", "0");

        opts.addOption("nocache","1");
        assertThat(opts.getOptions()).containsEntry("nocache", "1");
    }

    @Test
    void cachefrom() {
        BuildOptions opts = new BuildOptions().cacheFrom(Collections.singletonList("foo/bar:latest"));
        assertThat(opts.getOptions()).containsEntry("cachefrom", "[\"foo/bar:latest\"]");

        opts.cacheFrom(Arrays.asList("foo/bar:latest", "foo/baz:1.0"));
        assertThat(opts.getOptions()).containsEntry("cachefrom", "[\"foo/bar:latest\",\"foo/baz:1.0\"]");

        opts.cacheFrom(Collections.emptyList());
        assertThat(opts.getOptions()).doesNotContainKey("cachefrom");

        opts.cacheFrom(null);
        assertThat(opts.getOptions()).doesNotContainKey("cachefrom");
    }
}
