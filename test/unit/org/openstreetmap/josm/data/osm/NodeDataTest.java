// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Assert;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;

public class NodeDataTest {
    @Test
    public void testSerializationForDragAndDrop() throws Exception {
        final NodeData data = new NodeData();
        data.setCoor(new LatLon(31.14, 15.9));
        data.setId(314);
        data.setVersion(14);
        data.setChangesetId(314159);
        final Object readData;
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(data);
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                readData = in.readObject();
            }
        }
        Assert.assertEquals(data.toString(), readData.toString());
    }
}
