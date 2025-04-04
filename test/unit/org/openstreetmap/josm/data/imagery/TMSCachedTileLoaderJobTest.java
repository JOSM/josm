// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import org.apache.commons.jcs3.access.behavior.ICacheAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.gui.jmapviewer.Tile;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoaderListener;
import org.openstreetmap.gui.jmapviewer.tilesources.TMSTileSource;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.cache.BufferedImageCacheEntry;
import org.openstreetmap.josm.data.cache.CacheEntryAttributes;
import org.openstreetmap.josm.data.cache.JCSCacheManager;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.BasicWiremock;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

import com.github.tomakehurst.wiremock.client.WireMock;

/**
 * Unit tests for class {@link TMSCachedTileLoaderJob}.
 */
@BasicWiremock
@BasicPreferences
class TMSCachedTileLoaderJobTest {
    /**
     * mocked tile server
     */
    private WireMockRuntimeInfo wireMockRuntimeInfo;

    @BeforeEach
    void clearCache() {
        getCache().clear();
    }

    @BeforeEach
    void setup(WireMockRuntimeInfo wireMockRuntimeInfo) {
        this.wireMockRuntimeInfo = wireMockRuntimeInfo;
    }

    private static ICacheAccess<String, BufferedImageCacheEntry> getCache() {
        return JCSCacheManager.getCache("test");
    }

    private static class TestCachedTileLoaderJob extends TMSCachedTileLoaderJob {
        private final String url;
        private final String key;

        TestCachedTileLoaderJob(TileLoaderListener listener, Tile tile, String key) throws IOException {
            this(listener, tile, key, (int) TimeUnit.DAYS.toSeconds(1));
        }

        TestCachedTileLoaderJob(TileLoaderListener listener, Tile tile, String key, int minimumExpiry) throws IOException {
            super(listener, tile, getCache(), new TileJobOptions(30000, 30000, null, minimumExpiry),
                    (ThreadPoolExecutor) Executors.newFixedThreadPool(1));

            this.url = tile.getUrl();
            this.key = key;
        }

        @Override
        public URL getUrl() {
            try {
                return new URL(url);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected BufferedImageCacheEntry createCacheEntry(byte[] content) {
            return new BufferedImageCacheEntry(content);
        }

        public CacheEntryAttributes getAttributes() {
            return attributes;
        }

        @Override
        public boolean isObjectLoadable() {
            // use implementation from grand parent, to avoid calling getImage on dummy data
            if (cacheData == null) {
                return false;
            }
            return cacheData.getContent().length > 0;
        }
    }

    private static final class Listener implements TileLoaderListener {
        private CacheEntryAttributes attributes;
        private boolean ready;
        private byte[] data;

        @Override
        public synchronized void tileLoadingFinished(Tile tile, boolean success) {
            ready = true;
            this.notifyAll();
        }
    }

    private static class MockTile extends Tile {
        MockTile(String url) {
            super(new MockTileSource(url), 0, 0, 0);
        }
    }

    private static class MockTileSource extends TMSTileSource {
        private final String url;

        MockTileSource(String url) {
            super(new ImageryInfo("mock"));
            this.url = url;
        }

        @Override
        public String getTileUrl(int zoom, int tilex, int tiley) {
            return url;
        }
    }

    /**
     * Tests that {@code TMSCachedTileLoaderJob#SERVICE_EXCEPTION_PATTERN} is correct.
     */
    @Test
    void testServiceExceptionPattern() {
        testServiceException("missing parameters ['version', 'format']",
                "<?xml version=\"1.0\"?>\n" +
                "<!DOCTYPE ServiceExceptionReport SYSTEM \"http://schemas.opengis.net/wms/1.1.1/exception_1_1_1.dtd\">\n" +
                "<ServiceExceptionReport version=\"1.1.1\">\n" +
                "    <ServiceException>missing parameters ['version', 'format']</ServiceException>\n" +
                "</ServiceExceptionReport>");
        testServiceException("Parameter 'layers' contains unacceptable layer names.",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\r\n" +
                "<!DOCTYPE ServiceExceptionReport SYSTEM \"http://schemas.opengis.net/wms/1.1.1/exception_1_1_1.dtd\">\r\n" +
                "<ServiceExceptionReport version=\"1.1.1\">\r\n" +
                "  <ServiceException code=\"LayerNotDefined\">\r\n" +
                "Parameter 'layers' contains unacceptable layer names.\r\n" +
                "  </ServiceException>\r\n" +
                "</ServiceExceptionReport>\r\n" +
                "");
    }

    /**
     * Tests that {@code TMSCachedTileLoaderJob#CDATA_PATTERN} is correct.
     */
    @Test
    void testCdataPattern() {
        testCdata("received unsuitable wms request: no <grid> with suitable srs found for layer capitais",
                "<![CDATA[\r\n" +
                "received unsuitable wms request: no <grid> with suitable srs found for layer capitais\r\n" +
                "]]>");
    }

    /**
     * Tests that {@code TMSCachedTileLoaderJob#JSON_PATTERN} is correct.
     */
    @Test
    void testJsonPattern() {
        testJson("Tile does not exist",
                "{\"message\":\"Tile does not exist\"}");
    }

    private static void testServiceException(String expected, String xml) {
        test(TMSCachedTileLoaderJob.SERVICE_EXCEPTION_PATTERN, expected, xml);
    }

    private static void testCdata(String expected, String xml) {
        test(TMSCachedTileLoaderJob.CDATA_PATTERN, expected, xml);
    }

    private static void testJson(String expected, String json) {
        test(TMSCachedTileLoaderJob.JSON_PATTERN, expected, json);
    }

    private static void test(Pattern pattern, String expected, String text) {
        Matcher m = pattern.matcher(text);
        assertTrue(m.matches(), text);
        assertEquals(expected, Utils.strip(m.group(1)));
    }

    private TestCachedTileLoaderJob submitJob(MockTile tile, String key, boolean force) throws IOException {
        return submitJob(tile, key, 0, force);
    }

    private TestCachedTileLoaderJob submitJob(MockTile tile, String key, int minimumExpiry, boolean force) throws IOException {
        Listener listener = new Listener();
        TestCachedTileLoaderJob job = new TestCachedTileLoaderJob(listener, tile, key, minimumExpiry);
        job.submit(force);
        synchronized (listener) {
            while (!listener.ready) {
                try {
                    listener.wait();
                } catch (InterruptedException e) {
                    // do nothing, wait
                    Logging.trace(e);
                }
            }
        }
        return job;
    }

    /**
     * When tile server doesn't return any Expires/Cache-Control headers, expire should be at least MINIMUM_EXPIRES
     * @throws IOException exception
     */
    @Test
    void testNoCacheHeaders() throws IOException {
        long testStart = System.currentTimeMillis();
        wireMockRuntimeInfo.getWireMock().register(
                WireMock.get(WireMock.urlEqualTo("/test"))
                .willReturn(WireMock.aResponse()
                        .withBody("mock entry")
                        )
                );

        TestCachedTileLoaderJob job = submitJob(new MockTile(wireMockRuntimeInfo.getHttpBaseUrl() + "/test"), "test", false);
        assertExpirationAtLeast(testStart + TMSCachedTileLoaderJob.MINIMUM_EXPIRES.get(), job);
        assertArrayEquals("mock entry".getBytes(StandardCharsets.UTF_8), job.get().getContent());
        job = submitJob(new MockTile(wireMockRuntimeInfo.getHttpBaseUrl() + "/test"), "test", false); // submit another job for the same tile
        // only one request to tile server should be made, second should come from cache
        wireMockRuntimeInfo.getWireMock().verifyThat(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/test")));
        assertArrayEquals("mock entry".getBytes(StandardCharsets.UTF_8), job.get().getContent());
    }

    /**
     * When tile server doesn't return any Expires/Cache-Control headers, expire should be at least minimumExpires parameter
     * @throws IOException exception
     */
    @Test
    void testNoCacheHeadersMinimumExpires() throws IOException {
        noCacheHeadersMinimumExpires((int) TimeUnit.MILLISECONDS.toSeconds(TMSCachedTileLoaderJob.MINIMUM_EXPIRES.get() * 2));
    }

    /**
     * When tile server doesn't return any Expires/Cache-Control headers, expire should be at least minimumExpires parameter,
     * which is larger than MAXIMUM_EXPIRES
     * @throws IOException exception
     */

    @Test
    void testNoCacheHeadersMinimumExpiresLargerThanMaximum() throws IOException {
        noCacheHeadersMinimumExpires((int) TimeUnit.MILLISECONDS.toSeconds(TMSCachedTileLoaderJob.MAXIMUM_EXPIRES.get() * 2));
    }

    private void noCacheHeadersMinimumExpires(int minimumExpires) throws IOException {
        long testStart = System.currentTimeMillis();
        wireMockRuntimeInfo.getWireMock().register(
                WireMock.get(WireMock.urlEqualTo("/test"))
                .willReturn(WireMock.aResponse()
                        .withBody("mock entry")
                        )
                );
        TestCachedTileLoaderJob job = submitJob(new MockTile(wireMockRuntimeInfo.getHttpBaseUrl() + "/test"), "test", minimumExpires, false);
        assertExpirationAtLeast(testStart + minimumExpires, job);
        assertArrayEquals("mock entry".getBytes(StandardCharsets.UTF_8), job.get().getContent());
        job = submitJob(new MockTile(wireMockRuntimeInfo.getHttpBaseUrl() + "/test"), "test", false); // submit another job for the same tile
        // only one request to tile server should be made, second should come from cache
        wireMockRuntimeInfo.getWireMock().verifyThat(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/test")));
        assertArrayEquals("mock entry".getBytes(StandardCharsets.UTF_8), job.get().getContent());
    }

    /**
     * When tile server returns Expires header shorter than MINIMUM_EXPIRES, we should cache if for at least MINIMUM_EXPIRES
     * @throws IOException exception
     */
    @Test
    void testShortExpire() throws IOException {
        long testStart = System.currentTimeMillis();
        long expires = TMSCachedTileLoaderJob.MINIMUM_EXPIRES.get() / 2;
        wireMockRuntimeInfo.getWireMock().register(
                WireMock.get(WireMock.urlEqualTo("/test"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Expires", TestUtils.getHTTPDate(testStart + expires))
                        .withBody("mock entry")
                        )
                );
        TestCachedTileLoaderJob job = submitJob(new MockTile(wireMockRuntimeInfo.getHttpBaseUrl() + "/test"), "test", false);
        assertExpirationAtLeast(testStart + TMSCachedTileLoaderJob.MINIMUM_EXPIRES.get(), job);
        assertArrayEquals("mock entry".getBytes(StandardCharsets.UTF_8), job.get().getContent());
        job = submitJob(new MockTile(wireMockRuntimeInfo.getHttpBaseUrl() + "/test"), "test", false); // submit another job for the same tile
        // only one request to tile server should be made, second should come from cache
        wireMockRuntimeInfo.getWireMock().verifyThat(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/test")));
        assertArrayEquals("mock entry".getBytes(StandardCharsets.UTF_8), job.get().getContent());
    }

    private void assertExpirationAtLeast(long duration, TestCachedTileLoaderJob job) {
        assertTrue(job.getAttributes().getExpirationTime() >= duration, "Expiration time shorter by " +
                                -1 * (job.getAttributes().getExpirationTime() - duration) +
                                " than expected");
    }

    private void assertExpirationAtMost(long duration, TestCachedTileLoaderJob job) {
        assertTrue(job.getAttributes().getExpirationTime() <= duration, "Expiration time longer by " +
                                (job.getAttributes().getExpirationTime() - duration) +
                                " than expected");
    }

    @Test
    void testLongExpire() throws IOException {
        long testStart = System.currentTimeMillis();
        long expires = TMSCachedTileLoaderJob.MAXIMUM_EXPIRES.get() * 2;
        wireMockRuntimeInfo.getWireMock().register(
                WireMock.get(WireMock.urlEqualTo("/test"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Expires", TestUtils.getHTTPDate(testStart + expires))
                        .withBody("mock entry")
                        )
                );
        TestCachedTileLoaderJob job = submitJob(new MockTile(wireMockRuntimeInfo.getHttpBaseUrl() + "/test"), "test", false);
        // give 1 second margin
        assertExpirationAtMost(testStart + TMSCachedTileLoaderJob.MAXIMUM_EXPIRES.get() + TimeUnit.SECONDS.toMillis(1), job);

        assertArrayEquals("mock entry".getBytes(StandardCharsets.UTF_8), job.get().getContent());
        job = submitJob(new MockTile(wireMockRuntimeInfo.getHttpBaseUrl() + "/test"), "test", false); // submit another job for the same tile
        // only one request to tile server should be made, second should come from cache
        wireMockRuntimeInfo.getWireMock().verifyThat(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/test")));
        assertArrayEquals("mock entry".getBytes(StandardCharsets.UTF_8), job.get().getContent());
    }
}
