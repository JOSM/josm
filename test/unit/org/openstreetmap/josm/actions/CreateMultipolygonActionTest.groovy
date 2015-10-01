package org.openstreetmap.josm.actions

import org.junit.BeforeClass
import org.junit.Test
import org.openstreetmap.josm.JOSMFixture
import org.openstreetmap.josm.TestUtils
import org.openstreetmap.josm.actions.search.SearchAction
import org.openstreetmap.josm.actions.search.SearchCompiler
import org.openstreetmap.josm.data.osm.Relation
import org.openstreetmap.josm.data.osm.Way
import org.openstreetmap.josm.io.OsmReader
import org.openstreetmap.josm.tools.Utils

class CreateMultipolygonActionTest {

    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    static def getRefToRoleMap(Relation relation) {
        def refToRole = new TreeMap<String, String>()
        for (i in relation.getMembers()) {
            refToRole.put(i.member.get("ref"), i.role);
        }
        return refToRole;
    }

    static def regexpSearch(String search) {
        def setting = new SearchAction.SearchSetting()
        setting.text = search
        setting.regexSearch = true
        return setting
    }

    @Test
    public void testCreate1() {
        def ds = OsmReader.parseDataSet(new FileInputStream(TestUtils.getTestDataRoot() + "create_multipolygon.osm"), null);
        def mp = CreateMultipolygonAction.createMultipolygonCommand(ds.getWays(), null)
        assert mp.a.getDescriptionText() == "Sequence: Create multipolygon"
        assert getRefToRoleMap(mp.b).toString() == "[1:outer, 1.1:inner, 1.1.1:outer, 1.1.2:outer, 1.2:inner]"
    }

    @Test
    public void testCreate2() {
        def ds = OsmReader.parseDataSet(new FileInputStream(TestUtils.getTestDataRoot() + "create_multipolygon.osm"), null);
        def ways = Utils.filter(ds.getWays(), SearchCompiler.compile("ref=1 OR ref:1.1."))
        def mp = CreateMultipolygonAction.createMultipolygonCommand(ways as Collection<Way>, null)
        assert getRefToRoleMap(mp.b).toString() == "[1:outer, 1.1.1:inner, 1.1.2:inner]"
    }

    @Test
    public void testUpdate1() {
        def ds = OsmReader.parseDataSet(new FileInputStream(TestUtils.getTestDataRoot() + "create_multipolygon.osm"), null);
        def ways = Utils.filter(ds.getWays(), SearchCompiler.compile(regexpSearch("ref=\".*1\$\"")))
        def mp = CreateMultipolygonAction.createMultipolygonCommand(ways as Collection<Way>, null)
        assert mp.b.getMembersCount() == 3
        assert getRefToRoleMap(mp.b).toString() == "[1:outer, 1.1:inner, 1.1.1:outer]"
        def ways2 = Utils.filter(ds.getWays(), SearchCompiler.compile(regexpSearch("ref=1.2")))
        def mp2 = CreateMultipolygonAction.createMultipolygonCommand(ways2 as Collection<Way>, mp.b)
        assert mp2.b.getMembersCount() == 4
        assert getRefToRoleMap(mp2.b).toString() == "[1:outer, 1.1:inner, 1.1.1:outer, 1.2:inner]"
    }

    @Test
    public void testUpdate2() {
        def ds = OsmReader.parseDataSet(new FileInputStream(TestUtils.getTestDataRoot() + "create_multipolygon.osm"), null);
        def ways = Utils.filter(ds.getWays(), SearchCompiler.compile("ref=1 OR ref:1.1.1"))
        def mp = CreateMultipolygonAction.createMultipolygonCommand(ways as Collection<Way>, null)
        assert getRefToRoleMap(mp.b).toString() == "[1:outer, 1.1.1:inner]"
        def ways2 = Utils.filter(ds.getWays(), SearchCompiler.compile(regexpSearch("ref=1.1 OR ref=1.2 OR ref=1.1.2")))
        def mp2 = CreateMultipolygonAction.createMultipolygonCommand(ways2 as Collection<Way>, mp.b)
        assert getRefToRoleMap(mp2.b).toString() == "[1:outer, 1.1:inner, 1.1.1:outer, 1.1.2:outer, 1.2:inner]"
    }
}
