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
package org.eclipse.jkube.kit.config.image.build;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;

import org.eclipse.jkube.kit.common.util.EnvUtil;

@Getter
@Setter
@EqualsAndHashCode
public class Arguments implements Serializable {

    private static final long serialVersionUID = -7896288279513401758L;

    private String shell;
    private List<String> exec;
    /**
     * Used to distinguish between shorter version
     *
     * <pre>
     *   &lt;cmd&gt;
     *     &lt;arg&gt;echo&lt;/arg&gt;
     *     &lt;arg&gt;Hello, world!&lt;/arg&gt;
     *   &lt;/cmd&gt;
     * </pre>
     *
     * from the full one
     *
     * <pre>
     *   &lt;cmd&gt;
     *     &lt;exec&gt;
     *       &lt;arg&gt;echo&lt;/arg&gt;
     *       &lt;arg&gt;Hello, world!&lt;/arg&gt;
     *     &lt;exec&gt;
     *   &lt;/cmd&gt;
     * </pre>
     *
     * and throw a validation error if both specified.
     */
    private List<String> execInlined;

    /**
     * Used to support shell specified as a default parameter, e.g.
     *
     * <pre>
     *   &lt;cmd&gt;java -jar $HOME/server.jar&lt;/cmd&gt;
     * </pre>
     *
     * Read <a href="http://blog.sonatype.com/2011/03/configuring-plugin-goals-in-maven-3/#.VeR3JbQ56Rv">more</a> on
     * this and other useful techniques.
     *
     * @param shell shell provided as string
     */
    public void set(String shell) {
        setShell(shell);
    }

    public Arguments() {
        this(null, null, null);
    }

    @Builder
    public Arguments(String shell, @Singular("execArgument") List<String> exec, @Singular("execInlinedArgument") List<String> execInlined) {
        this.shell = shell;
        this.exec = exec;
        this.execInlined = Optional.ofNullable(execInlined).orElse(new ArrayList<>());
    }

    public List<String> getExec() {
        return Optional.ofNullable(exec).orElse(execInlined);
    }

    public void validate() {
        int valueSources = 0;
        if (shell != null) {
            valueSources ++;
        }
        if (exec != null && !exec.isEmpty()) {
            valueSources ++;
        }
        if (!execInlined.isEmpty()) {
            valueSources ++;
        }

        if (valueSources != 1){
            throw new IllegalArgumentException("Argument conflict: either shell or args should be specified and only in one form.");
        }
    }

    public List<String> asStrings() {
        if (shell != null) {
            return Arrays.asList(EnvUtil.splitOnSpaceWithEscape(shell));
        }
        if (exec != null) {
            return Collections.unmodifiableList(exec);
        }
        return Collections.unmodifiableList(execInlined);
    }

}
