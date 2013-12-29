// License: GPL. For details, see LICENSE file.
package org.openstreetmap;

import org.junit.Ignore;

@Ignore
public class TestUtils {
    private TestUtils() {
    }

    /**
     * Returns the path to test data root directory.
     */
    public static String getTestDataRoot() {
        String testDataRoot = System.getProperty("josm.test.data");
        if (testDataRoot == null || testDataRoot.isEmpty()) {
            testDataRoot = "test/data";
            System.out.println("System property josm.test.data is not set, using '" + testDataRoot + "'");
        }
        return testDataRoot.endsWith("/") ? testDataRoot : testDataRoot + "/";
    }
}
