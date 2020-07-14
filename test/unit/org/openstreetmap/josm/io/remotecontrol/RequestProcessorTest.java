// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RequestProcessor}
 * @author Taylor Smock
 */
public class RequestProcessorTest {
    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/19436">#19436</a>
     */
    @Test
    public void testFeaturesDoesNotThrowNPE() {
        assertDoesNotThrow(() -> RequestProcessor.getHandlersInfoAsJSON(Arrays.asList("add_node", "/add_node", "", null)));
    }
}
