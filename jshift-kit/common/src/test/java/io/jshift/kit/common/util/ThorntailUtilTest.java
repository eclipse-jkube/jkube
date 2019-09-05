package io.jshift.kit.common.util;

import java.util.Properties;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ThorntailUtilTest {

    @Test
    public void testReadThorntailPort() {
        Properties props = YamlUtil.getPropertiesFromYamlResource(SpringBootUtilTest.class.getResource("/util/project-default.yml"));
        assertNotNull(props);
        assertEquals("8082", props.getProperty("thorntail.http.port"));

    }

}
