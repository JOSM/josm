// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.File;
import java.net.URI;
import java.util.Objects;
import java.util.function.Supplier;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;

/**
 * Test class for {@link RemoteEntry}
 */
class RemoteEntryTest {
    private static final Supplier<RemoteEntry> NULL_SUPPLIER = () -> null;
    @Test
    void testHashCodeEquals() {
        final RemoteEntry remoteEntryA = new RemoteEntry(URI.create("https://somewhere.com/image.png?hash=a"),
                NULL_SUPPLIER, NULL_SUPPLIER, NULL_SUPPLIER, NULL_SUPPLIER);
        final RemoteEntry remoteEntryB = new RemoteEntry(URI.create("https://somewhere.com/image.png?hash=b"),
                NULL_SUPPLIER, NULL_SUPPLIER, NULL_SUPPLIER, NULL_SUPPLIER);
        EqualsVerifier.simple().forClass(RemoteEntry.class).usingGetClass()
                .withIgnoredFields("firstImage", "lastImage", "nextImage", "previousImage" /* These suppliers don't have good == semantics */,
                        "width", "height" /* Width and height can be corrected later, although it is originally from exif, see #22626 */)
                .withNonnullFields("uri")
                .withPrefabValues(RemoteEntry.class, remoteEntryA, remoteEntryB)
                .withGenericPrefabValues(Supplier.class,
                        a -> () -> new RemoteEntry(URI.create("https://somewhere.com/image.png?hash=" + Objects.hash(a)),
                                NULL_SUPPLIER, NULL_SUPPLIER, NULL_SUPPLIER, NULL_SUPPLIER)).verify();
    }

    @Test
    void testNonRegression23119() {
        final String fileLocation = TestUtils.getRegressionDataFile(11685, "2015-11-08_15-33-27-Xiaomi_YI-Y0030832.jpg");
        final RemoteEntry remoteEntry = new RemoteEntry(new File(fileLocation).toURI(),
                NULL_SUPPLIER, NULL_SUPPLIER, NULL_SUPPLIER, NULL_SUPPLIER);
        assertDoesNotThrow(remoteEntry::getLastModified);
    }
}
