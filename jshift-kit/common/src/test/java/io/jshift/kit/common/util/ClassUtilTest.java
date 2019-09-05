package io.jshift.kit.common.util;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import static io.jshift.kit.common.util.FileUtil.getAbsolutePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author roland
 * @since 01/08/16
 */

public class ClassUtilTest {

    @Test
    public void findOne() throws IOException {
        File root = getRelativePackagePath("mainclass/one");
        List<String> ret = ClassUtil.findMainClasses(root);
        assertEquals(1,ret.size());
        assertEquals("sub.OneMain", ret.get(0));
    }

    @Test
    public void findTwo() throws IOException {
        File root = getRelativePackagePath("mainclass/two");
        Set<String> ret = new HashSet<>(ClassUtil.findMainClasses(root));
        assertEquals(2,ret.size());
        assertTrue(ret.contains("OneMain"));
        assertTrue(ret.contains("another.sub.a.bit.deeper.TwoMain"));
    }

    @Test
    public void findNone() throws IOException {
        File root = getRelativePackagePath("mainclass/zero");
        List<String> ret = ClassUtil.findMainClasses(root);
        assertEquals(0,ret.size());
    }

    private File getRelativePackagePath(String subpath) {
    	File parent =        		
            new File(getAbsolutePath(this.getClass().getProtectionDomain().getCodeSource().getLocation()));
        String intermediatePath = getClass().getPackage().getName().replace(".","/");
        return new File(new File(parent, intermediatePath),subpath);
    }
}
