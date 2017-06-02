// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.MoveAction.Direction;

/**
 * Unit tests for class {@link ExtensionFileFilter}.
 */
public class MoveActionTest {

    /**
     * Unit test of {@link Direction} enum.
     */
    @Test
    public void testEnumDirection() {
        TestUtils.superficialEnumCodeCoverage(Direction.class);
    }
}
