// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.mappaint.styleelement.LabelCompositionStrategy;
import org.openstreetmap.josm.gui.mappaint.styleelement.LabelCompositionStrategy.DeriveLabelFromNameTagsCompositionStrategy;
import org.openstreetmap.josm.gui.mappaint.styleelement.LabelCompositionStrategy.StaticLabelCompositionStrategy;
import org.openstreetmap.josm.gui.mappaint.styleelement.LabelCompositionStrategy.TagLookupCompositionStrategy;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link LabelCompositionStrategy}.
 */
@BasicPreferences
class LabelCompositionStrategyTest {
    /**
     * Test {@link StaticLabelCompositionStrategy}.
     */
    @Test
    void testCreateStaticLabelCompositionStrategy() {
        Node n = new Node();

        LabelCompositionStrategy strat = new StaticLabelCompositionStrategy(null);
        assertNull(strat.compose(n));

        strat = new StaticLabelCompositionStrategy("a label");
        assertEquals("a label", strat.compose(n));
    }

    /**
     * Test {@link TagLookupCompositionStrategy}.
     */
    @Test
    void testCreateTagLookupCompositionStrategy() {
        Node n = new Node();
        n.put("my-tag", "my-value");

        LabelCompositionStrategy strat = new TagLookupCompositionStrategy(null);
        assertNull(strat.compose(n));

        strat = new TagLookupCompositionStrategy("name");
        assertNull(strat.compose(n));

        strat = new TagLookupCompositionStrategy("my-tag");
        assertEquals("my-value", strat.compose(n));
    }

    /**
     * Test {@link DeriveLabelFromNameTagsCompositionStrategy}.
     */
    @Test
    void testCreateDeriveLabelFromNameTagsCompositionStrategy() {
        DeriveLabelFromNameTagsCompositionStrategy strat = new DeriveLabelFromNameTagsCompositionStrategy();
        strat.setNameTags(null);
        assertEquals(Collections.emptyList(), strat.getNameTags());

        strat = new DeriveLabelFromNameTagsCompositionStrategy();
        strat.setNameTags(Arrays.asList("name", "brand"));
        assertEquals(Arrays.asList("name", "brand"), strat.getNameTags());

        Node n = new Node();
        n.put("brand", "my brand");
        assertEquals("my brand", strat.compose(n));

        n = new Node();
        n.put("name", "my name");
        n.put("brand", "my brand");
        assertEquals("my name", strat.compose(n));
    }
}
