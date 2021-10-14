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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Timestamp holding a {@link LocalDateTime} and nano seconds and which can be compared.
 *
 */
public class Timestamp implements Comparable<Timestamp> {

    private LocalDateTime date;
    private int rest;

    private static final Pattern TS_PATTERN = Pattern.compile("^(.*?)(?:\\.(\\d{3})(\\d*))?(Z|[+\\-][\\d:]+)?$",Pattern.CASE_INSENSITIVE);

    /**
     * Create a timestamp for *now*
     *
     */
    public Timestamp() {
        date = LocalDateTime.now();
    }

    /**
     * Create a timestamp by parsing the given string representation which must be in an extended ISO 8601 Format
     * with Nanoseconds since this is the format used by Docker for logging (e.g. "2014-11-24T22:34:00.761764812Z")
     *
     * @param spec date specification to parse
     */
    public Timestamp(String spec) {
        //
        Matcher matcher = TS_PATTERN.matcher(spec);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid timestamp '" + spec + "' given.");
        }
        String millis = matcher.group(2);
        String rest = matcher.group(3);
        this.rest = rest != null ? Integer.parseInt(rest) : 0;
        date = LocalDateTime.parse(matcher.group(1) + (millis != null ? "." + millis : ".000") + matcher.group(4), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public LocalDateTime getDate() {
        return date;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Timestamp timestamp = (Timestamp) o;

        if (rest != timestamp.rest) return false;
        if (!date.equals(timestamp.date)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = date.hashCode();
        result = 31 * result + (int) (rest ^ (rest >>> 32));
        return result;
    }

    @Override
    public int compareTo(Timestamp ts) {
        int fc = this.date.compareTo(ts.date);
        if (fc != 0) {
            return fc;
        }
        return this.rest - ts.rest;
    }

    @Override
    public String toString() {
        return date.toString();
    }

}
