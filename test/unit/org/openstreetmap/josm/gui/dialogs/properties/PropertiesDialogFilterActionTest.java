// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JTable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Filter;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.search.SearchSetting;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.dialogs.FilterDialog;
import org.openstreetmap.josm.gui.dialogs.FilterTableModel;
import org.openstreetmap.josm.gui.dialogs.properties.PropertiesDialog.FilterAction;
import org.openstreetmap.josm.gui.dialogs.properties.PropertiesDialog.ReadOnlyTableModel;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests of {@link PropertiesDialog.FilterAction} class
 */
public class PropertiesDialogFilterActionTest {

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().main().projection().preferences();

    @Test
    void createFilterForKeysTest() {
        List<OsmPrimitive> prims = new ArrayList<>();
        Node n = new Node();
        n.put("foo", "bar");
        Way w = new Way();
        w.put("foo", "baz");
        w.put("blah", "blub");
        prims.add(n);
        prims.add(w);

        Filter filter = PropertiesDialog.createFilterForKeys(Arrays.asList("foo", "blah"), prims);
        SearchSetting expectedSearchSetting = new SearchSetting();
        expectedSearchSetting.text = "((\"foo\"=\"bar\") OR (\"foo\"=\"baz\")) AND (\"blah\"=\"blub\")";
        expectedSearchSetting.caseSensitive = true;
        Filter expected = new Filter(expectedSearchSetting);
        assertEquals(0, filter.compareTo(expected));
    }

    @Test
    void filterActionHideTest() {
        DataSet ds = new DataSet();
        OsmDataLayer layer = new OsmDataLayer(ds, "", null);
        MainApplication.getLayerManager().addLayer(layer);
        MapFrame mf = MainApplication.getMap();
        PropertiesDialog propertiesDialog = mf.propertiesDialog;
        FilterDialog filterDialog = mf.filterDialog;
        FilterTableModel filterModel = filterDialog.getFilterModel();

        Node n = new Node(new EastNorth(0, 0));
        n.put("natural", "tree");
        n.put("leaf_type", "broadleaved");
        ds.addPrimitive(n);

        Node n2 = new Node(new EastNorth(0, 0));
        n2.put("natural", "tree");
        n2.put("leaf_type", "needleleaved");
        ds.addPrimitive(n2);

        Node n3 = new Node(new EastNorth(0, 0));
        n3.put("natural", "tree");
        ds.addPrimitive(n3);

        try {
            Field filterActionHideField = PropertiesDialog.class.getDeclaredField("filterActionHide");
            filterActionHideField.setAccessible(true);
            FilterAction filterActionHide = (PropertiesDialog.FilterAction) filterActionHideField.get(propertiesDialog);

            Field tagTableField = PropertiesDialog.class.getDeclaredField("tagTable");
            tagTableField.setAccessible(true);
            JTable tagTable = (JTable) tagTableField.get(propertiesDialog);

            Field tagDataField = PropertiesDialog.class.getDeclaredField("tagData");
            tagDataField.setAccessible(true);
            ReadOnlyTableModel tagData = (ReadOnlyTableModel) tagDataField.get(propertiesDialog);

            assertEquals(0, tagData.getRowCount());
            ds.setSelected(n, n2);
            assertEquals(2, tagData.getRowCount());

            tagTable.selectAll();

            assertEquals(0, filterModel.getFilters().size());
            filterActionHide.actionPerformed(null);
            assertEquals(1, filterModel.getFilters().size());

            SearchSetting expectedSearchSetting = new SearchSetting();
            expectedSearchSetting.text = "((\"leaf_type\"=\"broadleaved\") OR (\"leaf_type\"=\"needleleaved\")) AND (\"natural\"=\"tree\")";
            expectedSearchSetting.caseSensitive = true;
            Filter expectedFilter = new Filter(expectedSearchSetting);

            Filter createdFilter = filterModel.getFilters().get(0);

            assertEquals(expectedFilter, createdFilter);
            assertEquals(0, createdFilter.compareTo(expectedFilter));
            assertEquals(true, createdFilter.enable);

            assertEquals(true, n.isDisabled());
            assertEquals(true, n2.isDisabled());
            assertEquals(false, n3.isDisabled());
        } catch (Exception e) {
            fail("Should not throw", e);
        }
    }

    @Test
    void filterActionShowOnlyTest() {
        DataSet ds = new DataSet();
        OsmDataLayer layer = new OsmDataLayer(ds, "", null);
        MainApplication.getLayerManager().addLayer(layer);
        MapFrame mf = MainApplication.getMap();
        PropertiesDialog propertiesDialog = mf.propertiesDialog;
        FilterDialog filterDialog = mf.filterDialog;
        FilterTableModel filterModel = filterDialog.getFilterModel();

        Node n = new Node(new EastNorth(0, 0));
        n.put("natural", "tree");
        n.put("leaf_type", "broadleaved");
        ds.addPrimitive(n);

        Node n2 = new Node(new EastNorth(0, 0));
        n2.put("natural", "tree");
        n2.put("leaf_type", "needleleaved");
        ds.addPrimitive(n2);

        Node n3 = new Node(new EastNorth(0, 0));
        n3.put("natural", "tree");
        ds.addPrimitive(n3);

        try {
            Field filterActionShowOnlyField = PropertiesDialog.class.getDeclaredField("filterActionShowOnly");
            filterActionShowOnlyField.setAccessible(true);
            FilterAction filterActionShowOnly = (PropertiesDialog.FilterAction) filterActionShowOnlyField.get(propertiesDialog);

            Field tagTableField = PropertiesDialog.class.getDeclaredField("tagTable");
            tagTableField.setAccessible(true);
            JTable tagTable = (JTable) tagTableField.get(propertiesDialog);

            Field tagDataField = PropertiesDialog.class.getDeclaredField("tagData");
            tagDataField.setAccessible(true);
            ReadOnlyTableModel tagData = (ReadOnlyTableModel) tagDataField.get(propertiesDialog);

            assertEquals(0, tagData.getRowCount());
            ds.setSelected(n, n2);
            assertEquals(2, tagData.getRowCount());

            tagTable.selectAll();

            assertEquals(0, filterModel.getFilters().size());
            filterActionShowOnly.actionPerformed(null);
            assertEquals(1, filterModel.getFilters().size());

            SearchSetting expectedSearchSetting = new SearchSetting();
            expectedSearchSetting.text = "((\"leaf_type\"=\"broadleaved\") OR (\"leaf_type\"=\"needleleaved\")) AND (\"natural\"=\"tree\")";
            expectedSearchSetting.caseSensitive = true;
            Filter expectedFilter = new Filter(expectedSearchSetting);
            expectedFilter.inverted = true;

            Filter createdFilter = filterModel.getFilters().get(0);

            assertEquals(expectedFilter, createdFilter);
            assertEquals(0, createdFilter.compareTo(expectedFilter));
            assertEquals(true, createdFilter.enable);

            assertEquals(false, n.isDisabled());
            assertEquals(false, n2.isDisabled());
            assertEquals(true, n3.isDisabled());
        } catch (Exception e) {
            fail("Should not throw", e);
        }
    }
}
