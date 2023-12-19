// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
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
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.MapPaintStyles;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;

/**
 * Unit test of {@link CreateMultipolygonAction}
 */
@BasicPreferences
@Main
@MapPaintStyles
@Projection
class CreateMultipolygonActionTest {
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
    void testCreate1() throws Exception {
        DataSet ds = OsmReader.parseDataSet(Files.newInputStream(Paths.get(TestUtils.getTestDataRoot(), "create_multipolygon.osm")), null);
        Pair<SequenceCommand, Relation> mp = CreateMultipolygonAction.createMultipolygonCommand(ds.getWays(), null);
        assertEquals("Sequence: Create multipolygon", mp.a.getDescriptionText());
        assertEquals("{1=outer, 1.1=inner, 1.1.1=outer, 1.1.2=outer, 1.2=inner}", getRefToRoleMap(mp.b).toString());
    }

    @Test
    void testCreate2() throws Exception {
        DataSet ds = OsmReader.parseDataSet(Files.newInputStream(Paths.get(TestUtils.getTestDataRoot(), "create_multipolygon.osm")), null);
        Relation mp = createMultipolygon(ds.getWays(), "ref=1 OR ref:1.1.", null, true);
        assertEquals("{1=outer, 1.1.1=inner, 1.1.2=inner}", getRefToRoleMap(mp).toString());
    }

    @Test
    void testUpdate1() throws Exception {
        DataSet ds = OsmReader.parseDataSet(Files.newInputStream(Paths.get(TestUtils.getTestDataRoot(), "create_multipolygon.osm")), null);
        Relation mp = createMultipolygon(ds.getWays(), "ref=\".*1$\"", null, true);
        assertEquals(3, mp.getMembersCount());
        assertEquals("{1=outer, 1.1=inner, 1.1.1=outer}", getRefToRoleMap(mp).toString());
        Relation mp2 = createMultipolygon(ds.getWays(), "ref=1.2", mp, true);
        assertEquals(4, mp2.getMembersCount());
        assertEquals("{1=outer, 1.1=inner, 1.1.1=outer, 1.2=inner}", getRefToRoleMap(mp2).toString());
    }

    @Test
    void testUpdate2() throws Exception {
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
    void testTicket17767() throws Exception {
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
    void testTicket17768() throws Exception {
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

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/20110">Bug #20110</a>.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket20110() throws Exception {
        DataSet ds = OsmReader.parseDataSet(TestUtils.getRegressionDataStream(20110, "data.osm"), null);
        assertEquals(1, ds.getRelations().size());
        Relation mp = ds.getRelations().iterator().next();
        assertEquals("wetland", mp.get("natural"));
        long numCoastlineWays = ds.getWays().stream().filter(w -> "coastline".equals(w.get("natural"))).count();
        Relation modMp = createMultipolygon(ds.getWays(), "type:way", mp, false);
        assertNotNull(modMp);
        assertEquals("wetland", modMp.get("natural"));
        assertEquals(numCoastlineWays, ds.getWays().stream().filter(w -> "coastline".equals(w.get("natural"))).count());
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/20230">Bug #20230</a>.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket20230() throws Exception {
        DataSet ds = OsmReader.parseDataSet(TestUtils.getRegressionDataStream(20230, "data.osm"), null);
        assertEquals(1, ds.getRelations().size());
        Relation mp = ds.getRelations().iterator().next();
        Relation modMp = createMultipolygon(ds.getWays(), "type:way", mp, true);
        assertNotNull(modMp);
        assertEquals(1, ds.getRelations().size());
        modMp = ds.getRelations().iterator().next();
        assertTrue(modMp.hasTag("building", "yes"));
        assertEquals(0, ds.getWays().stream().filter(w -> w.hasTag("building", "yes")).count());
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/20238">Bug #20238</a>.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket20238() throws Exception {
        DataSet ds = OsmReader.parseDataSet(TestUtils.getRegressionDataStream(20238, "data.osm"), null);
        assertEquals(1, ds.getRelations().size());
        Relation mp = ds.getRelations().iterator().next();
        assertFalse(ds.getRelations().iterator().next().hasTag("building", "yes"));
        assertEquals(1, ds.getWays().stream().filter(w -> w.hasTag("building", "yes")).count());
        Pair<SequenceCommand, Relation> cmd = CreateMultipolygonAction.createMultipolygonCommand(ds.getWays(), mp);
        assertNotNull(cmd);
        cmd.a.executeCommand();
        assertEquals(1, ds.getRelations().size());
        assertTrue(ds.getRelations().iterator().next().hasTag("building", "yes"));
        assertEquals(0, ds.getWays().stream().filter(w -> w.hasTag("building", "yes")).count());
        cmd.a.undoCommand();
        assertEquals(1, ds.getRelations().size());
        assertFalse(ds.getRelations().iterator().next().hasTag("building", "yes"));
        assertEquals(1, ds.getWays().stream().filter(w -> w.hasTag("building", "yes")).count());
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/20325">Bug #20325</a>.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket20325() throws Exception {
        DataSet ds = OsmReader.parseDataSet(TestUtils.getRegressionDataStream(20325, "data.osm"), null);
        assertEquals(1, ds.getRelations().size());
        Relation mp = ds.getRelations().iterator().next();
        assertFalse(ds.getRelations().iterator().next().hasTag("landuse", "farmland"));
        assertEquals(1, ds.getWays().stream().filter(w -> w.hasTag("landuse", "farmland")).count());
        Pair<SequenceCommand, Relation> cmd = CreateMultipolygonAction.createMultipolygonCommand(ds.getWays(), mp);
        assertNotNull(cmd);
        cmd.a.executeCommand();
        assertEquals(1, ds.getRelations().size());
        assertTrue(ds.getRelations().iterator().next().hasTag("landuse", "farmland"));
        assertEquals(0, ds.getWays().stream().filter(w -> w.hasTag("landuse", "farmland")).count());
        cmd.a.undoCommand();
        assertEquals(1, ds.getRelations().size());
        assertFalse(ds.getRelations().iterator().next().hasTag("landuse", "farmland"));
        assertEquals(1, ds.getWays().stream().filter(w -> w.hasTag("landuse", "farmland")).count());
    }

    /**
     * Coverage test for <a href="https://josm.openstreetmap.de/ticket/20325">Bug #20325</a>.
     * New relation, no update needed, no command should be produced.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket20325New() throws Exception {
        DataSet ds = OsmReader.parseDataSet(TestUtils.getRegressionDataStream(20325, "no-change-new.osm"), null);
        assertEquals(1, ds.getRelations().size());
        Relation mp = ds.getRelations().iterator().next();
        Pair<SequenceCommand, Relation> cmd = CreateMultipolygonAction.createMultipolygonCommand(ds.getWays(), mp);
        assertNull(cmd);
    }

    /**
     * Coverage test for <a href="https://josm.openstreetmap.de/ticket/20325">Bug #20325</a>.
     * Old relation, no update needed, no command should be produced.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket20325Old() throws Exception {
        DataSet ds = OsmReader.parseDataSet(TestUtils.getRegressionDataStream(20325, "no-change-old.osm"), null);
        assertEquals(1, ds.getRelations().size());
        Relation mp = ds.getRelations().iterator().next();
        Pair<SequenceCommand, Relation> cmd = CreateMultipolygonAction.createMultipolygonCommand(ds.getWays(), mp);
        assertNull(cmd);
    }

    /**
     * Coverage test for <a href="https://josm.openstreetmap.de/ticket/20325">Bug #20325</a>.
     * Relation cannot be updated but produces warnings. Doesn't test that a popup was shown.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket20325Invalid() throws Exception {
        DataSet ds = OsmReader.parseDataSet(TestUtils.getRegressionDataStream(20325, "invalid-new-upldate.osm"), null);
        assertEquals(1, ds.getRelations().size());
        Relation mp = ds.getRelations().iterator().next();
        Pair<SequenceCommand, Relation> cmd = CreateMultipolygonAction.createMultipolygonCommand(ds.getWays(), mp);
        assertNull(cmd);
    }

    /**
     * Coverage test for <a href="https://josm.openstreetmap.de/ticket/20325">Bug #20325</a>.
     * Relation needs no updates but produces warnings. Doesn't test that a popup was shown.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket20325NoUpdateWarning() throws Exception {
        DataSet ds = OsmReader.parseDataSet(TestUtils.getRegressionDataStream(20325, "update-no-command-warning.osm"), null);
        assertEquals(1, ds.getRelations().size());
        Relation mp = ds.getRelations().iterator().next();
        Pair<SequenceCommand, Relation> cmd = CreateMultipolygonAction.createMultipolygonCommand(ds.getWays(), mp);
        assertNull(cmd);
    }

}
