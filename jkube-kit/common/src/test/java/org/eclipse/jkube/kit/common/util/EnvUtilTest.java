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

import mockit.Expectations;
import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.Verifications;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.eclipse.jkube.kit.common.util.EnvUtil.firstRegistryOf;
import static org.eclipse.jkube.kit.common.util.EnvUtil.isWindows;
import static org.eclipse.jkube.kit.common.util.EnvUtil.loadTimestamp;
import static org.eclipse.jkube.kit.common.util.EnvUtil.storeTimestamp;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;


public class EnvUtilTest {

    @Test
    public void testConvertTcpToHttpsUrl() {
        // Given
        String urlWithHttpsPort = "tcp://0.0.0.0:2376";
        // When
        String result1 = EnvUtil.convertTcpToHttpUrl(urlWithHttpsPort);
        // Then
        assertEquals("https://0.0.0.0:2376", result1);
    }

    @Test
    public void testConvertTcpToHttpUrl() {
        // Given
        String urlWithHttpPort="tcp://0.0.0.0:2375";
        // When
        String result2 = EnvUtil.convertTcpToHttpUrl(urlWithHttpPort);
        // Then
        assertEquals("http://0.0.0.0:2375", result2);
    }

    @Test
    public void testExtractLargerVersionWhenBothNull(){
        assertNull(EnvUtil.extractLargerVersion(null,null));
    }
    @Test
    public void testExtractLargerVersionWhenBIsNull() {
        //Given
        String versionA = "4.0.2";
        //When
        String result = EnvUtil.extractLargerVersion(versionA,null);
        //Then
        assertEquals( versionA, result);
    }
    @Test
    public void testExtractLargerVersionWhenAIsNull() {
        //Given
        String versionB = "3.1.1.0";
        //When
        String result = EnvUtil.extractLargerVersion(null,versionB);
        //Then
        assertEquals( versionB, result);
    }

    @Test
    public void testExtractLargerVersion() {
        //Given
        //When
        String result = EnvUtil.extractLargerVersion("4.0.0.1","4.0.0");
        //Then
        assertEquals("4.0.0.1",result);
    }

    @Test
    public void testGreaterOrEqualsVersionWhenTrue() {
        //Given
        String versionA = "4.0.2";
        String versionB = "3.1.1.0";
        //When
        boolean result1 = EnvUtil.greaterOrEqualsVersion(versionA,versionB);
        //Then
        assertTrue(result1);
    }

    @Test
    public void testGreaterOrEqualsVersionWhenEqual() {
        //Given
        String versionA = "4.0.2";
        //When
        boolean result2 = EnvUtil.greaterOrEqualsVersion("4.0.2", versionA);
        //Then
        assertTrue(result2);
    }


    @Test
    public void testGreaterOrEqualsVersionWhenFalse() {
        //Given
        String versionA = "4.0.2";
        String versionB = "3.1.1.0";
        //When
        boolean result3 = EnvUtil.greaterOrEqualsVersion(versionB,versionA);
        //Then
        assertFalse(result3);
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
        fail();
    }

    @Test
    public void testSplitOnLastColonWhenNotNull() {
        // Given
        List<String> list1 = Collections.singletonList("element1:element2");
        // When
        List<String[]> result1 = EnvUtil.splitOnLastColon(list1);
        // Then
        assertEquals(1, result1.size());
        assertEquals(2,result1.get(0).length);
        assertArrayEquals(new String[]{"element1", "element2"}, result1.get(0));
    }

    @Test
    public void testSplitOnLastColonWhenNull() {
        // Given
        List<String> list2 = null;
        // When
        List<String[]> result2 = EnvUtil.splitOnLastColon(list2);
        // Then
        assertTrue(result2.isEmpty());
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
        assertArrayEquals( new String[]{"set", "set2"} ,result1.toArray());
    }

    @Test
    public void testRemoveEmptyEntriesWhenNull(){
        //Given
        List<String>  string2 = new ArrayList<>();
        string2.add(null);
        //When
        List<String>  result2 = EnvUtil.removeEmptyEntries(string2);
        //Then
        assertTrue(result2.isEmpty());
    }


    @Test
    public void testSplitAtCommasAndTrimWhenNotNull(){
        //Given
        Iterable<String>  strings1 = Collections.singleton("hello,world");
        //When
        List<String> result1 = EnvUtil.splitAtCommasAndTrim(strings1);
        //Then
        assertEquals(2,result1.size());
        assertEquals("world", result1.get(1));
    }

    @Test
    public void testSplitAtCommasAndTrimWhenNull(){
        //Given
        Iterable<String>  strings2 = Collections.singleton(null);
        //When
        List<String> result2 = EnvUtil.splitAtCommasAndTrim(strings2);
        //Then
        assertTrue(result2.isEmpty());
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
        assertNotNull(result);
        assertEquals(2, result.size());
        assertArrayEquals(new String[]{"valu", "value"}, result.toArray());
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
        assertNotNull(result);
        assertEquals(2 ,result.size());
        assertEquals("value",result.get("name"));
    }

    @Test
    public void testFormatDurationTill() {
        long startTime = System.currentTimeMillis() - 200L;
        assertTrue(EnvUtil.formatDurationTill(startTime).contains("milliseconds"));
    }

    @Test
    public void testFirstRegistryOf() {
        assertEquals("quay.io", firstRegistryOf("quay.io", "docker.io", "registry.access.redhat.io"));
        assertEquals("registry.access.redhat.io", firstRegistryOf(null, null, "registry.access.redhat.io"));
    }

    @Test
    public void testPrepareAbsolutePath() {
        assumeFalse(isWindows());
        assertEquals("test-project/target/testDir/bar",EnvUtil.prepareAbsoluteOutputDirPath("target", "test-project", "testDir", "bar").getPath());
        assertEquals("/home/redhat/jkube",EnvUtil.prepareAbsoluteOutputDirPath("target", "test-project", "testDir", "/home/redhat/jkube").getPath());
    }

    @Test
    public void testPrepareAbsolutePathWindows() {
        assumeTrue(isWindows());
        assertEquals("test-project\\target\\testDir\\bar", EnvUtil.prepareAbsoluteOutputDirPath("target", "test-project", "testDir", "bar").getPath());
        assertEquals("C:\\users\\redhat\\jkube", EnvUtil.prepareAbsoluteOutputDirPath("target", "test-project", "testDir", "C:\\users\\redhat\\jkube").getPath());
    }

    @Test
    public void testPrepareAbsoluteSourceDirPath() {
        assumeFalse(isWindows());
        assertEquals("test-project/target/testDir",EnvUtil.prepareAbsoluteSourceDirPath("target", "test-project", "testDir").getPath());
        assertEquals("/home/redhat/jkube",EnvUtil.prepareAbsoluteSourceDirPath("target", "test-project", "/home/redhat/jkube").getPath());
    }

    @Test
    public void testPrepareAbsoluteSourceDirPathWindows() {
        assumeTrue(isWindows());
        assertEquals("test-project\\target\\testDir", EnvUtil.prepareAbsoluteSourceDirPath("target", "test-project", "testDir").getPath());
        assertEquals("C:\\users\\redhat\\jkube", EnvUtil.prepareAbsoluteSourceDirPath("target", "test-project", "C:\\users\\redhat\\jkube").getPath());
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
        assertEquals("element1,element2",result);
    }

    @Test
    public void testExtractMavenPropertyName() {
        assertEquals("project.baseDir", EnvUtil.extractMavenPropertyName("${project.baseDir}"));
        assertNull(EnvUtil.extractMavenPropertyName("roject.notbaseDi"));
    }

    @Test
    public void testFixupPathWhenNotWindows(){
        //Given
        String test2 = "/etc/ip/";
        //When
        String result2 = EnvUtil.fixupPath(test2);
        //Then
        assertEquals("/etc/ip/",result2);
    }

    @Test
    public void testFixupPathWhenWindows(){
        //Given
        String test1 = "c:\\...\\";
        //When
        String result1 = EnvUtil.fixupPath(test1);
        //Then
        assertEquals("/c/.../",result1);
    }

    @Test
    public void testEnsureRegistryHttpUrlIsTrue(){
        //Given
        String url1 = "http://registor";
        //When
        String result1 = EnvUtil.ensureRegistryHttpUrl(url1);
        //Then
        assertEquals("http://registor",result1);
    }

    @Test
    public void testEnsureRegistryHttpUrlIsNotHttp(){
        //Given
        String url2 = "registerurl";
        //When
        String result2 = EnvUtil.ensureRegistryHttpUrl(url2);
        //Then
        assertEquals("https://registerurl",result2);
    }

    @Test
    public void testStoreTimestamp(
            @Mocked Files files, @Mocked File fileToStoreTimestamp, @Mocked File dir) throws IOException {
        // Given
        new Expectations() {{
            fileToStoreTimestamp.exists() ;
            result = false;
            fileToStoreTimestamp.getParentFile();
            result = dir;
            dir.exists();
            result = true;
        }};
        final Date date = new Date(1445385600000L);
        // When
        storeTimestamp(fileToStoreTimestamp, date);
        // Then
        new Verifications() {{
            files.write(withInstanceOf(Path.class), "1445385600000".getBytes(StandardCharsets.US_ASCII));
        }};
    }

    @Test
    public void testLoadTimestampShouldLoadFromFile() throws Exception {
        // Given
        final File file = new File(EnvUtilTest.class.getResource("/util/loadTimestamp.timestamp").getFile());
        // When
        final Date timestamp = loadTimestamp(file);
        // Then
        assertThat(timestamp, equalTo(new Date(1445385600000L)));
    }

    @Test
    public  void testIsWindowsFalse(){
        //Given
        new SystemMock();
        SystemMock.FAKE_PROPS.put("os.name", "random");
        //When
        boolean result= EnvUtil.isWindows();
        //Then
        assertFalse(result);
    }

    @Test
    public  void testIsWindows(){
        //Given
        new SystemMock();
        SystemMock.FAKE_PROPS.put("os.name", "windows");
        //When
        boolean result= EnvUtil.isWindows();
        //Then
        assertTrue(result);
    }

    private static final class SystemMock extends MockUp<System> {
        private static Map<String, String> FAKE_PROPS = new HashMap<>();

        @Mock
        public static String getProperty(Invocation invocation, String key) {
            return FAKE_PROPS.getOrDefault(key, invocation.proceed(key));
        }

    }
}
