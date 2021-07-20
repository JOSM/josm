// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link PropertiesCellRenderer} class.
 */
@BasicPreferences
class PropertiesCellRendererTest {
    /**
     * Test of color rendering.
     */
    @Test
    void testColorRendering() {
        PropertiesCellRenderer renderer = new PropertiesCellRenderer();
        DefaultTableModel tableModel = new DefaultTableModel(new Object[][]{
                new Object[]{"colour", "red"},
                new Object[]{"colour", "green"},
                new Object[]{"colour", "#123"},
                new Object[]{"colour", "#123456"},
                new Object[]{"colour", "foobarbaz"},
                new Object[]{"elevation", "314159"},
        }, new Object[]{"key", "value"});
        JTable table = new JTable(tableModel);
        IntFunction<String> getLabel = row -> ((JLabel) renderer.getTableCellRendererComponent(
                table, table.getValueAt(row, 1), false, false, row, 1)).getText();
        assertEquals("<html><body><span color='#FF0000'>\u25A0</span> red</body></html>", getLabel.apply(0));
        assertEquals("<html><body><span color='#008000'>\u25A0</span> green</body></html>", getLabel.apply(1));
        assertEquals("<html><body><span color='#123'>\u25A0</span> #123</body></html>", getLabel.apply(2));
        assertEquals("<html><body><span color='#123456'>\u25A0</span> #123456</body></html>", getLabel.apply(3));
        assertEquals("foobarbaz", getLabel.apply(4));
        assertEquals("314159", getLabel.apply(5));
    }

}
