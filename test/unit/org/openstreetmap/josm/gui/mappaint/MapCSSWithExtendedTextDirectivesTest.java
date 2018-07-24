// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.TagKeyReference;
import org.openstreetmap.josm.gui.mappaint.styleelement.LabelCompositionStrategy.DeriveLabelFromNameTagsCompositionStrategy;
import org.openstreetmap.josm.gui.mappaint.styleelement.LabelCompositionStrategy.TagLookupCompositionStrategy;
import org.openstreetmap.josm.gui.mappaint.styleelement.TextLabel;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Extended text directives tests.
 */
public class MapCSSWithExtendedTextDirectivesTest {

    /**
     * Setup rule
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test {@link DeriveLabelFromNameTagsCompositionStrategy}
     */
    @Test
    public void testCreateAutoTextElement() {
        MultiCascade mc = new MultiCascade();
        Cascade c = mc.getOrCreateCascade("default");
        c.put("text", new Keyword("auto"));
        Node osm = new Node();
        osm.put("ref", "A456");
        Environment env = new Environment(osm, mc, "default", null);

        TextLabel te = TextLabel.create(env, Color.WHITE, false /* no default annotate */);
        assertNotNull(te.labelCompositionStrategy);
        assertTrue(te.labelCompositionStrategy instanceof DeriveLabelFromNameTagsCompositionStrategy);
    }

    /**
     * Test {@link TagLookupCompositionStrategy}.
     */
    @Test
    public void testCreateTextElementComposingTextFromTag() {
        MultiCascade mc = new MultiCascade();
        Cascade c = mc.getOrCreateCascade("default");
        c.put("text", new TagKeyReference("my_name"));
        Node osm = new Node();
        osm.put("my_name", "foobar");
        Environment env = new Environment(osm, mc, "default", null);

        TextLabel te = TextLabel.create(env, Color.WHITE, false /* no default annotate */);
        assertNotNull(te.labelCompositionStrategy);
        assertTrue(te.labelCompositionStrategy instanceof TagLookupCompositionStrategy);
        assertEquals("my_name", ((TagLookupCompositionStrategy) te.labelCompositionStrategy).getDefaultLabelTag());
    }

    /**
     * Test null strategy.
     */
    @Test
    public void testCreateNullStrategy() {
        MultiCascade mc = new MultiCascade();
        Node osm = new Node();
        Environment env = new Environment(osm, mc, "default", null);

        TextLabel te = TextLabel.create(env, Color.WHITE, false /* no default annotate */);
        assertNull(te);
    }
}
