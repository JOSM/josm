// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Font;
import java.awt.Rectangle;

import javax.swing.JTable;
import javax.swing.SwingConstants;

import org.junit.jupiter.api.Test;

class LargeTextTableTest {

    private final JTable table = new LargeTextTable(null, null);
    private final Rectangle visibleRectangle = new Rectangle(0, 0, 101, 202);

    @Test
    void testGetScrollableBlockIncrementVertical() {
        int scrollableBlockIncrement = table.getScrollableBlockIncrement(visibleRectangle, SwingConstants.VERTICAL, 0);

        assertThat(scrollableBlockIncrement, is(202));
    }

    @Test
    void testGetScrollableBlockIncrementHorizontal() {
        int scrollableBlockIncrement = table.getScrollableBlockIncrement(visibleRectangle, SwingConstants.HORIZONTAL, 0);

        assertThat(scrollableBlockIncrement, is(101));
    }

    @Test
    void testGetScrollableBlockIncrementWithInvalidOrientation() {
        assertThrows(IllegalArgumentException.class,
                () -> table.getScrollableBlockIncrement(visibleRectangle, -11, 0));
    }

    @Test
    void testGetScrollableUnitIncrementVertical() {
        Font font = new Font("", Font.PLAIN, 10);
        table.setFont(font);

        int actualIncrement = table.getScrollableUnitIncrement(visibleRectangle, SwingConstants.VERTICAL, 0);
        int expectedIncrement = table.getFontMetrics(font).getHeight();

        assertThat(actualIncrement, is(expectedIncrement));
    }

    @Test
    void testGetScrollableUnitIncrementHorizontal() {
        Font font = new Font("", Font.PLAIN, 10);
        table.setFont(font);

        int actualIncrement = table.getScrollableUnitIncrement(visibleRectangle, SwingConstants.HORIZONTAL, 0);
        int expectedIncrement = table.getFontMetrics(font).charWidth('m');

        assertThat(actualIncrement, is(expectedIncrement));
    }

    @Test
    void testGetScrollableUnitIncrementWithInvalidOrientation() {
        assertThrows(IllegalArgumentException.class, () -> table.getScrollableUnitIncrement(visibleRectangle, -11, 0));
    }
}
