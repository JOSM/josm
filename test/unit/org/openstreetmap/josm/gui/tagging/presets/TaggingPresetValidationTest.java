// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import javax.swing.JLabel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.I18n;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Unit tests for {@code TaggingPresetValidation}
 */
@BasicPreferences
@I18n
@Projection
class TaggingPresetValidationTest {
    @BeforeEach
    void setUp() {
        OsmValidator.initialize();
    }

    /**
     * Tests {@link TaggingPresetValidation#validate}
     */
    @Test
    void testValidate() {
        JLabel label = new JLabel();
        OsmPrimitive primitive = OsmUtils.createPrimitive("way incline=10m width=1mm opening_hours=\"Mo-Fr 8-10\"");
        new DataSet(primitive);

        TaggingPresetValidation.validate(primitive, label);

        // CHECKSTYLE.OFF: LineLength
        assertTrue(label.isVisible());
        assertEquals("<html><ul>" +
            "<li>Opening hours syntax (Hours without minutes)</li>" +
            "<li>unusual value of width: meters is default; only positive values; point is decimal separator; if units, put space then unit</li>" +
            "<li>unusual value of incline, use x% or x° or up or down instead</li>" +
            "<li>suspicious tag combination (width on suspicious object)</li>" +
            "<li>suspicious tag combination (incline on suspicious object)</li></ul>", label.getToolTipText());
        // CHECKSTYLE.ON: LineLength
    }

    /**
     * Tests {@link TaggingPresetValidation#applyChangedTags}
     */
    @Test
    void testApplyChangedTags() {
        OsmPrimitive primitive = OsmUtils.createPrimitive("way incline=10m width=1mm opening_hours=\"Mo-Fr 8-10\"");
        new DataSet(primitive);
        OsmPrimitive clone = TaggingPresetValidation.applyChangedTags(primitive, Arrays.asList(new Tag("incline", "20m")));
        assertEquals("20m", clone.get("incline"));
        assertEquals("1mm", clone.get("width"));
    }
}
