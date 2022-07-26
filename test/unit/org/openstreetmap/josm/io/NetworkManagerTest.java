// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.OsmApiType;

/**
 * Unit tests of {@link NetworkManager} class.
 */
@OsmApiType(OsmApiType.APIType.DEV)
class NetworkManagerTest {

    /**
     * Unit test of {@link NetworkManager#addNetworkError},
     *              {@link NetworkManager#getNetworkErrors} and
     *              {@link NetworkManager#clearNetworkErrors}.
     * @throws MalformedURLException if any error occurs
     */
    @Test
    void testNetworkErrors() throws MalformedURLException {
        NetworkManager.clearNetworkErrors();
        assertTrue(NetworkManager.getNetworkErrors().isEmpty());
        NetworkManager.addNetworkError("http://url1", new Exception("exception_1"));
        NetworkManager.addNetworkError(new URL("http://url2"), new Exception("exception_2"));
        Map<String, Throwable> errors = NetworkManager.getNetworkErrors();
        assertEquals(2, errors.size());
        assertEquals("exception_1", errors.get("http://url1").getMessage());
        assertEquals("exception_2", errors.get("http://url2").getMessage());
        NetworkManager.clearNetworkErrors();
        assertTrue(NetworkManager.getNetworkErrors().isEmpty());
    }

    /**
     * Unit test of {@link NetworkManager#setOffline} and {@link NetworkManager#getOfflineResources}.
     */
    @Test
    void testOfflineResources() {
        NetworkManager.setOnline(OnlineResource.ALL);
        assertFalse(NetworkManager.isOffline("http://www.example.com/"));
        assertTrue(NetworkManager.getOfflineResources().isEmpty());
        assertFalse(NetworkManager.isOffline(OnlineResource.JOSM_WEBSITE));
        NetworkManager.setOffline(OnlineResource.JOSM_WEBSITE);
        assertTrue(NetworkManager.isOffline("https://josm.openstreetmap.de/maps"));
        assertFalse(NetworkManager.isOffline("http://www.example.com/"));
        assertTrue(NetworkManager.isOffline(OnlineResource.JOSM_WEBSITE));
        NetworkManager.setOnline(OnlineResource.JOSM_WEBSITE);
        assertFalse(NetworkManager.isOffline("https://josm.openstreetmap.de/maps"));
        assertFalse(NetworkManager.isOffline(OnlineResource.JOSM_WEBSITE));
        NetworkManager.setOffline(OnlineResource.ALL);
        assertTrue(NetworkManager.isOffline("https://josm.openstreetmap.de/maps"));
        assertTrue(NetworkManager.isOffline("http://www.example.com/"));
        assertTrue(NetworkManager.isOffline(OnlineResource.JOSM_WEBSITE));
        assertTrue(NetworkManager.isOffline(OnlineResource.OSM_API));
        NetworkManager.setOnline(OnlineResource.ALL);
    }
}
