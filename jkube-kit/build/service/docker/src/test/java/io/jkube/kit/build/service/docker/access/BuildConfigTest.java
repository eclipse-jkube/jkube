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
package io.jkube.kit.build.service.docker.access;

import java.util.Collections;
import java.util.Map;

import io.jkube.kit.common.JsonFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author roland
 * @since 03/01/17
 */
public class BuildConfigTest {

    @Test
    public void empty() {
        BuildOptions opts = new BuildOptions();
        assertEquals(0, opts.getOptions().size());
    }

    @Test
    public void forcerm() {
        BuildOptions opts = new BuildOptions().forceRemove(false);
        assertEquals(0, opts.getOptions().size());
        opts = new BuildOptions().forceRemove(true);
        assertEquals("1", opts.getOptions().get("forcerm"));
    }

    @Test
    public void nocache() {
        BuildOptions opts = new BuildOptions().noCache(true);
        assertEquals("1", opts.getOptions().get("nocache"));
        opts = new BuildOptions().noCache(false);
        assertEquals("0", opts.getOptions().get("nocache"));
    }

    @Test
    public void dockerfile() {
        BuildOptions opts = new BuildOptions().dockerfile("blub");
        assertEquals("blub", opts.getOptions().get("dockerfile"));
        opts = new BuildOptions().dockerfile(null);
        assertEquals(0, opts.getOptions().size());
    }

    @Test
    public void buildArgs() {
        Map<String,String> args = Collections.singletonMap("arg1", "blub");
        BuildOptions opts = new BuildOptions().buildArgs(args);
        assertEquals(JsonFactory.newJsonObject(args).toString(), opts.getOptions().get("buildargs"));
        opts = new BuildOptions().buildArgs(null);
        assertEquals(0, opts.getOptions().size());

    }

    @Test
    public void override() {
        BuildOptions opts = new BuildOptions(Collections.singletonMap("nocache", "1"));
        assertEquals(1, opts.getOptions().size());
        assertEquals("1", opts.getOptions().get("nocache"));
        opts.noCache(false);
        assertEquals("0", opts.getOptions().get("nocache"));
        opts.addOption("nocache","1");
        assertEquals("1", opts.getOptions().get("nocache"));
    }
}
