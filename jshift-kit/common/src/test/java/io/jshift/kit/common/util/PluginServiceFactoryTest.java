package io.jshift.kit.common.util;

import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author roland
 * @since 01/04/16
 */
public class PluginServiceFactoryTest {

    private class TestContext {}
    private PluginServiceFactory<TestContext> pluginServiceFactory;

    @Before
    public void setup() {
        pluginServiceFactory = new PluginServiceFactory<>(new TestContext());
    }

    @Test
    public void testOrder() {
        List<TestService> services =
                pluginServiceFactory.createServiceObjects("service/test-services-default", "service/test-services");
        String[] orderExpected = new String[] { "three", "two", "five", "one"};
        assertEquals(services.size(), 4);
        Iterator<TestService> it = services.iterator();
        for (String val : orderExpected) {
            assertEquals(it.next().getName(),val);
        }
    }

    @Test
    public void errorHandling() {
        try {
            pluginServiceFactory.createServiceObjects("service/error-services");
            fail();
        } catch (IllegalStateException exp) {
            assertTrue(exp.getMessage().matches(".*bla\\.blub\\.NotExist.*"));
        }
    }

    @Test(expected = ClassCastException.class)
    public void classCastException() {
        List<String> services = pluginServiceFactory.createServiceObjects("service/test-services");
        String bla = services.get(0);
    }

    interface TestService { String getName(); }
    public static class Test1 implements TestService { public Test1(TestContext ctx) { } public String getName() { return "one"; } }
    public static class Test2 implements TestService { public Test2(TestContext ctx) { } public String getName() { return "two"; } }
    public static class Test3 implements TestService { public Test3(TestContext ctx) { } public String getName() { return "three"; } }
    public static class Test4 implements TestService { public Test4(TestContext ctx) { } public String getName() { return "four"; } }
    public static class Test5 implements TestService { public Test5(TestContext ctx) { } public String getName() { return "five"; } }
}
