// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.stream.XMLStreamException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openstreetmap.josm.spi.preferences.IBaseDirectories;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.OsmApi;
import org.xml.sax.SAXException;

/**
 * Unit tests of {@link Preferences}.
 */
@BasicPreferences
@OsmApi(OsmApi.APIType.FAKE)
class PreferencesTest {
    /**
     * Test {@link Preferences#toXML}.
     */
    @Test
    void testToXml() {
        assertEquals(String.format(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>%n" +
            "<preferences xmlns='http://josm.openstreetmap.de/preferences-1.0' version='%d'>%n" +
            "  <tag key='osm-server.url' value='http://fake.xxx/api'/>%n" +
            "</preferences>%n", Version.getInstance().getVersion()),
                Preferences.main().toXML(true));
    }

    /**
     * The OAuth 1.0a access token, which nothing has used since OAuth 1.0a was removed, is deleted from the
     * preferences when they are loaded, so that it does not stay on disk indefinitely.
     * @param dir a temporary directory for the preferences file
     * @throws IOException if the preferences file cannot be written or read
     * @throws SAXException if the preferences file is not valid
     * @throws XMLStreamException if the preferences file cannot be parsed
     */
    @Test
    void testObsoleteOAuth10aTokenIsRemoved(@TempDir Path dir) throws IOException, SAXException, XMLStreamException {
        Files.write(dir.resolve("preferences.xml"), String.format(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>%n" +
            "<preferences xmlns='http://josm.openstreetmap.de/preferences-1.0' version='19000'>%n" +
            "  <tag key='oauth.access-token.key' value='an-old-key'/>%n" +
            "  <tag key='oauth.access-token.secret' value='an-old-secret'/>%n" +
            "  <tag key='osm-server.url' value='http://fake.xxx/api'/>%n" +
            "</preferences>%n").getBytes(StandardCharsets.UTF_8));
        final File directory = dir.toFile();
        final Preferences preferences = new Preferences(new IBaseDirectories() {
            @Override
            public File getPreferencesDirectory(boolean createIfMissing) {
                return directory;
            }

            @Override
            public File getUserDataDirectory(boolean createIfMissing) {
                return directory;
            }

            @Override
            public File getCacheDirectory(boolean createIfMissing) {
                return directory;
            }
        });
        preferences.load();

        assertNull(preferences.get("oauth.access-token.key", null));
        assertNull(preferences.get("oauth.access-token.secret", null));
        assertEquals("http://fake.xxx/api", preferences.get("osm-server.url", null));
    }
}
