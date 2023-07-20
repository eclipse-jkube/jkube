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

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author roland
 * @since 04.11.17
 */
public class FormatParameterReplacer {

    // Detect format elements within the name
    private final Pattern formatIdentifierPattern = Pattern.compile("^(.*?)%([^a-zA-Z]*)([a-zA-Z])(.*)$");

    private final Map<String, Lookup> lookupMap;

    public FormatParameterReplacer(Map<String, Lookup> lookupMap) {
        this.lookupMap = lookupMap;
    }

    public synchronized String replace(String input) {
        StringBuilder ret = new StringBuilder();
        while (true) {
            Matcher matcher = formatIdentifierPattern.matcher(input);
            if (!matcher.matches()) {
                ret.append(input);
                return ret.toString();
            }
            ret.append(matcher.group(1));
            ret.append(formatElement(matcher.group(2),matcher.group(3)));
            input = matcher.group(4);
        }
    }

    private String formatElement(String options, String what) {
        FormatParameterReplacer.Lookup lookup = lookupMap.get(what);
        if (lookup == null) {
            throw new IllegalArgumentException(String.format("No image name format element '%%%s' known", what) );
        }
        String val = lookup.lookup();
        return String.format("%" + (options != null ? options : "") + "s",val);
    }


    // Lookup abstraction
    public interface Lookup {
        String lookup();
    }

}
