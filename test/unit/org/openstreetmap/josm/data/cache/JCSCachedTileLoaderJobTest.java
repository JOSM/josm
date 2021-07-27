// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.cache;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.headRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.status;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.commons.jcs3.access.behavior.ICacheAccess;
import org.apache.commons.jcs3.engine.behavior.ICacheElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.cache.ICachedLoaderListener.LoadResult;
import org.openstreetmap.josm.data.imagery.TileJobOptions;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.BasicWiremock;
import org.openstreetmap.josm.tools.Logging;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.UrlPattern;

/**
 * Unit tests for class {@link JCSCachedTileLoaderJob}.
 */
@BasicWiremock
@BasicPreferences
@Timeout(20)
class JCSCachedTileLoaderJobTest {

    /**
     * mocked tile server
     */
    @BasicWiremock
    WireMockServer tileServer;

    private static class TestCachedTileLoaderJob extends JCSCachedTileLoaderJob<String, CacheEntry> {
        private final String url;
        private final String key;

        TestCachedTileLoaderJob(String url, String key) {
            this(url, key, (int) TimeUnit.DAYS.toSeconds(1));
        }

        TestCachedTileLoaderJob(String url, String key, int minimumExpiry) {
            super(getCache(), new TileJobOptions(30000, 30000, null, minimumExpiry));

            this.url = url;
            this.key = key;
        }

        @Override
        public String getCacheKey() {
            return key;
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
        protected CacheEntry createCacheEntry(byte[] content) {
            return new CacheEntry(content);
        }
    }

    private static class Listener implements ICachedLoaderListener {
        private CacheEntryAttributes attributes;
        private boolean ready;
        private LoadResult result;
        private byte[] data;

        @Override
        public synchronized void loadingFinished(CacheEntry data, CacheEntryAttributes attributes, LoadResult result) {
            this.attributes = attributes;
            this.ready = true;
            this.result = result;
            if (data != null) {
                this.data = data.content;
            }
            this.notifyAll();
        }
    }

    /**
     * Always clear cache before tests
     * @throws Exception when clearing fails
     */
    @BeforeEach
    void clearCache() throws Exception {
        getCache().clear();
    }

    /**
     * Test status codes
     * @throws InterruptedException in case of thread interruption
     * @throws IOException in case of I/O error
     */
    @Test
    void testStatusCodes() throws IOException, InterruptedException {
        doTestStatusCode(200);
        doTestStatusCode(401);
        doTestStatusCode(402);
        doTestStatusCode(403);
        doTestStatusCode(404);
        doTestStatusCode(405);
        doTestStatusCode(500);
        doTestStatusCode(501);
        doTestStatusCode(502);
    }

    /**
     * Test unknown host
     * @throws IOException in case of I/O error
     */
    @Test
    void testUnknownHost() throws IOException {
        String key = "key_unknown_host";
        TestCachedTileLoaderJob job = new TestCachedTileLoaderJob("http://unkownhost.unkownhost/unkown", key);
        Listener listener = submitJob(job);
        assertEquals(LoadResult.FAILURE, listener.result); // because response will be cached, and that is checked below
        assertEquals("java.net.UnknownHostException: unkownhost.unkownhost", listener.attributes.getErrorMessage());

        ICacheAccess<String, CacheEntry> cache = getCache();
        CacheEntry e = new CacheEntry(new byte[]{0, 1, 2, 3});
        CacheEntryAttributes attributes = new CacheEntryAttributes();
        attributes.setExpirationTime(2);
        cache.put(key, e, attributes);

        job = new TestCachedTileLoaderJob("http://unkownhost.unkownhost/unkown", key);
        listener = submitJob(job);
        assertEquals(LoadResult.SUCCESS, listener.result);
        assertFalse(job.isCacheElementValid());
    }

    private void doTestStatusCode(int responseCode) throws IOException {
        tileServer.stubFor(get(urlEqualTo("/httpstat/" + responseCode)).willReturn(aResponse().withStatus(responseCode)));
        TestCachedTileLoaderJob job = getStatusLoaderJob(responseCode);
        Listener listener = submitJob(job);
        assertEquals(responseCode, listener.attributes.getResponseCode());
    }

    private Listener submitJob(TestCachedTileLoaderJob job) throws IOException {
        return submitJob(job, true);
    }

    private Listener submitJob(TestCachedTileLoaderJob job, boolean force) throws IOException {
        Listener listener = new Listener();
        job.submit(listener, force);
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
        return listener;
    }

    /**
     * That no request is made when entry is in cache and force == false
     * @throws IOException exception
     */
    @Test
    void testNoRequestMadeWhenEntryInCache() throws IOException {
        ICacheAccess<String, CacheEntry> cache = getCache();
        long expires = TimeUnit.DAYS.toMillis(1);
        long testStart = System.currentTimeMillis();
        cache.put("test",
                new CacheEntry("cached entry".getBytes(StandardCharsets.UTF_8)),
                createEntryAttributes(expires, 200, testStart, "eTag")
                );
        createHeadGetStub(urlEqualTo("/test"), expires, testStart, "eTag", "mock entry");

        TestCachedTileLoaderJob job = new TestCachedTileLoaderJob(tileServer.url("/test"), "test");
        Listener listener = submitJob(job, false);
        tileServer.verify(0, getRequestedFor(anyUrl()));
        assertArrayEquals("cached entry".getBytes(StandardCharsets.UTF_8), listener.data);
    }

    /**
     * that request is made, when object is in cache, but force mode is used
     * @throws IOException exception
     */
    @Test
    void testRequestMadeWhenEntryInCacheAndForce() throws IOException {
        ICacheAccess<String, CacheEntry> cache = getCache();
        long expires = TimeUnit.DAYS.toMillis(1);
        long testStart = System.currentTimeMillis();
        cache.put("test",
                new CacheEntry("cached dummy".getBytes(StandardCharsets.UTF_8)),
                createEntryAttributes(expires, 200, testStart + expires, "eTag")
                );
        createHeadGetStub(urlEqualTo("/test"), expires, testStart, "eTag", "mock entry");

        TestCachedTileLoaderJob job = new TestCachedTileLoaderJob(tileServer.url("/test"), "test");
        Listener listener = submitJob(job, true);
        tileServer.verify(1, getRequestedFor(urlEqualTo("/test")));
        assertArrayEquals("mock entry".getBytes(StandardCharsets.UTF_8), listener.data);
    }

    /**
     * Mock returns no cache-control / expires headers
     * Expire time should be set to DEFAULT_EXPIRE_TIME
     * @throws IOException exception
     */
    @Test
    void testSettingMinimumExpiryWhenNoExpires() throws IOException {
        long testStart = System.currentTimeMillis();
        tileServer.stubFor(get(urlEqualTo("/test")).willReturn(aResponse().withBody("mock entry")));

        TestCachedTileLoaderJob job = new TestCachedTileLoaderJob(tileServer.url("/test"), "test");
        Listener listener = submitJob(job, false);
        tileServer.verify(1, getRequestedFor(urlEqualTo("/test")));

        assertTrue(listener.attributes.getExpirationTime() >= testStart + JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME,
                "Cache entry expiration is " + (listener.attributes.getExpirationTime() - testStart) + " which is not larger than " +
                        JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME + " (DEFAULT_EXPIRE_TIME)");

        assertTrue(listener.attributes.getExpirationTime() <= System.currentTimeMillis() + JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME,
                "Cache entry expiration is " +
                        (listener.attributes.getExpirationTime() - System.currentTimeMillis()) +
                        " which is not less than " +
                        JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME + " (DEFAULT_EXPIRE_TIME)"
                );

        assertArrayEquals("mock entry".getBytes(StandardCharsets.UTF_8), listener.data);
    }

    /**
     * Mock returns expires headers, but Cache-Control
     * Expire time should be set to max-age
     * @throws IOException exception
     */
    @Test
    void testSettingExpireByMaxAge() throws IOException {
        long testStart = System.currentTimeMillis();
        long expires = TimeUnit.DAYS.toSeconds(1);
        tileServer.stubFor(get(urlEqualTo("/test"))
                .willReturn(aResponse()
                        .withHeader("Cache-control", "max-age=" + expires)
                        .withBody("mock entry")
                        )
                );

        TestCachedTileLoaderJob job = new TestCachedTileLoaderJob(tileServer.url("/test"), "test");
        Listener listener = submitJob(job, false);
        tileServer.verify(1, getRequestedFor(urlEqualTo("/test")));

        assertTrue(listener.attributes.getExpirationTime() >= testStart + TimeUnit.SECONDS.toMillis(expires),
                "Cache entry expiration is " + (listener.attributes.getExpirationTime() - testStart) + " which is not larger than " +
                        TimeUnit.SECONDS.toMillis(expires) + " (max-age)");

        assertTrue(
                listener.attributes.getExpirationTime() <= System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(expires),
                "Cache entry expiration is " +
                        (listener.attributes.getExpirationTime() - System.currentTimeMillis()) +
                        " which is not less than " +
                        TimeUnit.SECONDS.toMillis(expires) + " (max-age)"
                        );

        assertArrayEquals("mock entry".getBytes(StandardCharsets.UTF_8), listener.data);
    }

    /**
     * mock returns expiration: JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 10
     * minimum expire time: JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 2
     * @throws IOException exception
     */
    @Test
    void testSettingMinimumExpiryByMinimumExpiryTimeLessThanDefault() throws IOException {
        long testStart = System.currentTimeMillis();
        int minimumExpiryTimeSeconds = (int) (JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 2);

        createHeadGetStub(urlEqualTo("/test"), (JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 10), testStart, "eTag", "mock entry");

        TestCachedTileLoaderJob job = new TestCachedTileLoaderJob(tileServer.url("/test"), "test", minimumExpiryTimeSeconds);
        Listener listener = submitJob(job, false);
        tileServer.verify(1, getRequestedFor(urlEqualTo("/test")));
        assertArrayEquals("mock entry".getBytes(StandardCharsets.UTF_8), listener.data);


        assertTrue(
                listener.attributes.getExpirationTime() >= testStart + TimeUnit.SECONDS.toMillis(minimumExpiryTimeSeconds),
                "Cache entry expiration is " + (listener.attributes.getExpirationTime() - testStart) + " which is not larger than " +
                        TimeUnit.SECONDS.toMillis(minimumExpiryTimeSeconds) + " (minimumExpireTime)");

        assertTrue(
                listener.attributes.getExpirationTime() <= System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(minimumExpiryTimeSeconds),
                "Cache entry expiration is " +
                        (listener.attributes.getExpirationTime() - System.currentTimeMillis()) +
                        " which is not less than " +
                        TimeUnit.SECONDS.toMillis(minimumExpiryTimeSeconds) + " (minimumExpireTime)"
                        );
    }

    /**
     * mock returns expiration: JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 10
     * minimum expire time: JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME * 2
     * @throws IOException exception
     */

    @Test
    void testSettingMinimumExpiryByMinimumExpiryTimeGreaterThanDefault() throws IOException {
        long testStart = System.currentTimeMillis();
        int minimumExpiryTimeSeconds = (int) (JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME * 2);

        createHeadGetStub(urlEqualTo("/test"), (JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 10), testStart, "eTag", "mock entry");

        TestCachedTileLoaderJob job = new TestCachedTileLoaderJob(tileServer.url("/test"), "test", minimumExpiryTimeSeconds);
        Listener listener = submitJob(job, false);
        tileServer.verify(1, getRequestedFor(urlEqualTo("/test")));
        assertArrayEquals("mock entry".getBytes(StandardCharsets.UTF_8), listener.data);


        assertTrue(
                listener.attributes.getExpirationTime() >= testStart + TimeUnit.SECONDS.toMillis(minimumExpiryTimeSeconds),
                "Cache entry expiration is " + (listener.attributes.getExpirationTime() - testStart) + " which is not larger than " +
                        TimeUnit.SECONDS.toMillis(minimumExpiryTimeSeconds) + " (minimumExpireTime)");

        assertTrue(
                listener.attributes.getExpirationTime() <= System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(minimumExpiryTimeSeconds),
                "Cache entry expiration is " +
                        (listener.attributes.getExpirationTime() - System.currentTimeMillis()) +
                        " which is not less than " +
                        TimeUnit.SECONDS.toMillis(minimumExpiryTimeSeconds) + " (minimumExpireTime)"
                        );
    }

    /**
     * Check if Cache-Control takes precedence over max-age
     * Expires is lower - JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 10
     * Cache control : JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 2
     *
     * Both are smaller than DEFAULT_EXPIRE_TIME, so we can test, that it's not DEFAULT_EXPIRE_TIME that extended
     * expiration
     *
     * @throws IOException exception
     */

    @Test
    void testCacheControlVsExpires() throws IOException {
        long testStart = System.currentTimeMillis();
        int minimumExpiryTimeSeconds = 0;

        tileServer.stubFor(get(urlEqualTo("/test"))
                .willReturn(aResponse()
                        .withHeader("Expires", TestUtils.getHTTPDate(testStart + (JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 10)))
                        .withHeader("Cache-Control", "max-age=" +
                                TimeUnit.MILLISECONDS.toSeconds((JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 2)))
                        .withBody("mock entry")
                        )
                );
        tileServer.stubFor(head(urlEqualTo("/test"))
                .willReturn(aResponse()
                        .withHeader("Expires", TestUtils.getHTTPDate(testStart + (JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 10)))
                        .withHeader("Cache-Control", "max-age=" +
                                TimeUnit.MILLISECONDS.toSeconds((JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 2)))
                        )
                );
        TestCachedTileLoaderJob job = new TestCachedTileLoaderJob(tileServer.url("/test"), "test", minimumExpiryTimeSeconds);
        Listener listener = submitJob(job, false);
        tileServer.verify(1, getRequestedFor(urlEqualTo("/test")));
        assertArrayEquals("mock entry".getBytes(StandardCharsets.UTF_8), listener.data);


        assertTrue(
                listener.attributes.getExpirationTime() >= testStart + (JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 10),
                "Cache entry expiration is " + (listener.attributes.getExpirationTime() - testStart) + " which is not larger than " +
                        (JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 10) + " (Expires header)");

        assertTrue(listener.attributes.getExpirationTime() <= System.currentTimeMillis() + (JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 2),
                "Cache entry expiration is " +
                        (listener.attributes.getExpirationTime() - System.currentTimeMillis()) +
                        " which is not less than " +
                        (JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 2) + " (Cache-Control: max-age=)"
                        );
    }

    /**
     * Check if Cache-Control s-max-age is honored
     * mock returns expiration: JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 10
     * minimum expire time: JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME * 2
     *
     * @throws IOException exception
     */
    @Test
    void testMaxAgeVsSMaxAge() throws IOException {
        long testStart = System.currentTimeMillis();
        int minimumExpiryTimeSeconds = 0;

        tileServer.stubFor(get(urlEqualTo("/test"))
                .willReturn(aResponse()
                        .withHeader("Cache-Control", "" +
                                "max-age=" + TimeUnit.MILLISECONDS.toSeconds((JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 10)) + "," +
                                "s-max-age=" + TimeUnit.MILLISECONDS.toSeconds((JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 2))
                                )
                        .withBody("mock entry")
                        )
                );
        tileServer.stubFor(head(urlEqualTo("/test"))
                .willReturn(aResponse()
                        .withHeader("Cache-Control", "" +
                                "max-age=" + TimeUnit.MILLISECONDS.toSeconds((JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 10)) + "," +
                                "s-max-age=" + TimeUnit.MILLISECONDS.toSeconds((JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 2))
                        )
                ));
        TestCachedTileLoaderJob job = new TestCachedTileLoaderJob(tileServer.url("/test"), "test", minimumExpiryTimeSeconds);
        Listener listener = submitJob(job, false);
        tileServer.verify(1, getRequestedFor(urlEqualTo("/test")));
        assertArrayEquals("mock entry".getBytes(StandardCharsets.UTF_8), listener.data);

        assertTrue(
                listener.attributes.getExpirationTime() >= testStart + (JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 10),
                "Cache entry expiration is " + (listener.attributes.getExpirationTime() - testStart) + " which is not larger than " +
                        (JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 10) + " (Cache-Control: max-age)");

        assertTrue(listener.attributes.getExpirationTime() <= System.currentTimeMillis() + (JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 2),
                "Cache entry expiration is " +
                        (listener.attributes.getExpirationTime() - System.currentTimeMillis()) +
                        " which is not less than " +
                        (JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 2) + " (Cache-Control: s-max-age)"
                        );
    }

    /**
     * Check if verifying cache entries using HEAD requests work properly
     * @throws IOException exception
     */
    @Test
    void testCheckUsingHead() throws IOException {
        ICacheAccess<String, CacheEntry> cache = getCache();
        long expires = TimeUnit.DAYS.toMillis(1);
        long testStart = System.currentTimeMillis();
        cache.put("test",
                new CacheEntry("cached dummy".getBytes(StandardCharsets.UTF_8)),
                createEntryAttributes(-1 * expires, 200, testStart, "eTag--gzip") // Jetty adds --gzip to etags when compressing output
                );

        tileServer.stubFor(get(urlEqualTo("/test"))
                .willReturn(aResponse()
                        .withHeader("Expires", TestUtils.getHTTPDate(testStart + expires))
                        .withHeader("Last-Modified", Long.toString(testStart))
                        .withHeader("ETag", "eTag") // Jetty adds "--gzip" suffix for compressed content
                        .withBody("mock entry")
                        )
                );
        tileServer.stubFor(head(urlEqualTo("/test"))
                .willReturn(aResponse()
                        .withHeader("Expires", TestUtils.getHTTPDate(testStart + expires))
                        .withHeader("Last-Modified", Long.toString(testStart))
                        .withHeader("ETag", "eTag--gzip") // but doesn't add to uncompressed
                        )
                );

        TestCachedTileLoaderJob job = new TestCachedTileLoaderJob(tileServer.url("/test"), "test");
        Listener listener = submitJob(job, false); // cache entry is expired, no need to force refetch
        tileServer.verify(1, getRequestedFor(urlEqualTo("/test")));
        assertArrayEquals("mock entry".getBytes(StandardCharsets.UTF_8), listener.data);

        // cache entry should be retrieved from cache
        listener = submitJob(job, false);
        tileServer.verify(1, getRequestedFor(urlEqualTo("/test")));
        assertArrayEquals("mock entry".getBytes(StandardCharsets.UTF_8), listener.data);

        // invalidate entry in cache
        ICacheElement<String, CacheEntry> cacheEntry = cache.getCacheElement("test");
        CacheEntryAttributes attributes = (CacheEntryAttributes) cacheEntry.getElementAttributes();
        attributes.setExpirationTime(testStart - TimeUnit.DAYS.toMillis(1));
        cache.put("test", cacheEntry.getVal(), attributes);

        // because cache entry is invalid - HEAD request shall be made
        tileServer.verify(0, headRequestedFor(urlEqualTo("/test"))); // no head requests were made until now
        listener = submitJob(job, false);
        tileServer.verify(1, headRequestedFor(urlEqualTo("/test"))); // verify head requests were made
        tileServer.verify(1, getRequestedFor(urlEqualTo("/test"))); // verify no more get requests were made
        assertArrayEquals("mock entry".getBytes(StandardCharsets.UTF_8), listener.data);
        assertTrue(listener.attributes.getExpirationTime() >= testStart + expires);

        // cache entry should be retrieved from cache
        listener = submitJob(job, false); // cache entry is expired, no need to force refetch
        tileServer.verify(1, getRequestedFor(urlEqualTo("/test")));
        tileServer.verify(1, getRequestedFor(urlEqualTo("/test")));
        assertArrayEquals("mock entry".getBytes(StandardCharsets.UTF_8), listener.data);
    }

    /**
     * Check if server returns 304 - it will update cache attributes and not ask again for it
     * @throws IOException exception
     */
    @Test
    public void testCheckUsing304() throws IOException {
        ICacheAccess<String, CacheEntry> cache = getCache();
        long expires = TimeUnit.DAYS.toMillis(1);
        long testStart = System.currentTimeMillis();
        cache.put("test",
                new CacheEntry("cached dummy".getBytes(StandardCharsets.UTF_8)),
                createEntryAttributes(-1 * expires, 200, testStart, "eTag")
                );

        tileServer.stubFor(get(urlEqualTo("/test"))
                .willReturn(status(304)
                        .withHeader("Expires", TestUtils.getHTTPDate(testStart + expires))
                        .withHeader("Last-Modified", Long.toString(testStart))
                        .withHeader("ETag", "eTag")
                        )
                );

        TestCachedTileLoaderJob job = new TestCachedTileLoaderJob(tileServer.url("/test"), "test");
        Listener listener = submitJob(job, false);
        tileServer.verify(1, getRequestedFor(urlEqualTo("/test")));
        assertArrayEquals("cached dummy".getBytes(StandardCharsets.UTF_8), listener.data);
        assertTrue(testStart + expires <= listener.attributes.getExpirationTime());
        submitJob(job, false);
        tileServer.verify(1, getRequestedFor(urlEqualTo("/test"))); // no more requests were made
    }

    private void createHeadGetStub(UrlPattern url, long expires, long lastModified, String eTag, String body) {
        tileServer.stubFor(get(url)
                .willReturn(aResponse()
                        .withHeader("Expires", TestUtils.getHTTPDate(lastModified + expires))
                        .withHeader("Last-Modified", Long.toString(lastModified))
                        .withHeader("ETag", eTag)
                        .withBody(body)
                        )
                );
        tileServer.stubFor(head(url)
                .willReturn(aResponse()
                        .withHeader("Expires", TestUtils.getHTTPDate(lastModified + expires))
                        .withHeader("Last-Modified", Long.toString(lastModified))
                        .withHeader("ETag", eTag)
                        )
                );
    }

    private CacheEntryAttributes createEntryAttributes(long expirationTime, int responseCode, long lastModification, String eTag) {
        CacheEntryAttributes entryAttributes = new CacheEntryAttributes();
        entryAttributes.setExpirationTime(lastModification + expirationTime);
        entryAttributes.setResponseCode(responseCode);
        entryAttributes.setLastModification(lastModification);
        entryAttributes.setEtag(eTag);
        return entryAttributes;
    }

    private TestCachedTileLoaderJob getStatusLoaderJob(int responseCode) {
        return new TestCachedTileLoaderJob(tileServer.url("/httpstat/" + responseCode), "key_" + responseCode);
    }

    private static ICacheAccess<String, CacheEntry> getCache() {
        return JCSCacheManager.getCache("test");
    }
}
