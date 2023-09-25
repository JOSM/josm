// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmDataManager;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.tagging.ac.AutoCompletionItem;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.properties.TagEditHelper.AddTagsDialog;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.testutils.annotations.Territories;
import org.openstreetmap.josm.testutils.mockers.WindowMocker;
import org.openstreetmap.josm.tools.JosmRuntimeException;

import mockit.Mock;
import mockit.MockUp;

/**
 * Unit tests of {@link TagEditHelper} class.
 */
@Projection
@Territories
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

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/23191>#23191</a>
     */
    @Test
    void testTicket23191() {
        TestUtils.assumeWorkingJMockit();
        new WindowMocker();
        final TagEditHelper tagEditHelper = newTagEditHelper();
        final DataSet original = new DataSet();
        MainApplication.getLayerManager().addLayer(new OsmDataLayer(original, "TagEditHelperTest.testTicket23191_1", null));
        final Node toSelect = TestUtils.newNode("");
        original.addPrimitive(toSelect);
        original.setSelected(toSelect);
        assertEquals(1, OsmDataManager.getInstance().getInProgressISelection().size());
        assertTrue(OsmDataManager.getInstance().getInProgressISelection().contains(toSelect));

        final AtomicBoolean canContinue = new AtomicBoolean();
        final AtomicBoolean showingDialog = new AtomicBoolean();

        // Instantiate the AddTagsDialog where we don't have to worry about race conditions
        tagEditHelper.sel = OsmDataManager.getInstance().getInProgressSelection();
        final AddTagsDialog addTagsDialog = tagEditHelper.getAddTagsDialog();
        tagEditHelper.resetSelection();
        new MockUp<TagEditHelper>() {
            @Mock
            public AddTagsDialog getAddTagsDialog() {
                return addTagsDialog;
            }
        };

        new MockUp<AddTagsDialog>() {
            @Mock
            public ExtendedDialog showDialog() {
                showingDialog.set(true);
                while (!canContinue.get()) {
                    synchronized (canContinue) {
                        try {
                            canContinue.wait();
                        } catch (InterruptedException e) {
                            throw new JosmRuntimeException(e);
                        }
                    }
                }
                return null;
            }

            @Mock
            public int getValue() {
                return 1;
            }
        };

        // Avoid showing the JOption pane
        Config.getPref().putBoolean("message.properties.selection-changed", false);
        Config.getPref().putInt("message.properties.selection-changed.value", JOptionPane.YES_OPTION);

        // "Open" the tag edit dialog -- this should technically be in the EDT, but we are mocking the UI parts out,
        // since the EDT does allow new EDT runnables when showing the add tag dialog
        Future<?> tagFuture = MainApplication.worker.submit(tagEditHelper::addTag);

        Awaitility.await().atMost(Durations.ONE_SECOND).untilTrue(showingDialog);
        // This is what remote control will effectively do
        MainApplication.getLayerManager().addLayer(new OsmDataLayer(new DataSet(), "TagEditHelperTest.testTicket23191_2", null));
        tagEditHelper.resetSelection();

        // Enter key=value
        addTagsDialog.keys.setText("building");
        addTagsDialog.values.setText("yes");

        // Close the tag edit dialog
        synchronized (canContinue) {
            canContinue.set(true);
            canContinue.notifyAll();
        }

        assertDoesNotThrow(() -> tagFuture.get());
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
