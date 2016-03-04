// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collections;

import javax.swing.JTable;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.TagCollection;

/**
 * Unit tests of {@link MultiValueCellRenderer} class.
 */
public class MultiValueCellRendererTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link MultiValueCellRenderer#MultiValueCellRenderer}.
     */
    @Test
    public void testMultiValueCellRenderer() {
        TagConflictResolverModel model = new TagConflictResolverModel();
        TagCollection tags = new TagCollection(Arrays.asList(new Tag("oneway", "yes"), new Tag("oneway", "no")));
        model.populate(tags, Collections.singleton("oneway"));
        JTable table = new JTable(model);
        MultiValueResolutionDecision decision = new MultiValueResolutionDecision(tags);
        MultiValueCellRenderer r = new MultiValueCellRenderer();
        test(table, decision, r);
        decision.keepAll();
        test(table, decision, r);
        decision.keepNone();
        test(table, decision, r);
        decision.keepOne("yes");
        test(table, decision, r);
        decision.sumAllNumeric();
        test(table, decision, r);
        decision.undecide();
        test(table, decision, r);
    }

    private void test(JTable table, MultiValueResolutionDecision value, MultiValueCellRenderer r) {
        assertEquals(r, r.getTableCellRendererComponent(table, value, false, false, 0, 0));
        assertEquals(r, r.getTableCellRendererComponent(table, value, false, false, 0, 1));
        assertNotNull(r.getTableCellRendererComponent(table, value, false, false, 0, 2));
        assertEquals(r, r.getTableCellRendererComponent(table, value, true, false, 0, 0));
        assertEquals(r, r.getTableCellRendererComponent(table, value, true, false, 0, 1));
        assertNotNull(r.getTableCellRendererComponent(table, value, true, false, 0, 2));
    }
}
