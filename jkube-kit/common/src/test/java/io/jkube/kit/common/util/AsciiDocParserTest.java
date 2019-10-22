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
package io.jkube.kit.common.util;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.assertThat;

public class AsciiDocParserTest {

    private static final String VALID_TABLE = "cols=2*,options=\"header\"]" + System.lineSeparator()
        + "|===" + System.lineSeparator()
        + "|Kind" + System.lineSeparator()
        + "|Filename Type" + System.lineSeparator()
        + System.lineSeparator()
        + "|ConfigMap" + System.lineSeparator()
        + "a|`cm`, `configmap`" + System.lineSeparator()
        + System.lineSeparator()
        + "|CronJob" + System.lineSeparator()
        + "a|`cronjob`" + System.lineSeparator()
        + "|===";

    private static final String NONE_END_VALID_TABLE = "cols=2*,options=\"header\"]" + System.lineSeparator()
        + "|===" + System.lineSeparator()
        + "|Kind" + System.lineSeparator()
        + "|Filename Type" + System.lineSeparator()
        + System.lineSeparator()
        + "|cm" + System.lineSeparator()
        + "a|ConfigMap" + System.lineSeparator()
        + System.lineSeparator()
        + "|cronjob" + System.lineSeparator()
        + "|CronJob" + System.lineSeparator();

    private static final String INVALID_TABLE_WITH_THREE_COLUMNS = "cols=2*,options=\"header\"]" + System.lineSeparator()
        + "|===" + System.lineSeparator()
        + "|Kind" + System.lineSeparator()
        + "|Filename Type" + System.lineSeparator()
        + System.lineSeparator()
        + "|cm" + System.lineSeparator()
        + "a|ConfigMap" + System.lineSeparator()
        + "a|ConfigMap" + System.lineSeparator()
        + System.lineSeparator()
        + "|cronjob" + System.lineSeparator()
        + "|CronJob" + System.lineSeparator()
        + "|===";

    @Test
    public void should_serialize_kind_and_filename_from_valid_asciidoc_table() {

        // Given

        final AsciiDocParser asciiDocParser = new AsciiDocParser();
        final ByteArrayInputStream tableContent = new ByteArrayInputStream(VALID_TABLE.getBytes());

        // When

        final Map<String, List<String>> serializedContent = asciiDocParser.serializeKindFilenameTable(tableContent);

        // Then
        final Map<String, List<String>> expectedSerlializedContent = new HashMap<>();
        expectedSerlializedContent.put("ConfigMap", Arrays.asList("cm", "configmap"));
        expectedSerlializedContent.put("CronJob", Arrays.asList("cronjob"));

        assertThat(serializedContent)
            .containsAllEntriesOf(expectedSerlializedContent);
    }

    @Test
    public void should_throw_exception_if_no_end_of_table() {

        // Given

        final AsciiDocParser asciiDocParser = new AsciiDocParser();
        final ByteArrayInputStream tableContent = new ByteArrayInputStream(NONE_END_VALID_TABLE.getBytes());

        // When

        Throwable error = catchThrowable(() -> asciiDocParser.serializeKindFilenameTable(tableContent));

        //Then

        assertThat(error).isInstanceOf(IllegalArgumentException.class);

    }

    @Test
    public void should_throw_exception_if_more_than_two_columns_are_present() {

        // Given

        final AsciiDocParser asciiDocParser = new AsciiDocParser();
        final ByteArrayInputStream tableContent = new ByteArrayInputStream(INVALID_TABLE_WITH_THREE_COLUMNS.getBytes());

        // When

        Throwable error = catchThrowable(() -> asciiDocParser.serializeKindFilenameTable(tableContent));

        //Then

        assertThat(error).isInstanceOf(IllegalArgumentException.class);

    }
}
