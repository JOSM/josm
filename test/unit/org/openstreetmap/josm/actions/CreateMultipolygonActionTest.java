// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.search.SearchCompiler;
import org.openstreetmap.josm.data.osm.search.SearchParseError;
import org.openstreetmap.josm.data.osm.search.SearchSetting;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit test of {@link CreateMultipolygonAction}
 */
public class CreateMultipolygonActionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection().main().preferences();

    private static Map<String, String> getRefToRoleMap(Relation relation) {
        Map<String, String> refToRole = new TreeMap<>();
        String ref = relation.get("ref");
        if (ref != null) {
            refToRole.put(ref, "outer");
        }
        for (RelationMember i : relation.getMembers()) {
            ref = i.getMember().get("ref");
            if (ref != null) {
                refToRole.put(ref, i.getRole());
            }
        }
        return refToRole;
    }

    private static SearchSetting regexpSearch(String search) {
        SearchSetting setting = new SearchSetting();
        setting.text = search;
        setting.regexSearch = true;
        return setting;
    }

    @SuppressWarnings("unchecked")
    private static Relation createMultipolygon(Collection<Way> ways, String pattern, Relation r, boolean runCmd)
            throws SearchParseError {
        Pair<SequenceCommand, Relation> cmd = CreateMultipolygonAction.createMultipolygonCommand(
            (Collection<Way>) (Collection<?>) SubclassFilteredCollection.filter(ways, SearchCompiler.compile(regexpSearch(pattern))), r);
        if (runCmd) {
            cmd.a.executeCommand();
        }
        return cmd.b;
    }

    @Test
    public void testCreate1() throws Exception {
        DataSet ds = OsmReader.parseDataSet(Files.newInputStream(Paths.get(TestUtils.getTestDataRoot(), "create_multipolygon.osm")), null);
        Pair<SequenceCommand, Relation> mp = CreateMultipolygonAction.createMultipolygonCommand(ds.getWays(), null);
        assertEquals("Sequence: Create multipolygon", mp.a.getDescriptionText());
        assertEquals("{1=outer, 1.1=inner, 1.1.1=outer, 1.1.2=outer, 1.2=inner}", getRefToRoleMap(mp.b).toString());
    }

    @Test
    public void testCreate2() throws Exception {
        DataSet ds = OsmReader.parseDataSet(Files.newInputStream(Paths.get(TestUtils.getTestDataRoot(), "create_multipolygon.osm")), null);
        Relation mp = createMultipolygon(ds.getWays(), "ref=1 OR ref:1.1.", null, true);
        assertEquals("{1=outer, 1.1.1=inner, 1.1.2=inner}", getRefToRoleMap(mp).toString());
    }

    @Test
    public void testUpdate1() throws Exception {
        DataSet ds = OsmReader.parseDataSet(Files.newInputStream(Paths.get(TestUtils.getTestDataRoot(), "create_multipolygon.osm")), null);
        Relation mp = createMultipolygon(ds.getWays(), "ref=\".*1$\"", null, true);
        assertEquals(3, mp.getMembersCount());
        assertEquals("{1=outer, 1.1=inner, 1.1.1=outer}", getRefToRoleMap(mp).toString());
        Relation mp2 = createMultipolygon(ds.getWays(), "ref=1.2", mp, true);
        assertEquals(4, mp2.getMembersCount());
        assertEquals("{1=outer, 1.1=inner, 1.1.1=outer, 1.2=inner}", getRefToRoleMap(mp2).toString());
    }

    @Test
    public void testUpdate2() throws Exception {
        DataSet ds = OsmReader.parseDataSet(Files.newInputStream(Paths.get(TestUtils.getTestDataRoot(), "create_multipolygon.osm")), null);
        Relation mp = createMultipolygon(ds.getWays(), "ref=1 OR ref:1.1.1", null, true);
        assertEquals("{1=outer, 1.1.1=inner}", getRefToRoleMap(mp).toString());
        Relation mp2 = createMultipolygon(ds.getWays(), "ref=1.1 OR ref=1.2 OR ref=1.1.2", mp, false);
        assertEquals("{1=outer, 1.1=inner, 1.1.1=outer, 1.1.2=outer, 1.2=inner}", getRefToRoleMap(mp2).toString());
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/17767">Bug #17767</a>.
     * @throws Exception if an error occurs
     */
    @Test
    public void testTicket17767() throws Exception {
        DataSet ds = OsmReader.parseDataSet(TestUtils.getRegressionDataStream(17767, "upd-mp.osm"), null);
        Layer layer = new OsmDataLayer(ds, null, null);
        MainApplication.getLayerManager().addLayer(layer);
        try {
            CreateMultipolygonAction updateAction = new CreateMultipolygonAction(true);
            CreateMultipolygonAction createAction = new CreateMultipolygonAction(false);
            assertFalse(updateAction.isEnabled());
            assertFalse(createAction.isEnabled());
            ds.setSelected(ds.getPrimitiveById(189944949L, OsmPrimitiveType.WAY));
            assertFalse(updateAction.isEnabled());
            assertTrue(createAction.isEnabled());
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/17768">Bug #17768</a>.
     * @throws Exception if an error occurs
     */
    @Test
    public void testTicket17768() throws Exception {
        DataSet ds = OsmReader.parseDataSet(TestUtils.getRegressionDataStream(17768, "dupmem.osm"), null);
        Layer layer = new OsmDataLayer(ds, null, null);
        MainApplication.getLayerManager().addLayer(layer);
        try {
            Relation old = (Relation) ds.getPrimitiveById(580092, OsmPrimitiveType.RELATION);
            assertEquals(3, old.getMembersCount());
            Relation mp = createMultipolygon(ds.getWays(), "type:way", old, true);
            assertEquals(mp.getPrimitiveId(), old.getPrimitiveId());
            assertEquals(2, mp.getMembersCount());
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }

    }
}
