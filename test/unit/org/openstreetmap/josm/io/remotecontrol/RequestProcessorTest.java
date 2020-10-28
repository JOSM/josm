// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Objects;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RequestProcessor}
 * @author Taylor Smock
 */
class RequestProcessorTest {
    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/19436">#19436</a>
     */
    @Test
    void testFeaturesDoesNotThrowNPE() {
        assertTrue(RequestProcessor.getHandlersInfo(Arrays.asList("add_node", "/add_node", "", null))
                .noneMatch(Objects::isNull));
    }
}
