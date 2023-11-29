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
package org.eclipse.jkube.kit.common.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jkube.kit.common.util.EnvUtil.firstRegistryOf;
import static org.eclipse.jkube.kit.common.util.EnvUtil.loadTimestamp;
import static org.eclipse.jkube.kit.common.util.EnvUtil.storeTimestamp;

class EnvUtilTest {

    @Test
    void testSplitOnLastColonWhenNotNull() {
        // Given
        List<String> list1 = Collections.singletonList("element1:element2");
        // When
        List<String[]> result1 = EnvUtil.splitOnLastColon(list1);
        // Then
        assertThat(result1)
                .hasSize(1)
                .first(InstanceOfAssertFactories.type(String[].class))
                .isEqualTo(new String[]{"element1", "element2"});
    }

    @Test
    void testSplitOnLastColonWhenNull() {
        // Given
        List<String> list2 = null;
        // When
        List<String[]> result2 = EnvUtil.splitOnLastColon(list2);
        // Then
        assertThat(result2).isEmpty();
    }

    @Test
    void testRemoveEmptyEntriesWhenNotNull(){
        //Given
        List<String>  string1 = new ArrayList<>();
        string1.add(" set ");
        string1.add(" set2  ");
        string1.add("");
        //When
        List<String>  result1 = EnvUtil.removeEmptyEntries(string1);
        //Then
        assertThat(result1.toArray()).isEqualTo(new String[]{"set", "set2"});
    }

    @Test
    void testRemoveEmptyEntriesWhenNull(){
        //Given
        List<String>  string2 = new ArrayList<>();
        string2.add(null);
        //When
        List<String>  result2 = EnvUtil.removeEmptyEntries(string2);
        //Then
        assertThat(result2).isEmpty();
    }


    @Test
    void testSplitAtCommasAndTrimWhenNotNull(){
        //Given
        Iterable<String>  strings1 = Collections.singleton("hello,world");
        //When
        List<String> result1 = EnvUtil.splitAtCommasAndTrim(strings1);
        //Then
        assertThat(result1).hasSize(2)
                .last()
                .isEqualTo("world");
    }

    @Test
    void testSplitAtCommasAndTrimWhenNull(){
        //Given
        Iterable<String>  strings2 = Collections.singleton(null);
        //When
        List<String> result2 = EnvUtil.splitAtCommasAndTrim(strings2);
        //Then
        assertThat(result2).isEmpty();
    }

    @Test
    void testExtractFromPropertiesAsList() {
        //Given
        String string = "key";
        Properties properties = new Properties();
        properties.put("key.name","value");
        properties.put("key.value","valu");
        properties.put("art","id");
        properties.put("note","bool");
        properties.put("key._combine","bool");
        //When
        List<String> result = EnvUtil.extractFromPropertiesAsList(string,properties);
        //Then
        assertThat(result)
            .hasSize(2)
            .containsExactly("valu", "value");
    }

    @Test
    void testExtractFromPropertiesAsMap(){
        //Given
        String prefix = "key";
        Properties properties = new Properties();
        properties.put("key.name","value");
        properties.put("key.value","valu");
        properties.put("art","id");
        properties.put("note","bool");
        properties.put("key._combine","bool");
        //when
        Map<String, String> result = EnvUtil.extractFromPropertiesAsMap(prefix,properties);
        //Then
        assertThat(result)
            .hasSize(2)
            .containsEntry("name", "value");
    }

    @Test
    void testFormatDurationTill() {
        long startTime = System.currentTimeMillis() - 200L;
        assertThat(EnvUtil.formatDurationTill(startTime)).contains("milliseconds");
    }

    @Test
    void testFormatDurationTillHoursMinutesAndSeconds() {
        long startTime = System.currentTimeMillis() - (60*60*1000 + 60*1000 + 1000);
        String formattedDuration = EnvUtil.formatDurationTill(startTime);
        assertThat(formattedDuration).contains("1 hour, 1 minute and 1 second");
    }

    @Test
    void testFirstRegistryOf() {
        assertThat(firstRegistryOf("quay.io", "docker.io", "registry.access.redhat.io")).isEqualTo("quay.io");
        assertThat(firstRegistryOf(null, null, "registry.access.redhat.io")).isEqualTo("registry.access.redhat.io");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testPrepareAbsolutePath() {
        assertThat(EnvUtil.prepareAbsoluteOutputDirPath("target", "test-project", "testDir", "bar").getPath())
                .isEqualTo("test-project/target/testDir/bar");
        assertThat(EnvUtil.prepareAbsoluteOutputDirPath("target", "test-project", "testDir", "/home/redhat/jkube").getPath())
                .isEqualTo("/home/redhat/jkube");
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testPrepareAbsolutePathWindows() {
        assertThat( EnvUtil.prepareAbsoluteOutputDirPath("target", "test-project", "testDir", "bar").getPath())
                .isEqualTo("test-project\\target\\testDir\\bar");
        assertThat(EnvUtil.prepareAbsoluteOutputDirPath("target", "test-project", "testDir", "C:\\users\\redhat\\jkube").getPath())
                .isEqualTo("C:\\users\\redhat\\jkube");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void testPrepareAbsoluteSourceDirPath() {
        assertThat(EnvUtil.prepareAbsoluteSourceDirPath("target", "test-project", "testDir").getPath())
                .isEqualTo("test-project/target/testDir");
        assertThat(EnvUtil.prepareAbsoluteSourceDirPath("target", "test-project", "/home/redhat/jkube").getPath())
                .isEqualTo("/home/redhat/jkube");
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testPrepareAbsoluteSourceDirPathWindows() {
        assertThat(EnvUtil.prepareAbsoluteSourceDirPath("target", "test-project", "testDir").getPath())
                .isEqualTo("test-project\\target\\testDir");
        assertThat(EnvUtil.prepareAbsoluteSourceDirPath("target", "test-project", "C:\\users\\redhat\\jkube").getPath())
                .isEqualTo("C:\\users\\redhat\\jkube");
    }

    @Test
    void testStringJoin(){
        //Given
        List<String> list = new ArrayList<>();
        String separator = ",";
        list.add("element1");
        list.add("element2");
        //When
        String result = EnvUtil.stringJoin(list,separator);
        //Then
        assertThat(result).isEqualTo("element1,element2");
    }

    @Test
    void testExtractMavenPropertyName() {
        assertThat(EnvUtil.extractMavenPropertyName("${project.baseDir}")).isEqualTo("project.baseDir");
        assertThat(EnvUtil.extractMavenPropertyName("roject.notbaseDi")).isNull();
    }

    @Test
    void testFixupPathWhenNotWindows(){
        //Given
        String test2 = "/etc/ip/";
        //When
        String result2 = EnvUtil.fixupPath(test2);
        //Then
        assertThat(result2).isEqualTo("/etc/ip/");
    }

    @Test
    void testFixupPathWhenWindows(){
        //Given
        String test1 = "c:\\...\\";
        //When
        String result1 = EnvUtil.fixupPath(test1);
        //Then
        assertThat(result1).isEqualTo("/c/.../");
    }

    @Test
    void testEnsureRegistryHttpUrlIsTrue(){
        //Given
        String url1 = "http://registor";
        //When
        String result1 = EnvUtil.ensureRegistryHttpUrl(url1);
        //Then
        assertThat(result1).isEqualTo("http://registor");
    }

    @Test
    void testEnsureRegistryHttpUrlIsNotHttp(){
        //Given
        String url2 = "registerurl";
        //When
        String result2 = EnvUtil.ensureRegistryHttpUrl(url2);
        //Then
        assertThat(result2).isEqualTo("https://registerurl");
    }

    @Test
    void testStoreTimestamp(@TempDir File temporaryFolder) throws IOException {
        // Given
        final File fileToStoreTimestamp = new File(temporaryFolder, UUID.randomUUID().toString());
        final Date date = new Date(1445385600000L);
        // When
        storeTimestamp(fileToStoreTimestamp, date);
        // Then
        assertThat(fileToStoreTimestamp)
            .exists()
            .hasContent("1445385600000");
    }

    @Test
    void testLoadTimestampShouldLoadFromFile() throws Exception {
        // Given
        final File file = new File(EnvUtilTest.class.getResource("/util/loadTimestamp.timestamp").getFile());
        // When
        final Date timestamp = loadTimestamp(file);
        // Then
        assertThat(timestamp).isEqualTo(new Date(1445385600000L));
    }

    @Test
    void testIsWindowsFalse(){
        String oldOsName = System.getProperty("os.name");
        try {
          //Given
          System.setProperty("os.name", "random");
          // TODO : Replace this when https://github.com/eclipse/jkube/issues/958 gets fixed
          //When
          boolean result = EnvUtil.isWindows();
          //Then
          assertThat(result).isFalse();
        } finally {
          System.setProperty("os.name", oldOsName);
        }
    }

    @Test
    void testIsWindows(){
      String oldOsName = System.getProperty("os.name");
      try {
        //Given
        System.setProperty("os.name", "windows");
        // TODO : Replace this when https://github.com/eclipse/jkube/issues/958 gets fixed
        //When
        boolean result = EnvUtil.isWindows();
        //Then
        assertThat(result).isTrue();
      } finally {
        System.setProperty("os.name", oldOsName);
      }
    }

    @Test
    void getUserHome_fromProperty() {
        final String original = System.getProperty("user.home");
        try {
            System.setProperty("user.home", "/home/a-user");
            assertThat(EnvUtil.getUserHome()).isEqualTo(new File("/home/a-user"));
        } finally {
            System.setProperty("user.home", original);
        }
    }

    @Test
    void getUserHome_fromEnvironment() {
        final String original = System.getProperty("user.home");
        try {
            System.clearProperty("user.home");
            final Map<String, String> env = Collections.singletonMap("HOME", "/home/a-user");
            EnvUtil.overrideEnvGetter(env::get);
            assertThat(EnvUtil.getUserHome()).isEqualTo(new File("/home/a-user"));
        } finally {
            System.setProperty("user.home", original);
            EnvUtil.overrideEnvGetter(System::getenv);
        }
    }

    @Test
    void testSystemPropertyRead() {
        System.setProperty("testProperty", "testPropertyValue");
        try {
          String propertyValue =
              EnvUtil.getEnvVarOrSystemProperty("testProperty", "defaultValue");
          assertThat(propertyValue).isEqualTo("testPropertyValue");
        } finally {
          System.clearProperty("testProperty");
        }
    }

    @Test
    void testDefaultSystemPropertyRead() {
        String propertyValue =
                EnvUtil.getEnvVarOrSystemProperty("testProperty", "defaultValue");
        assertThat(propertyValue).isEqualTo("defaultValue");
    }

    @DisplayName("URL Conversion tests")
    @ParameterizedTest(name = "{0}")
    @MethodSource("urlTestData")
    void urlTest(String testDesc, String givenURL, String expectedURL) {
        // Given & When
        String url = EnvUtil.convertTcpToHttpUrl(givenURL);
        // Then
        assertThat(url).isEqualTo(expectedURL);
    }

    public static Stream<Arguments> urlTestData() {
        return Stream.of(
                Arguments.of("Convert TCP to HTTPS URL", "tcp://0.0.0.0:2376", "https://0.0.0.0:2376"),
                Arguments.of("Convert TCP to HTTP URL", "tcp://0.0.0.0:2375", "http://0.0.0.0:2375"),
                Arguments.of("Convert TCP to HTTP URL should default to HTTPS", "tcp://127.0.0.1:32770", "https://127.0.0.1:32770")
        );
    }

    @DisplayName("Larger version extraction Tests")
    @ParameterizedTest(name = "{0}")
    @MethodSource("extractLargerVersionTestData")
    void extractLargerVersionTest(String testDesc, String versionA, String versionB, String result) {
        // Given & When
        String version = EnvUtil.extractLargerVersion(versionA, versionB);
        // Then
        assertThat(version).isEqualTo(result);
    }

    public static Stream<Arguments> extractLargerVersionTestData() {
        return Stream.of(
                Arguments.arguments("When A is null should return B", null, "3.1.1.0", "3.1.1.0"),
                Arguments.arguments("When B is null should return A", "4.0.2", null, "4.0.2"),
                Arguments.arguments("When both are null should return null", null, null, null),
                Arguments.arguments("When both are given should return larger version", "4.0.0.1", "4.0.0", "4.0.0.1"),
                Arguments.arguments("When both are invalid should return null", "asdw4.0.2", "3.1.1.0{0.1}", null),
                Arguments.arguments("When A is invalid should return B", "3.a.b.c", "4.0.2", "4.0.2"),
                Arguments.arguments("When B is invalid should return A", "4.0.2", "3.a.b.c", "4.0.2")
        );
    }

    @DisplayName("Greater or equal version tests")
    @ParameterizedTest(name = "{0}")
    @MethodSource("greaterOrEqualVersionTestData")
    void greaterOrEqualTest(String testDesc, String versionA, String versionB, boolean expected) {
        // Given & When
        boolean result = EnvUtil.greaterOrEqualsVersion(versionA, versionB);
        // Then
        assertThat(result).isEqualTo(expected);
    }

    public static Stream<Arguments> greaterOrEqualVersionTestData(){
        return Stream.of(
                Arguments.arguments("Greater or Equal version when true", "4.0.2", "3.1.1.0", true),
                Arguments.arguments("Greater or Equal version when equal", "4.0.2", "4.0.2", true),
                Arguments.arguments("Greater or Equal version when false", "3.1.1.0", "4.0.2", false)
        );
    }
}
