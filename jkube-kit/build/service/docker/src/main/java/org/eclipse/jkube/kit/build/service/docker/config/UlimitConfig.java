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
package org.eclipse.jkube.kit.build.service.docker.config;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Configuration for ulimit
 *
 * @since 0.15
 * @author Alexis Thaveau
 */
public class UlimitConfig implements Serializable {

    private String name;

    private Integer hard;

    private Integer soft;

    public UlimitConfig(String name, Integer hard, Integer soft) {
        this.name = name;
        this.hard = hard;
        this.soft = soft;
    }

    public String getName() {
        return name;
    }

    public Integer getHard() {
        return hard;
    }

    public Integer getSoft() {
        return soft;
    }

    Pattern ULIMIT_PATTERN = Pattern.compile("^(?<name>[^=]+)=(?<hard>[^:]*):?(?<soft>[^:]*)$");

    public UlimitConfig() {}

    public UlimitConfig(String ulimit) {
        Matcher matcher = ULIMIT_PATTERN.matcher(ulimit);
        if (matcher.matches()) {
            name = matcher.group("name");
            hard = asInteger(matcher.group("hard"));
            soft = asInteger(matcher.group("soft"));
        } else {
            throw new IllegalArgumentException("Invalid ulimit specification " + ulimit);
        }
    }

    private Integer asInteger(String number) {
        if (number == null || number.length() == 0) {
            return null;
        }
        return Integer.parseInt(number);
    }

    public String serialize() {
        if(hard != null && soft != null) {
            return name + "="+hard+":"+soft;
        } else if(hard != null) {
            return name + "="+hard;
        } else if(soft != null) {
            return name + "=:"+soft;
        } else {
            return null;
        }
    }
}

