// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

class NodeDataTest {

    @SuppressFBWarnings(value = "OBJECT_DESERIALIZATION")
    private static NodeData serializeUnserialize(NodeData data) throws IOException, ClassNotFoundException {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(data);
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                return (NodeData) in.readObject();
            }
        }
    }

    @Test
    void testSerializationForDragAndDrop() throws Exception {
        final NodeData data = new NodeData();
        data.setCoor(new LatLon(31.14, 15.9));
        data.setId(314);
        data.setVersion(14);
        data.setChangesetId(314159);
        final NodeData readData = serializeUnserialize(data);
        Assert.assertEquals(data.toString(), readData.toString());
    }

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/13395">#13395</a>.
     * @throws Exception if an error occurs
     */
    @Test
    void testTicket13395() throws Exception {
        Node n = new Node(1925320646, 1);
        n.setCoor(null);
        assertNull(n.getCoor());
        assertTrue(n.isIncomplete());

        NodeData data = n.save();
        assertNull(data.getCoor());
        assertTrue(data.isIncomplete());

        NodeData readData = serializeUnserialize(data);
        assertNull(readData.getCoor());
        assertTrue(readData.isIncomplete());
    }
}
