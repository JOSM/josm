// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;

import javax.swing.JPanel;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItemGuiSupport;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link Combo} class.
 */
class ComboTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().main().i18n("de");

    /**
     * Unit test for {@link Combo#addToPanel}.
     */
    @Test
    void testAddToPanel() {
        JPanel p = new JPanel();
        assertEquals(0, p.getComponentCount());
        assertTrue(new Combo().addToPanel(p, TaggingPresetItemGuiSupport.create(false)));
        assertTrue(p.getComponentCount() > 0);
    }

    /**
     * Unit test for {@link ComboMultiSelect#use_last_as_default} and {@link ComboMultiSelect#getItemToSelect}
     */
    @Test
    void testUseLastAsDefault() {
        Combo combo = new Combo();
        combo.key = "addr:country";
        combo.use_last_as_default = 1;
        combo.values_from = "java.util.Locale#getISOCountries";
        OsmPrimitive way = OsmUtils.createPrimitive("way");
        OsmPrimitive wayAT = OsmUtils.createPrimitive("way addr:country=AT");
        OsmPrimitive waySI = OsmUtils.createPrimitive("way addr:country=SI");

        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, way));
        assertEquals("", combo.getSelectedValue());

        combo.default_ = "SI";
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, way));
        assertEquals("SI", combo.getSelectedValue());
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, wayAT));
        assertEquals("AT", combo.getSelectedValue());
        combo.default_ = null;

        KeyedItem.LAST_VALUES.clear();
        KeyedItem.LAST_VALUES.put("addr:country", "AT");
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, way));
        assertEquals("AT", combo.getSelectedValue());
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(true, wayAT));
        assertEquals("AT", combo.getSelectedValue());
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(true));
        assertEquals("", combo.getSelectedValue());
        combo.use_last_as_default = 2; // "force"
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(true));
        assertEquals("AT", combo.getSelectedValue());
        KeyedItem.LAST_VALUES.clear();

        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(true, wayAT, waySI));
        assertEquals(Combo.DIFFERENT, combo.getSelectedValue());
    }

    @Test
    void testColor() {
        Combo combo = new Combo();
        combo.key = "colour";
        combo.values = "red;green;blue;black";
        combo.values_context = "color";
        combo.delimiter = ';';
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false));
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
