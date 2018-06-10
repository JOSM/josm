// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class WayDataTest {

    @Test
    @SuppressFBWarnings(value = "OBJECT_DESERIALIZATION")
    public void testSerializationForDragAndDrop() throws Exception {
        final WayData data = new WayData();
        data.setNodeIds(Arrays.asList(1415L, 9265L, 3589L, 7932L, 3846L));
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
