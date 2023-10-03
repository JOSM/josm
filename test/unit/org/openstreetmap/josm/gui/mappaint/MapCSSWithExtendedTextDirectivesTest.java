// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Color;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.TagKeyReference;
import org.openstreetmap.josm.gui.mappaint.styleelement.LabelCompositionStrategy.DeriveLabelFromNameTagsCompositionStrategy;
import org.openstreetmap.josm.gui.mappaint.styleelement.LabelCompositionStrategy.TagLookupCompositionStrategy;
import org.openstreetmap.josm.gui.mappaint.styleelement.TextLabel;

/**
 * Extended text directives tests.
 */
class MapCSSWithExtendedTextDirectivesTest {
    /**
     * Test {@link DeriveLabelFromNameTagsCompositionStrategy}
     */
    @Test
    void testCreateAutoTextElement() {
        MultiCascade mc = new MultiCascade();
        Cascade c = mc.getOrCreateCascade("default");
        c.put("text", new Keyword("auto"));
        Node osm = new Node();
        osm.put("ref", "A456");
        Environment env = new Environment(osm, mc, "default", null);

        TextLabel te = TextLabel.create(env, Color.WHITE, false /* no default annotate */);
        assertNotNull(te.labelCompositionStrategy);
        assertInstanceOf(DeriveLabelFromNameTagsCompositionStrategy.class, te.labelCompositionStrategy);
    }

    /**
     * Test {@link TagLookupCompositionStrategy}.
     */
    @Test
    void testCreateTextElementComposingTextFromTag() {
        MultiCascade mc = new MultiCascade();
        Cascade c = mc.getOrCreateCascade("default");
        c.put("text", new TagKeyReference("my_name"));
        Node osm = new Node();
        osm.put("my_name", "foobar");
        Environment env = new Environment(osm, mc, "default", null);

        TextLabel te = TextLabel.create(env, Color.WHITE, false /* no default annotate */);
        assertNotNull(te.labelCompositionStrategy);
        assertInstanceOf(TagLookupCompositionStrategy.class, te.labelCompositionStrategy);
        assertEquals("my_name", ((TagLookupCompositionStrategy) te.labelCompositionStrategy).getDefaultLabelTag());
    }

    /**
     * Test null strategy.
     */
    @Test
    void testCreateNullStrategy() {
        MultiCascade mc = new MultiCascade();
        Node osm = new Node();
        Environment env = new Environment(osm, mc, "default", null);

        TextLabel te = TextLabel.create(env, Color.WHITE, false /* no default annotate */);
        assertNull(te);
    }
}
