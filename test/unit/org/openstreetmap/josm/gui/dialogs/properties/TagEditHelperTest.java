// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionListItem;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link TagEditHelper} class.
 */
public class TagEditHelperTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    private static TagEditHelper newTagEditHelper() {
        DefaultTableModel propertyData = new DefaultTableModel();
        JTable tagTable = new JTable(propertyData);
        Map<String, Map<String, Integer>> valueCount = new HashMap<>();
        return new TagEditHelper(tagTable, propertyData, valueCount);
    }

    /**
     * Checks that autocompleting list items are sorted correctly.
     */
    @Test
    public void testAcItemComparator() {
        List<AutoCompletionListItem> list = new ArrayList<>();
        list.add(new AutoCompletionListItem("Bing Sat"));
        list.add(new AutoCompletionListItem("survey"));
        list.add(new AutoCompletionListItem("Bing"));
        list.add(new AutoCompletionListItem("digitalglobe"));
        list.add(new AutoCompletionListItem("bing"));
        list.add(new AutoCompletionListItem("DigitalGlobe"));
        list.sort(TagEditHelper.DEFAULT_AC_ITEM_COMPARATOR);
        assertEquals(Arrays.asList("Bing", "bing", "Bing Sat", "digitalglobe", "DigitalGlobe", "survey"),
                list.stream().map(AutoCompletionListItem::getValue).collect(Collectors.toList()));
    }

    /**
     * Unit test of {@link TagEditHelper#containsDataKey}.
     */
    @Test
    public void testContainsDataKey() {
        assertFalse(newTagEditHelper().containsDataKey("foo"));
        // TODO: complete test
    }
}
