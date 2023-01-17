// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import java.net.URI;
import java.util.Objects;
import java.util.function.Supplier;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

/**
 * Test class for {@link RemoteEntry}
 */
class RemoteEntryTest {
    @Test
    void testHashCodeEquals() {
        RemoteEntry remoteEntryA = new RemoteEntry(URI.create("https://somewhere.com/image.png?hash=a"),
                () -> null, () -> null, () -> null, () -> null);
        RemoteEntry remoteEntryB = new RemoteEntry(URI.create("https://somewhere.com/image.png?hash=b"),
                () -> null, () -> null, () -> null, () -> null);
        EqualsVerifier.simple().forClass(RemoteEntry.class).usingGetClass()
                .withIgnoredFields("firstImage", "lastImage", "nextImage", "previousImage" /* These suppliers don't have good == semantics */,
                        "width", "height" /* Width and height can be corrected later, although it is originally from exif, see #22626 */)
                .withNonnullFields("uri")
                .withPrefabValues(RemoteEntry.class, remoteEntryA, remoteEntryB)
                .withGenericPrefabValues(Supplier.class,
                        a -> () -> new RemoteEntry(URI.create("https://somewhere.com/image.png?hash=" + Objects.hash(a)),
                                () -> null, () -> null, () -> null, () -> null)).verify();
    }
}
