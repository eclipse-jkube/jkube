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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.eclipse.jkube.kit.common.util.EnvUtil.firstRegistryOf;
import static org.eclipse.jkube.kit.common.util.EnvUtil.isWindows;
import static org.eclipse.jkube.kit.common.util.EnvUtil.loadTimestamp;
import static org.eclipse.jkube.kit.common.util.EnvUtil.storeTimestamp;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
public class EnvUtilTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testConvertTcpToHttpsUrl() {
        // Given
        String urlWithHttpsPort = "tcp://0.0.0.0:2376";
        // When
        String result1 = EnvUtil.convertTcpToHttpUrl(urlWithHttpsPort);
        // Then
        assertThat(result1).isEqualTo("https://0.0.0.0:2376");
    }

    @Test
    public void testConvertTcpToHttpUrl() {
        // Given
        String urlWithHttpPort="tcp://0.0.0.0:2375";
        // When
        String result2 = EnvUtil.convertTcpToHttpUrl(urlWithHttpPort);
        // Then
        assertThat(result2).isEqualTo("http://0.0.0.0:2375");
    }

    @Test
    public void testConvertTcpToHttpUrlShouldDefaultToHttps() {
        // Given
        String url = "tcp://127.0.0.1:32770";
        // When
        String result = EnvUtil.convertTcpToHttpUrl(url);
        // Then
        assertThat(result).isEqualTo("https://127.0.0.1:32770");
    }

    @Test
    public void testExtractLargerVersionWhenBothNull(){
        assertThat(EnvUtil.extractLargerVersion(null,null)).isNull();
    }
    @Test
    public void testExtractLargerVersionWhenBIsNull() {
        //Given
        String versionA = "4.0.2";
        //When
        String result = EnvUtil.extractLargerVersion(versionA,null);
        //Then
        assertThat(versionA).isEqualTo(result);
    }
    @Test
    public void testExtractLargerVersionWhenAIsNull() {
        //Given
        String versionB = "3.1.1.0";
        //When
        String result = EnvUtil.extractLargerVersion(null,versionB);
        //Then
        assertThat(versionB).isEqualTo(result);
    }

    @Test
    public void testExtractLargerVersion() {
        //Given
        //When
        String result = EnvUtil.extractLargerVersion("4.0.0.1","4.0.0");
        //Then
        assertThat(result).isEqualTo("4.0.0.1");
    }

    @Test
    public void testGreaterOrEqualsVersionWhenTrue() {
        //Given
        String versionA = "4.0.2";
        String versionB = "3.1.1.0";
        //When
        boolean result1 = EnvUtil.greaterOrEqualsVersion(versionA,versionB);
        //Then
        assertThat(result1).isTrue();
    }

    @Test
    public void testGreaterOrEqualsVersionWhenEqual() {
        //Given
        String versionA = "4.0.2";
        //When
        boolean result2 = EnvUtil.greaterOrEqualsVersion("4.0.2", versionA);
        //Then
        assertThat(result2).isTrue();
    }


    @Test
    public void testGreaterOrEqualsVersionWhenFalse() {
        //Given
        String versionA = "4.0.2";
        String versionB = "3.1.1.0";
        //When
        boolean result3 = EnvUtil.greaterOrEqualsVersion(versionB,versionA);
        //Then
        assertThat(result3).isFalse();
    }

    @Test(expected = IllegalArgumentException.class)
    @Ignore
// TODO: Remove when implementation is fixed
    public void testGreaterOrEqualsVersionCornerCase() {
        //Given
        String versionA = "asdw4.0.2";
        String versionB = "3.1.1.0{0.1}";
        //When
        String result = EnvUtil.extractLargerVersion(versionA, versionB);
        //Then
        fail("Exception should have thrown");
    }

    @Test
    public void testSplitOnLastColonWhenNotNull() {
        // Given
        List<String> list1 = Collections.singletonList("element1:element2");
        // When
        List<String[]> result1 = EnvUtil.splitOnLastColon(list1);
        // Then
        assertThat(result1).hasSize(1);
        assertThat(result1.get(0)).hasSize(2);
        assertThat(result1.get(0)).isEqualTo(new String[]{"element1", "element2"});
    }

    @Test
    public void testSplitOnLastColonWhenNull() {
        // Given
        List<String> list2 = null;
        // When
        List<String[]> result2 = EnvUtil.splitOnLastColon(list2);
        // Then
        assertThat(result2).isEmpty();
    }

    @Test
    public void testRemoveEmptyEntrieWhenNotNull(){
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
    public void testRemoveEmptyEntriesWhenNull(){
        //Given
        List<String>  string2 = new ArrayList<>();
        string2.add(null);
        //When
        List<String>  result2 = EnvUtil.removeEmptyEntries(string2);
        //Then
        assertThat(result2).isEmpty();
    }


    @Test
    public void testSplitAtCommasAndTrimWhenNotNull(){
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
    public void testSplitAtCommasAndTrimWhenNull(){
        //Given
        Iterable<String>  strings2 = Collections.singleton(null);
        //When
        List<String> result2 = EnvUtil.splitAtCommasAndTrim(strings2);
        //Then
        assertThat(result2).isEmpty();
    }

    @Test
    public void testExtractFromPropertiesAsList() {
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
    public void testExtractFromPropertiesAsMap(){
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
    public void testFormatDurationTill() {
        long startTime = System.currentTimeMillis() - 200L;
        assertThat(EnvUtil.formatDurationTill(startTime)).contains("milliseconds");
    }

    @Test
    public void testFormatDurationTillHoursMinutesAndSeconds() {
        long startTime = System.currentTimeMillis() - (60*60*1000 + 60*1000 + 1000);
        String formattedDuration = EnvUtil.formatDurationTill(startTime);
        assertThat(formattedDuration).contains("1 hour, 1 minute and 1 second");
    }

    @Test
    public void testFirstRegistryOf() {
        assertThat(firstRegistryOf("quay.io", "docker.io", "registry.access.redhat.io")).isEqualTo("quay.io");
        assertThat(firstRegistryOf(null, null, "registry.access.redhat.io")).isEqualTo("registry.access.redhat.io");
    }

    @Test
    public void testPrepareAbsolutePath() {
        assumeFalse(isWindows());
        assertThat(EnvUtil.prepareAbsoluteOutputDirPath("target", "test-project", "testDir", "bar").getPath())
                .isEqualTo("test-project/target/testDir/bar");
        assertThat(EnvUtil.prepareAbsoluteOutputDirPath("target", "test-project", "testDir", "/home/redhat/jkube").getPath())
                .isEqualTo("/home/redhat/jkube");
    }

    @Test
    public void testPrepareAbsolutePathWindows() {
        assumeTrue(isWindows());
        assertThat( EnvUtil.prepareAbsoluteOutputDirPath("target", "test-project", "testDir", "bar").getPath())
                .isEqualTo("test-project\\target\\testDir\\bar");
        assertThat(EnvUtil.prepareAbsoluteOutputDirPath("target", "test-project", "testDir", "C:\\users\\redhat\\jkube").getPath())
                .isEqualTo("C:\\users\\redhat\\jkube");
    }

    @Test
    public void testPrepareAbsoluteSourceDirPath() {
        assumeFalse(isWindows());
        assertThat(EnvUtil.prepareAbsoluteSourceDirPath("target", "test-project", "testDir").getPath())
                .isEqualTo("test-project/target/testDir");
        assertThat(EnvUtil.prepareAbsoluteSourceDirPath("target", "test-project", "/home/redhat/jkube").getPath())
                .isEqualTo("/home/redhat/jkube");
    }

    @Test
    public void testPrepareAbsoluteSourceDirPathWindows() {
        assumeTrue(isWindows());
        assertThat(EnvUtil.prepareAbsoluteSourceDirPath("target", "test-project", "testDir").getPath())
                .isEqualTo("test-project\\target\\testDir");
        assertThat(EnvUtil.prepareAbsoluteSourceDirPath("target", "test-project", "C:\\users\\redhat\\jkube").getPath())
                .isEqualTo("C:\\users\\redhat\\jkube");
    }

    @Test
    public void testStringJoin(){
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
    public void testExtractMavenPropertyName() {
        assertThat(EnvUtil.extractMavenPropertyName("${project.baseDir}")).isEqualTo("project.baseDir");
        assertThat(EnvUtil.extractMavenPropertyName("roject.notbaseDi")).isNull();
    }

    @Test
    public void testFixupPathWhenNotWindows(){
        //Given
        String test2 = "/etc/ip/";
        //When
        String result2 = EnvUtil.fixupPath(test2);
        //Then
        assertThat(result2).isEqualTo("/etc/ip/");
    }

    @Test
    public void testFixupPathWhenWindows(){
        //Given
        String test1 = "c:\\...\\";
        //When
        String result1 = EnvUtil.fixupPath(test1);
        //Then
        assertThat(result1).isEqualTo("/c/.../");
    }

    @Test
    public void testEnsureRegistryHttpUrlIsTrue(){
        //Given
        String url1 = "http://registor";
        //When
        String result1 = EnvUtil.ensureRegistryHttpUrl(url1);
        //Then
        assertThat(result1).isEqualTo("http://registor");
    }

    @Test
    public void testEnsureRegistryHttpUrlIsNotHttp(){
        //Given
        String url2 = "registerurl";
        //When
        String result2 = EnvUtil.ensureRegistryHttpUrl(url2);
        //Then
        assertThat(result2).isEqualTo("https://registerurl");
    }

    @Test
    public void testStoreTimestamp() throws IOException {
        // Given
        final File fileToStoreTimestamp = new File(temporaryFolder.getRoot(), UUID.randomUUID().toString());
        final Date date = new Date(1445385600000L);
        // When
        storeTimestamp(fileToStoreTimestamp, date);
        // Then
        assertThat(fileToStoreTimestamp)
            .exists()
            .hasContent("1445385600000");
    }

    @Test
    public void testLoadTimestampShouldLoadFromFile() throws Exception {
        // Given
        final File file = new File(EnvUtilTest.class.getResource("/util/loadTimestamp.timestamp").getFile());
        // When
        final Date timestamp = loadTimestamp(file);
        // Then
        assertThat(timestamp).isEqualTo(new Date(1445385600000L));
    }

    @Test
    public  void testIsWindowsFalse(){
        //Given
        String oldOsName = System.getProperty("os.name");
        System.setProperty("os.name", "random");
        // TODO : Replace this when https://github.com/eclipse/jkube/issues/958 gets fixed
        //When
        boolean result= EnvUtil.isWindows();
        //Then
        assertThat(result).isFalse();
        System.setProperty("os.name", oldOsName);
    }

    @Test
    public  void testIsWindows(){
        //Given
        String oldOsName = System.getProperty("os.name");
        System.setProperty("os.name", "windows");
        // TODO : Replace this when https://github.com/eclipse/jkube/issues/958 gets fixed
        //When
        boolean result= EnvUtil.isWindows();
        //Then
        assertThat(result).isTrue();
        System.setProperty("os.name", oldOsName);
    }

    @Test
    public void testSystemPropertyRead() {
        System.setProperty("testProperty", "testPropertyValue");
        String propertyValue =
                EnvUtil.getEnvVarOrSystemProperty("testProperty", "defaultValue");
        assertThat(propertyValue).isEqualTo("testPropertyValue");
        System.clearProperty("testProperty");
    }

    @Test
    public void testDefaultSystemPropertyRead() {
        String propertyValue =
                EnvUtil.getEnvVarOrSystemProperty("testProperty", "defaultValue");
        assertThat(propertyValue).isEqualTo("defaultValue");
    }
}
