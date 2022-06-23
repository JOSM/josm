// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.GraphicsEnvironment;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmDataManager;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.tagging.ac.AutoCompletionItem;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.properties.TagEditHelper.AddTagsDialog;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.testutils.annotations.LayerEnvironment;
import org.openstreetmap.josm.testutils.annotations.MapStyles;
import org.openstreetmap.josm.testutils.mockers.WindowMocker;

/**
 * Unit tests of {@link TagEditHelper} class.
 */
@LayerEnvironment
@MapStyles
class TagEditHelperTest {
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
    void testAcItemComparator() {
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
    void testContainsDataKey() {
        assertFalse(newTagEditHelper().containsDataKey("foo"));
        // TODO: complete test
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/18764>#18764</a>
     *
     * @throws Exception if any error occurs
     */
    @Test
    void testTicket18764() throws Exception {
        testIcon("*[building] ⧉ *[highway] { text: tr(\"Building crossing highway\"); }", ds -> {
            Way way = TestUtils.newWay("", new Node(LatLon.NORTH_POLE), new Node(LatLon.SOUTH_POLE));
            way.getNodes().forEach(ds::addPrimitive);
            return way;
        }, "highway", "");
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/18798>#18798</a>
     *
     * @throws Exception if any error occurs
     */
    @Test
    void testTicket18798() throws Exception {
        testIcon("node:righthandtraffic[junction=roundabout] { text: tr(\"Roundabout node\"); }", ds -> {
            Node node = new Node(LatLon.NORTH_POLE);
            ds.addPrimitive(node);
            return node;
        }, "junction", "roundabout");
    }

    void testIcon(String cssString, Function<DataSet, OsmPrimitive> prepare, String key, String value) throws Exception {
        TestUtils.assumeWorkingJMockit();
        if (GraphicsEnvironment.isHeadless()) {
            new WindowMocker();
        }
        MapCSSStyleSource css = new MapCSSStyleSource(cssString);
        css.loadStyleSource();
        MapPaintStyles.addStyle(css);
        DataSet ds = new DataSet();
        final OsmPrimitive primitive = prepare.apply(ds);
        OsmDataManager.getInstance().setActiveDataSet(ds);
        MainApplication.getLayerManager().addLayer(new OsmDataLayer(ds, "Test Layer", null));
        TagEditHelper helper = newTagEditHelper();
        Field sel = TagEditHelper.class.getDeclaredField("sel");
        sel.set(helper, Collections.singletonList(primitive));
        AddTagsDialog addTagsDialog = helper.getAddTagsDialog();
        Method findIcon = TagEditHelper.AbstractTagsDialog.class.getDeclaredMethod("findIcon", String.class, String.class);
        findIcon.setAccessible(true);
        Object val = findIcon.invoke(addTagsDialog, key, value);
        assertNotNull(val);
    }
}
