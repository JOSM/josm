// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main.InitStatusListener;
import org.openstreetmap.josm.Main.InitializationTask;
import org.openstreetmap.josm.data.coor.conversion.CoordinateFormatManager;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link Main} class.
 */
public class MainTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().platform().https().devAPI().main().projection();

    /**
     * Unit test of {@link Main#preConstructorInit}.
     */
    @Test
    public void testPreConstructorInit() {
        Main.preConstructorInit();
        assertNotNull(CoordinateFormatManager.getDefaultFormat());
    }

    private static class InitStatusListenerStub implements InitStatusListener {

        boolean updated;
        boolean finished;

        @Override
        public Object updateStatus(String event) {
            updated = true;
            return null;
        }

        @Override
        public void finish(Object status) {
            finished = true;
        }
    }

    /**
     * Unit test of {@link Main#setInitStatusListener}.
     */
    @Test
    public void testSetInitStatusListener() {
        InitStatusListenerStub listener = new InitStatusListenerStub();
        Main.setInitStatusListener(listener);
        assertFalse(listener.updated);
        assertFalse(listener.finished);
        new InitializationTask("", () -> { }).call();
        assertTrue(listener.updated);
        assertTrue(listener.finished);
    }
}
