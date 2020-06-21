// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;

import javax.swing.JPanel;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link Combo} class.
 */
public class ComboTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().main().i18n("de");

    /**
     * Unit test for {@link Combo#addToPanel}.
     */
    @Test
    public void testAddToPanel() {
        JPanel p = new JPanel();
        assertEquals(0, p.getComponentCount());
        assertTrue(new Combo().addToPanel(p, Collections.<OsmPrimitive>emptyList(), false));
        assertTrue(p.getComponentCount() > 0);
    }

    /**
     * Unit test for {@link ComboMultiSelect#use_last_as_default} and {@link ComboMultiSelect#getItemToSelect}
     */
    @Test
    public void testUseLastAsDefault() {
        Combo combo = new Combo();
        combo.key = "addr:country";
        combo.use_last_as_default = 1;
        combo.values_from = "java.util.Locale#getISOCountries";
        OsmPrimitive way = OsmUtils.createPrimitive("way");
        OsmPrimitive wayAT = OsmUtils.createPrimitive("way addr:country=AT");
        OsmPrimitive waySI = OsmUtils.createPrimitive("way addr:country=SI");

        combo.addToPanel(new JPanel(), Collections.singleton(way), false);
        assertEquals("", combo.getSelectedValue());

        combo.default_ = "SI";
        combo.addToPanel(new JPanel(), Collections.singleton(way), false);
        assertEquals("SI", combo.getSelectedValue());
        combo.addToPanel(new JPanel(), Collections.singleton(wayAT), false);
        assertEquals("AT", combo.getSelectedValue());
        combo.default_ = null;

        KeyedItem.LAST_VALUES.clear();
        KeyedItem.LAST_VALUES.put("addr:country", "AT");
        combo.addToPanel(new JPanel(), Collections.singleton(way), false);
        assertEquals("AT", combo.getSelectedValue());
        combo.addToPanel(new JPanel(), Collections.singleton(wayAT), true);
        assertEquals("AT", combo.getSelectedValue());
        combo.addToPanel(new JPanel(), Collections.singleton(way), true);
        assertEquals("", combo.getSelectedValue());
        combo.use_last_as_default = 2; // "force"
        combo.addToPanel(new JPanel(), Collections.singleton(way), true);
        assertEquals("AT", combo.getSelectedValue());
        KeyedItem.LAST_VALUES.clear();

        combo.addToPanel(new JPanel(), Arrays.asList(wayAT, waySI), true);
        assertEquals(tr("<different>"), combo.getSelectedValue());
    }

    @Test
    public void testColor() {
        Combo combo = new Combo();
        combo.key = "colour";
        combo.values = "red;green;blue;black";
        combo.values_context = "color";
        combo.delimiter = ';';
        combo.addToPanel(new JPanel(), Collections.<OsmPrimitive>emptyList(), false);
        assertEquals(5, combo.combobox.getItemCount());
        combo.presetListEntries.stream().filter(e -> "red".equals(e.value)).findFirst().ifPresent(combo.combobox::setSelectedItem);
        assertEquals("red", combo.getSelectedValue());
        assertEquals("Rot", combo.getSelectedItem().toString());
        assertEquals(new Color(0xFF0000), combo.getColor());
        combo.presetListEntries.stream().filter(e -> "green".equals(e.value)).findFirst().ifPresent(combo.combobox::setSelectedItem);
        assertEquals("green", combo.getSelectedValue());
        assertEquals("Grün", combo.getSelectedItem().toString());
        assertEquals(new Color(0x008000), combo.getColor());
        combo.combobox.setSelectedItem("#135");
        assertEquals("#135", combo.getSelectedValue());
        assertEquals(new Color(0x113355), combo.getColor());
        combo.combobox.setSelectedItem("#123456");
        assertEquals("#123456", combo.getSelectedValue());
        assertEquals(new Color(0x123456), combo.getColor());
        combo.setColor(new Color(0x448822));
        assertEquals("#448822", combo.getSelectedValue());
    }
}
