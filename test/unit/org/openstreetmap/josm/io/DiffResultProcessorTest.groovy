// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static groovy.test.GroovyAssert.shouldFail
import static org.junit.Assert.*

import org.junit.Test
import org.openstreetmap.josm.data.coor.LatLon
import org.openstreetmap.josm.data.osm.Changeset
import org.openstreetmap.josm.data.osm.Node
import org.openstreetmap.josm.data.osm.OsmPrimitiveType
import org.openstreetmap.josm.data.osm.Relation
import org.openstreetmap.josm.data.osm.SimplePrimitiveId
import org.openstreetmap.josm.data.osm.Way
import org.openstreetmap.josm.gui.progress.NullProgressMonitor
import org.openstreetmap.josm.tools.XmlParsingException

class DiffResultProcessorTest {

    @Test
    public void testConstructor() {
        Node n = new Node(1)
        // these calls should not fail
        //
        new DiffResultProcessor(null)
        new DiffResultProcessor([])
        new DiffResultProcessor([n])
    }

    @Test
    public void testParse_NOK_Cases() {
        def DiffResultProcessor processor  = new DiffResultProcessor([])

        shouldFail(IllegalArgumentException) {
            processor.parse null, NullProgressMonitor.INSTANCE
        }

        shouldFail(XmlParsingException) {
            processor.parse "", NullProgressMonitor.INSTANCE
        }

        shouldFail(XmlParsingException) {
            processor.parse "<x></x>", NullProgressMonitor.INSTANCE
        }
    }

    @Test
    public void testParse_OK_Cases() {
        def DiffResultProcessor processor  = new DiffResultProcessor([])
        String doc = """\
        <diffResult version="0.6" generator="Test Data">
            <node old_id="-1" new_id="1" new_version="1"/>
            <way old_id="-2" new_id="2" new_version="1"/>
            <relation old_id="-3" new_id="3" new_version="1"/>
        </diffResult>
        """

        processor.parse doc, null
        assert processor.@diffResults.size() == 3
        SimplePrimitiveId id = new SimplePrimitiveId(-1, OsmPrimitiveType.NODE)
        def entry = processor.@diffResults[id]
        assert entry != null
        assert entry.newId == 1
        assert entry.newVersion == 1

        id = new SimplePrimitiveId(-2, OsmPrimitiveType.WAY)
        entry = processor.@diffResults[id]
        assert entry != null
        assert entry.newId == 2
        assert entry.newVersion == 1

        id = new SimplePrimitiveId(-3, OsmPrimitiveType.RELATION)
        entry = processor.@diffResults[id]
        assert entry != null
        assert entry.newId == 3
        assert entry.newVersion == 1
    }

    @Test
    public void testPostProcess_Invocation_Variants() {
        def DiffResultProcessor processor  = new DiffResultProcessor([])
        String doc = """\
        <diffResult version="0.6" generator="Test Data">
            <node old_id="-1" new_id="1" new_version="1"/>
            <way old_id="-2" new_id="2" new_version="1"/>
            <relation old_id="-3" new_id="3" new_version="1"/>
        </diffResult>
        """

        processor.parse doc, null

        // should all be ok
        //
        processor.postProcess null, null
        processor.postProcess null, NullProgressMonitor.INSTANCE
        processor.postProcess new Changeset(1), null
        processor.postProcess new Changeset(1), NullProgressMonitor.INSTANCE
    }

    @Test
    public void testPostProcess_OK() {

        Node n = new Node()
        Way w = new Way()
        Relation r = new Relation()

        String doc = """\
            <diffResult version="0.6" generator="Test Data">
                <node old_id="${n.uniqueId}" new_id="1" new_version="10"/>
                <way old_id="${w.uniqueId}" new_id="2" new_version="11"/>
                <relation old_id="${r.uniqueId}" new_id="3" new_version="12"/>
            </diffResult>
            """

        def DiffResultProcessor processor  = new DiffResultProcessor([n,w,r])
        processor.parse doc, null
        def processed = processor.postProcess(new Changeset(5), null)
        assert processed.size() == 3
        n = processed.find {it.uniqueId == 1}
        assert n.changesetId == 5
        assert n.version == 10

        w = processed.find {it.uniqueId == 2}
        assert w.changesetId == 5
        assert w.version == 11

        r = processed.find {it.uniqueId == 3}
        assert r.changesetId == 5
        assert r.version == 12
    }

    @Test
    public void testPostProcess_ForCreatedElement() {

        Node n = new Node()
        String doc = """\
            <diffResult version="0.6" generator="Test Data">
                <node old_id="${n.uniqueId}" new_id="1" new_version="1"/>
            </diffResult>
            """

        def DiffResultProcessor processor  = new DiffResultProcessor([n])
        processor.parse doc, null
        def processed = processor.postProcess(new Changeset(5), null)
        assert processed.size() == 1
        n = processed.find {it.uniqueId == 1}
        assert n.changesetId == 5
        assert n.version == 1
    }

    @Test
    public void testPostProcess_ForModifiedElement() {

        Node n = new Node(1)
        n.coor = new LatLon(1,1)
        n.setOsmId 1, 2
        n.modified = true
        String doc = """\
            <diffResult version="0.6" generator="Test Data">
                <node old_id="${n.uniqueId}" new_id="${n.uniqueId}" new_version="3"/>
            </diffResult>
            """

        def DiffResultProcessor processor  = new DiffResultProcessor([n])
        processor.parse doc, null
        def processed = processor.postProcess(new Changeset(5), null)
        assert processed.size() == 1
        n = processed.find {it.uniqueId == 1}
        assert n.changesetId == 5
        assert n.version == 3
    }

    @Test
    public void testPostProcess_ForDeletedElement() {

        Node n = new Node(1)
        n.coor = new LatLon(1,1)
        n.setOsmId 1, 2
        n.deleted = true
        String doc = """\
            <diffResult version="0.6" generator="Test Data">
                <node old_id="${n.uniqueId}"/>
            </diffResult>
            """

        def DiffResultProcessor processor  = new DiffResultProcessor([n])
        processor.parse doc, null
        def processed = processor.postProcess(new Changeset(5), null)
        assert processed.size() == 1
        n = processed.find {it.uniqueId == 1}
        assert n.changesetId == 5
        assert n.version == 2
    }
}
