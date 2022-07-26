// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.HTTP;
import org.openstreetmap.josm.testutils.annotations.HTTPS;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.OsmApiType;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Unit tests of {@link Lifecycle} class.
 */
@HTTP
@HTTPS
@Main
@OsmApiType(OsmApiType.APIType.DEV)
@Projection
class LifecycleTest {
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
     * Unit test of {@link Lifecycle#setInitStatusListener}.
     */
    @Test
    void testSetInitStatusListener() {
        InitStatusListenerStub listener = new InitStatusListenerStub();
        Lifecycle.setInitStatusListener(listener);
        assertFalse(listener.updated);
        assertFalse(listener.finished);
        new InitializationTask("", () -> { }).call();
        assertTrue(listener.updated);
        assertTrue(listener.finished);
    }
}
