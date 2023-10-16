// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItemGuiSupport;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.I18n;
import org.openstreetmap.josm.testutils.annotations.Main;

/**
 * Unit tests of {@link Combo} class.
 */
@BasicPreferences
@I18n("de")
@Main
class ComboTest {
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
     * Unit test for {@link ComboMultiSelect#use_last_as_default} and {@link ComboMultiSelect#getInitialValue}
     */
    @Test
    void testUseLastAsDefault() {
        Combo combo = new Combo();
        combo.key = "addr:country";
        combo.values_from = "java.util.Locale#getISOCountries";
        OsmPrimitive way = OsmUtils.createPrimitive("way");
        OsmPrimitive wayTagged = OsmUtils.createPrimitive("way highway=residential");
        OsmPrimitive wayAT = OsmUtils.createPrimitive("way addr:country=AT");
        OsmPrimitive waySI = OsmUtils.createPrimitive("way addr:country=SI");
        KeyedItem.LAST_VALUES.clear();
        KeyedItem.LAST_VALUES.put("addr:country", "AT");
        Combo.PROP_FILL_DEFAULT.put(false);
        combo.use_last_as_default = 0;

        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, way));
        assertEquals("", combo.getSelectedItem().value);
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, wayTagged));
        assertEquals("", combo.getSelectedItem().value);
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, wayAT));
        assertEquals("AT", combo.getSelectedItem().value);
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, waySI));
        assertEquals("SI", combo.getSelectedItem().value);
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, wayAT, waySI));
        assertEquals(Combo.DIFFERENT, combo.getSelectedItem().value);

        combo.default_ = "AT";
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, way));
        assertEquals("AT", combo.getSelectedItem().value);
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, wayTagged));
        assertEquals("", combo.getSelectedItem().value);
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, wayAT));
        assertEquals("AT", combo.getSelectedItem().value);
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, waySI));
        assertEquals("SI", combo.getSelectedItem().value);
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, wayAT, waySI));
        assertEquals(Combo.DIFFERENT, combo.getSelectedItem().value);

        Combo.PROP_FILL_DEFAULT.put(true);
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, way));
        assertEquals("AT", combo.getSelectedItem().value);
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, wayTagged));
        assertEquals("AT", combo.getSelectedItem().value);
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, wayAT));
        assertEquals("AT", combo.getSelectedItem().value);
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, waySI));
        assertEquals("SI", combo.getSelectedItem().value);
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, wayAT, waySI));
        assertEquals(Combo.DIFFERENT, combo.getSelectedItem().value);
        Combo.PROP_FILL_DEFAULT.put(false);
        combo.default_ = null;

        combo.use_last_as_default = 1; // untagged objects only
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, way));
        assertEquals("AT", combo.getSelectedItem().value);
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, wayTagged));
        assertEquals("", combo.getSelectedItem().value);
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, wayAT));
        assertEquals("AT", combo.getSelectedItem().value);
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, waySI));
        assertEquals("SI", combo.getSelectedItem().value);
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, wayAT, waySI));
        assertEquals(Combo.DIFFERENT, combo.getSelectedItem().value);

        combo.use_last_as_default = 2; // "force" on tagged objects too
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, way));
        assertEquals("AT", combo.getSelectedItem().value);
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, wayTagged));
        assertEquals("AT", combo.getSelectedItem().value);
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, wayAT));
        assertEquals("AT", combo.getSelectedItem().value);
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, waySI));
        assertEquals("SI", combo.getSelectedItem().value);
        combo.addToPanel(new JPanel(), TaggingPresetItemGuiSupport.create(false, wayAT, waySI));
        assertEquals(Combo.DIFFERENT, combo.getSelectedItem().value);
        combo.use_last_as_default = 0;

        KeyedItem.LAST_VALUES.clear();
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
        assertEquals("red", combo.getSelectedItem().value);
        assertEquals("Rot", combo.getSelectedItem().toString());
        assertEquals(new Color(0xFF0000), combo.getColor());
        combo.presetListEntries.stream().filter(e -> "green".equals(e.value)).findFirst().ifPresent(combo.combobox::setSelectedItem);
        assertEquals("green", combo.getSelectedItem().value);
        assertEquals("Grün", combo.getSelectedItem().toString());
        assertEquals(new Color(0x008000), combo.getColor());
        combo.combobox.setSelectedItem("#135");
        assertEquals("#135", combo.getSelectedItem().value);
        assertEquals(new Color(0x113355), combo.getColor());
        combo.combobox.setSelectedItem("#123456");
        assertEquals("#123456", combo.getSelectedItem().value);
        assertEquals(new Color(0x123456), combo.getColor());
        combo.setColor(new Color(0x448822));
        assertEquals("#448822", combo.getSelectedItem().value);
    }
}
