// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmDataManager;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.tagging.ac.AutoCompletionItem;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.properties.TagEditHelper.AddTagsDialog;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
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
        List<AutoCompletionItem> list = new ArrayList<>();
        list.add(new AutoCompletionItem("Bing Sat"));
        list.add(new AutoCompletionItem("survey"));
        list.add(new AutoCompletionItem("Bing"));
        list.add(new AutoCompletionItem("digitalglobe"));
        list.add(new AutoCompletionItem("bing"));
        list.add(new AutoCompletionItem("DigitalGlobe"));
        list.sort(TagEditHelper.DEFAULT_AC_ITEM_COMPARATOR);
        assertEquals(Arrays.asList("Bing", "bing", "Bing Sat", "digitalglobe", "DigitalGlobe", "survey"),
                list.stream().map(AutoCompletionItem::getValue).collect(Collectors.toList()));
    }

    /**
     * Unit test of {@link TagEditHelper#containsDataKey}.
     */
    @Test
    public void testContainsDataKey() {
        assertFalse(newTagEditHelper().containsDataKey("foo"));
        // TODO: complete test
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/18764>#18764</a>
     *
     * @throws InvocationTargetException Check logs -- if caused by NPE, a
     *                                   regression probably occurred.
     * @throws IllegalArgumentException  Check source code
     * @throws IllegalAccessException    Check source code
     * @throws NoSuchFieldException      Check source code
     * @throws SecurityException         Probably shouldn't happen for tests
     * @throws NoSuchMethodException     Check source code
     */
    @Test
    public void testTicket18764() throws NoSuchMethodException, SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
        MapCSSStyleSource css = new MapCSSStyleSource(
                "*[building] ⧉ *[highway] { text: tr(\"Building crossing highway\"); }");
        css.loadStyleSource();
        MapPaintStyles.addStyle(css);
        DataSet ds = new DataSet();
        // This does require a way
        Way way = TestUtils.newWay("", new Node(LatLon.NORTH_POLE), new Node(LatLon.SOUTH_POLE));
        way.getNodes().forEach(ds::addPrimitive);
        ds.addPrimitive(way);
        OsmDataManager.getInstance().setActiveDataSet(ds);
        MainApplication.getLayerManager().addLayer(new OsmDataLayer(ds, "Test Layer", null));
        TagEditHelper helper = newTagEditHelper();
        Field sel = TagEditHelper.class.getDeclaredField("sel");
        sel.set(helper, Collections.singletonList(way));
        AddTagsDialog addTagsDialog = helper.getAddTagsDialog();
        Method findIcon = TagEditHelper.AbstractTagsDialog.class.getDeclaredMethod("findIcon", String.class,
                String.class);
        findIcon.setAccessible(true);
        Object val = findIcon.invoke(addTagsDialog, "highway", "");
        assertNotNull(val);
    }
}
