// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItemGuiSupport;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItemTest;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetType;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.testutils.annotations.Main;

/**
 * Unit tests of {@link Text} class.
 */
@Main
class TextTest implements TaggingPresetItemTest {
    @Override
    public Text getInstance() {
        return new Text();
    }

    /**
     * Unit test for {@link Check#addToPanel}.
     */
    @Override
    @Test
    public void testAddToPanel() {
        JPanel p = new JPanel();
        assertEquals(0, p.getComponentCount());
        assertTrue(getInstance().addToPanel(p, TaggingPresetItemGuiSupport.create(false)));
        assertTrue(p.getComponentCount() > 0);
    }

    @org.openstreetmap.josm.testutils.annotations.TaggingPresets
    @ParameterizedTest
    @ValueSource(strings = {"\n\n\n\t\r {0}\n\n\n", "{0}"})
    void testNonRegression24023(String inscription) {
        // There is a bit of "extra" whitespace in the string (` \n`). It is somewhat deliberate. We probably ought to remove the ` ` at some time.
        final String expected = "This is a \nsample \ninscription";
        final String toTest = MessageFormat.format(inscription, expected).replace("sample ", "sample    ");
        final Collection<TaggingPreset> presets = TaggingPresets.getMatchingPresets(Collections.singleton(TaggingPresetType.NODE), Map.of("historic", "boundary_stone", "inscription", "bar"), false);
        assertEquals(1, presets.size());
        final TaggingPreset preset = presets.iterator().next();
        final Text text = assertInstanceOf(Text.class, preset.data.get(5));
        final List<Tag> changeCommands = new ArrayList<>(1);
        final JPanel panel = new JPanel();
        text.addToPanel(panel, TaggingPresetItemGuiSupport.create(false));
        JComponent value = assertInstanceOf(JComponent.class, panel.getComponent(1));
        while (value instanceof JPanel) {
            value = (JComponent) value.getComponent(0);
        }
        final JosmTextField textField = assertInstanceOf(JosmTextField.class, value, "Until we support multiline editing, this should be a text field");
        textField.setText(toTest);
        text.addCommands(changeCommands);
        assertTrue(text.multiline);
        assertTrue(text.normalize);
        assertEquals(1, changeCommands.size());
        assertEquals(expected, changeCommands.get(0).getValue(), "If the only difference is a trailing space was removed, update the test.");
    }
}
