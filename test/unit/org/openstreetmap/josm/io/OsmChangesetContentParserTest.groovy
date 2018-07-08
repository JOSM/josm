// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static groovy.test.GroovyAssert.shouldFail

import java.nio.charset.StandardCharsets

import org.junit.Test
import org.openstreetmap.josm.data.osm.ChangesetDataSet
import org.openstreetmap.josm.data.osm.OsmPrimitiveType
import org.openstreetmap.josm.data.osm.SimplePrimitiveId
import org.openstreetmap.josm.data.osm.ChangesetDataSet.ChangesetModificationType
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive
import org.openstreetmap.josm.data.osm.history.HistoryRelation
import org.openstreetmap.josm.data.osm.history.HistoryWay
import org.openstreetmap.josm.gui.progress.NullProgressMonitor

class OsmChangesetContentParserTest {

    @Test
    public void test_Constructor() {
        OsmChangesetContentParser parser

        // should be OK
        parser = new OsmChangesetContentParser(new ByteArrayInputStream("".bytes))

        shouldFail(IllegalArgumentException) {
            parser = new OsmChangesetContentParser((String) null)
        }

        shouldFail(IllegalArgumentException) {
            parser = new OsmChangesetContentParser((InputStream) null)
        }
    }


    @Test
    public void test_parse_arguments() {
        OsmChangesetContentParser parser

        def String doc = """
            <osmChange version="0.6" generator="OpenStreetMap server">
            </osmChange>
        """

        // should be OK
        parser = new OsmChangesetContentParser(new ByteArrayInputStream(doc.getBytes(StandardCharsets.UTF_8)))
        parser.parse null

        // should be OK
        parser = new OsmChangesetContentParser(new ByteArrayInputStream(doc.getBytes(StandardCharsets.UTF_8)))
        parser.parse NullProgressMonitor.INSTANCE

        // should be OK
        parser = new OsmChangesetContentParser(doc)
        parser.parse null
    }

    /**
     * A simple changeset content document with one created node
     *
     */
    @Test
    public void test_OK_OneCreatedNode() {
        OsmChangesetContentParser parser

        def String doc = """
            <osmChange version="0.6" generator="OpenStreetMap server">
              <create>
                <node id="1" version="1" visible="true" changeset="1" lat="1.0" lon="1.0" timestamp="2009-12-22" />
              </create>
            </osmChange>
        """

        // should be OK
        parser = new OsmChangesetContentParser(doc)
        ChangesetDataSet ds = parser.parse()

        assert ds.size() == 1
        HistoryOsmPrimitive p = ds.getPrimitive(new SimplePrimitiveId(1, OsmPrimitiveType.NODE));
        assert p != null
        assert p.getId() == 1
        assert p.getVersion() == 1
        assert p.getChangesetId() == 1
        assert p.getTimestamp() != null
        assert ds.getModificationType(p.getPrimitiveId()) == ChangesetModificationType.CREATED
        assert ds.isCreated(p.getPrimitiveId())
    }

    /**
     * A simple changeset content document with one updated node
     *
     */
    @Test
    public void test_OK_OneUpdatedNode() {
        OsmChangesetContentParser parser

        def String doc = """
            <osmChange version="0.6" generator="OpenStreetMap server">
              <modify>
                <node id="1" version="1" visible="true" changeset="1" lat="1.0" lon="1.0" timestamp="2009-12-22" />
              </modify>
            </osmChange>
        """

        // should be OK
        parser = new OsmChangesetContentParser(doc)
        ChangesetDataSet ds = parser.parse()

        assert ds.size() == 1
        HistoryOsmPrimitive p = ds.getPrimitive(new SimplePrimitiveId(1, OsmPrimitiveType.NODE));
        assert p != null
        assert p.getId() == 1
        assert p.getVersion() == 1
        assert p.getChangesetId() == 1
        assert p.getTimestamp() != null
        assert ds.getModificationType(p.getPrimitiveId()) == ChangesetModificationType.UPDATED
        assert ds.isUpdated(p.getPrimitiveId())
    }

    /**
     * A simple changeset content document with one deleted node
     *
     */
    @Test
    public void test_OK_OneDeletedNode() {
        OsmChangesetContentParser parser

        def String doc = """
            <osmChange version="0.6" generator="OpenStreetMap server">
              <delete>
                <node id="1" version="1" visible="true" changeset="1" lat="1.0" lon="1.0" timestamp="2009-12-22" />
              </delete>
            </osmChange>
        """

        // should be OK
        parser = new OsmChangesetContentParser(doc)
        ChangesetDataSet ds = parser.parse()

        assert ds.size() == 1
        HistoryOsmPrimitive p = ds.getPrimitive(new SimplePrimitiveId(1, OsmPrimitiveType.NODE));
        assert p != null
        assert p.getId() == 1
        assert p.getVersion() == 1
        assert p.getChangesetId() == 1
        assert p.getTimestamp() != null
        assert ds.getModificationType(p.getPrimitiveId()) == ChangesetModificationType.DELETED
        assert ds.isDeleted(p.getPrimitiveId())
    }

    /**
     * A more complex test with a document including nodes, ways, and relations.
     *
     */
    @Test
    public void test_OK_ComplexTestCase() {
        OsmChangesetContentParser parser

        def String doc = """
            <osmChange version="0.6" generator="OpenStreetMap server">
              <create>
                <node id="1" version="1" visible="true" changeset="1" lat="1.0" lon="1.0" timestamp="2009-12-22">
                  <tag k="a.key" v="a.value" />
                </node>
              </create>
              <modify>
               <way id="2" version="2" visible="true" changeset="1" timestamp="2009-12-22">
                  <nd ref="21"/>
                  <nd ref="22"/>
               </way>
             </modify>
             <delete>
                <relation id="3" version="3" visible="true" changeset="1" timestamp="2009-12-22" />
              </delete>
            </osmChange>
        """

        // should be OK
        parser = new OsmChangesetContentParser(doc)
        ChangesetDataSet ds = parser.parse()

        assert ds.size() == 3

        HistoryOsmPrimitive p = ds.getPrimitive(new SimplePrimitiveId(1, OsmPrimitiveType.NODE));
        assert p != null
        assert p.getId() == 1
        assert p.getVersion() == 1
        assert p.getChangesetId() == 1
        assert p.getTimestamp() != null
        assert ds.getModificationType(p.getPrimitiveId()) == ChangesetModificationType.CREATED
        assert ds.isCreated(p.getPrimitiveId())
        assert p.get("a.key") == "a.value"

        HistoryWay w = (HistoryWay)ds.getPrimitive(new SimplePrimitiveId(2, OsmPrimitiveType.WAY));
        assert w != null
        assert w.getId() == 2
        assert w.getVersion() == 2
        assert w.getChangesetId() == 1
        assert w.getTimestamp() != null
        assert ds.getModificationType(w.getPrimitiveId()) == ChangesetModificationType.UPDATED
        assert ds.isUpdated(w.getPrimitiveId())
        assert w.getNumNodes() == 2
        assert w.getNodes() == [21,22]

        HistoryRelation r = (HistoryRelation)ds.getPrimitive(new SimplePrimitiveId(3, OsmPrimitiveType.RELATION));
        assert r != null
        assert r.getId() == 3
        assert r.getVersion() == 3
        assert r.getChangesetId() == 1
        assert r.getTimestamp() != null
        assert ds.getModificationType(r.getPrimitiveId()) == ChangesetModificationType.DELETED
        assert ds.isDeleted(r.getPrimitiveId())
    }
}
