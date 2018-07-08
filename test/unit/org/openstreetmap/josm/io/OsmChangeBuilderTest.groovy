package org.openstreetmap.josm.io;

import static groovy.test.GroovyAssert.shouldFail
import static org.junit.Assert.*

import org.junit.Test
import org.openstreetmap.josm.data.coor.LatLon
import org.openstreetmap.josm.data.osm.Changeset
import org.openstreetmap.josm.data.osm.Node

class OsmChangeBuilderTest {

    /**
     * Test various constructor invocations
     */
    @Test
    public void testConstructor() {
        def Changeset cs = new Changeset(1)
        // should not fail
        OsmChangeBuilder builder = new OsmChangeBuilder(cs)

        // should not fail either - null allowed
        builder = new OsmChangeBuilder(null)

        // should not fail
        builder = new OsmChangeBuilder(cs, "0.5")

        builder = new OsmChangeBuilder(cs, null)

        builder = new OsmChangeBuilder(null, null)
    }

    /**
     * Test the sequence of method calls. Should throw IllegalStateException if
     * the protocol start(),append()*, finish() is violated.
     */
    @Test
    public void testSequenceOfMethodCalls() {
        def Changeset cs = new Changeset(1)
        OsmChangeBuilder builder = new OsmChangeBuilder(cs)

        // should be OK
        builder.start()
        Node n = new Node(LatLon.ZERO)
        builder.append n
        builder.finish()

        shouldFail(IllegalStateException) {
            builder = new OsmChangeBuilder(cs)
            builder.append n
        }

        shouldFail(IllegalStateException) {
            builder = new OsmChangeBuilder(cs)
            builder.append([n])
        }

        shouldFail(IllegalStateException) {
            builder = new OsmChangeBuilder(cs)
            builder.finish()
        }

        shouldFail(IllegalStateException) {
            builder = new OsmChangeBuilder(cs)
            builder.start()
            builder.start()
        }
    }

    @Test
    public void testDocumentWithNewNode() {
        def Changeset cs = new Changeset(1)
        OsmChangeBuilder builder = new OsmChangeBuilder(cs)
        Node n = new Node(LatLon.ZERO)

        builder.start()
        builder.append n
        builder.finish()

        def doc = new XmlParser().parseText(builder.document)
        assert doc.@version == "0.6"
        assert doc.@generator == "JOSM"
        assert doc.name() == "osmChange"
        assert doc.children().size() == 1
        def create = doc.create
        assert create != null

        assert create.size() == 1
        def nodes = create[0].node
        assert nodes.size() == 1
        def node = nodes[0]
        assert node.@id == n.uniqueId.toString()
        assert node.@lat != null
        assert node.@lon != null
        assert node.@changeset == cs.id.toString()
    }

    /**
     * Test building a document with a modified node
     */
    @Test
    public void testDocumentWithModifiedNode() {
        def Changeset cs = new Changeset(1)
        OsmChangeBuilder builder = new OsmChangeBuilder(cs)
        Node n = new Node(1)
        n.coor = LatLon.ZERO
        n.incomplete = false
        n.modified = true

        builder.start()
        builder.append n
        builder.finish()

        def doc = new XmlParser().parseText(builder.document)
        assert doc.@version == "0.6"
        assert doc.@generator == "JOSM"
        assert doc.name() == "osmChange"
        assert doc.children().size() == 1
        def modify = doc.modify
        assert modify != null

        assert modify.size() == 1
        def nodes = modify[0].node
        assert nodes.size() == 1
        def node = nodes[0]
        assert node.@id == n.uniqueId.toString()
        assert node.@lat != null
        assert node.@lon != null
        assert node.@changeset == cs.id.toString()
    }

    /**
     * Test building a document with a deleted node
     */
    @Test
    public void testDocumentWithDeletedNode() {
        def Changeset cs = new Changeset(1)
        OsmChangeBuilder builder = new OsmChangeBuilder(cs)
        Node n = new Node(1)
        n.coor = LatLon.ZERO
        n.incomplete = false
        n.deleted = true

        builder.start()
        builder.append n
        builder.finish()

        def doc = new XmlParser().parseText(builder.document)
        assert doc.@version == "0.6"
        assert doc.@generator == "JOSM"
        assert doc.name() == "osmChange"
        assert doc.children().size() == 1
        def delete = doc.delete
        assert delete != null

        assert delete.size() == 1
        def nodes = delete[0].node
        assert nodes.size() == 1
        def node = nodes[0]
        assert node.@id == n.uniqueId.toString()
        assert node.@lat == null
        assert node.@lon == null
        assert node.@changeset == cs.id.toString()
    }

    /**
     * Test building a mixed document.
     *
     */
    @Test
    public void testMixed() {
        def Changeset cs = new Changeset(1)
        OsmChangeBuilder builder = new OsmChangeBuilder(cs)
        Node n1 = new Node(1)
        n1.coor = LatLon.ZERO
        n1.incomplete = false
        n1.deleted = true

        Node n2 = new Node(LatLon.ZERO)

        Node n3 = new Node(2)
        n3.coor = LatLon.ZERO
        n3.incomplete = false
        n3.modified = true

        builder.start()
        builder.append([n1,n2,n3])
        builder.finish()

        def doc = new XmlParser().parseText(builder.document)

        assert doc.children().size() == 3
        assert doc.children()[0].name() == "delete"
        assert doc.children()[1].name() == "create"
        assert doc.children()[2].name() == "modify"

        def node = doc.children()[0].node[0]
        assert node.@id == n1.uniqueId.toString()

        node = doc.children()[1].node[0]
        assert node.@id == n2.uniqueId.toString()

        node = doc.children()[2].node[0]
        assert node.@id == n3.uniqueId.toString()
    }
}
