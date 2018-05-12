// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.cache;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.commons.jcs.access.behavior.ICacheAccess;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.cache.ICachedLoaderListener.LoadResult;
import org.openstreetmap.josm.data.imagery.TileJobOptions;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.matching.UrlPattern;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link JCSCachedTileLoaderJob}.
 */
public class JCSCachedTileLoaderJobTest {

    /**
     * mocked tile server
     */
    @Rule
    public WireMockRule tileServer = new WireMockRule(WireMockConfiguration.options()
            .dynamicPort());

    private static class TestCachedTileLoaderJob extends JCSCachedTileLoaderJob<String, CacheEntry> {
        private String url;
        private String key;

        TestCachedTileLoaderJob(String url, String key)  {
            this(url, key, (int) TimeUnit.DAYS.toSeconds(1));
        }

        TestCachedTileLoaderJob(String url, String key, int minimumExpiry)  {
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
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Always clear cache before tests
     * @throws Exception when clearing fails
     */
    @Before
    public void clearCache() throws Exception {
        getCache().clear();
    }

    /**
     * Test status codes
     * @throws InterruptedException in case of thread interruption
     * @throws IOException in case of I/O error
     */
    @Test
    public void testStatusCodes() throws IOException, InterruptedException {
        doTestStatusCode(200);
        // can't test for 3xx, as httpstat.us redirects finally to 200 page
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
    public void testUnknownHost() throws IOException {
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
     * That no requst is made when entry is in cache and force == false
     * @throws IOException
     */
    @Test
    public void testNoRequestMadeWhenEntryInCache() throws IOException {
        ICacheAccess<String, CacheEntry> cache = getCache();
        long expires = TimeUnit.DAYS.toMillis(1);
        long testStart = System.currentTimeMillis();
        cache.put("test",
                new CacheEntry("cached entry".getBytes(StandardCharsets.UTF_8)),
                createEntryAttributes(expires, 200, testStart, "eTag")
                );
        createHeadGetStub(WireMock.urlEqualTo("/test"), expires, testStart, "eTag", "mock entry");

        TestCachedTileLoaderJob job = new TestCachedTileLoaderJob(tileServer.url("/test"), "test");
        Listener listener = submitJob(job, false);
        tileServer.verify(0, WireMock.getRequestedFor(WireMock.anyUrl()));
        assertArrayEquals("cached entry".getBytes(StandardCharsets.UTF_8), listener.data);
    }

    /**
     * that request is made, when object is in cache, but force mode is used
     * @throws IOException
     */
    @Test
    public void testRequestMadeWhenEntryInCacheAndForce() throws IOException {
        ICacheAccess<String, CacheEntry> cache = getCache();
        long expires =  TimeUnit.DAYS.toMillis(1);
        long testStart = System.currentTimeMillis();
        cache.put("test",
                new CacheEntry("cached dummy".getBytes(StandardCharsets.UTF_8)),
                createEntryAttributes(expires, 200, testStart + expires, "eTag")
                );
        createHeadGetStub(WireMock.urlEqualTo("/test"), expires, testStart, "eTag", "mock entry");

        TestCachedTileLoaderJob job = new TestCachedTileLoaderJob(tileServer.url("/test"), "test");
        Listener listener = submitJob(job, true);
        tileServer.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/test")));
        assertArrayEquals("mock entry".getBytes(StandardCharsets.UTF_8), listener.data);
    }

    /**
     * Mock returns no cache-control / expires headers
     * Expire time should be set to DEFAULT_EXPIRE_TIME
     * @throws IOException
     */
    @Test
    public void testSettingMinimumExpiryWhenNoExpires() throws IOException {
        long testStart = System.currentTimeMillis();
        tileServer.stubFor(
                WireMock.get(WireMock.urlEqualTo("/test"))
                .willReturn(WireMock.aResponse()
                        .withBody("mock entry")
                        )
                );

        TestCachedTileLoaderJob job = new TestCachedTileLoaderJob(tileServer.url("/test"), "test");
        Listener listener = submitJob(job, false);
        tileServer.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/test")));

        assertTrue("Cache entry expiration is " + (listener.attributes.getExpirationTime() - testStart) + " which is not larger than " +
                JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME + " (DEFAULT_EXPIRE_TIME)",
                listener.attributes.getExpirationTime() >= testStart + JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME);

        assertTrue("Cache entry expiration is " + (listener.attributes.getExpirationTime() - System.currentTimeMillis()) + " which is not less than " +
                JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME + " (DEFAULT_EXPIRE_TIME)",
                listener.attributes.getExpirationTime() <= System.currentTimeMillis() + JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME);

        assertArrayEquals("mock entry".getBytes(StandardCharsets.UTF_8), listener.data);
    }

    /**
     * Mock returns expires headers, but Cache-Control
     * Expire time should be set to max-age
     * @throws IOException
     */
    @Test
    public void testSettingExpireByMaxAge() throws IOException {
        long testStart = System.currentTimeMillis();
        long expires =  TimeUnit.DAYS.toSeconds(1);
        tileServer.stubFor(
                WireMock.get(WireMock.urlEqualTo("/test"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Cache-control", "max-age=" + expires)
                        .withBody("mock entry")
                        )
                );

        TestCachedTileLoaderJob job = new TestCachedTileLoaderJob(tileServer.url("/test"), "test");
        Listener listener = submitJob(job, false);
        tileServer.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/test")));

        assertTrue("Cache entry expiration is " + (listener.attributes.getExpirationTime() - testStart) + " which is not larger than " +
                TimeUnit.SECONDS.toMillis(expires) + " (max-age)",
                listener.attributes.getExpirationTime() >= testStart + TimeUnit.SECONDS.toMillis(expires));

        assertTrue("Cache entry expiration is " + (listener.attributes.getExpirationTime() - System.currentTimeMillis()) + " which is not less than " +
                TimeUnit.SECONDS.toMillis(expires) + " (max-age)",
                listener.attributes.getExpirationTime() <= System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(expires));

        assertArrayEquals("mock entry".getBytes(StandardCharsets.UTF_8), listener.data);
    }

    /**
     * mock returns expiration: JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 10
     * minimum expire time: JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 2
     * @throws IOException
     */
    @Test
    public void testSettingMinimumExpiryByMinimumExpiryTimeLessThanDefault() throws IOException {
        long testStart = System.currentTimeMillis();
        int minimumExpiryTimeSeconds = (int)(JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 2);

        createHeadGetStub(WireMock.urlEqualTo("/test"), (JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 10), testStart, "eTag", "mock entry");

        TestCachedTileLoaderJob job = new TestCachedTileLoaderJob(tileServer.url("/test"), "test", minimumExpiryTimeSeconds);
        Listener listener = submitJob(job, false);
        tileServer.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/test")));
        assertArrayEquals("mock entry".getBytes(StandardCharsets.UTF_8), listener.data);


        assertTrue("Cache entry expiration is " + (listener.attributes.getExpirationTime() - testStart) + " which is not larger than " +
                TimeUnit.SECONDS.toMillis(minimumExpiryTimeSeconds) + " (minimumExpireTime)",
                listener.attributes.getExpirationTime() >= testStart + TimeUnit.SECONDS.toMillis(minimumExpiryTimeSeconds) );

        assertTrue("Cache entry expiration is " + (listener.attributes.getExpirationTime() - System.currentTimeMillis()) + " which is not less than " +
                TimeUnit.SECONDS.toMillis(minimumExpiryTimeSeconds) + " (minimumExpireTime)",
                listener.attributes.getExpirationTime() <= System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(minimumExpiryTimeSeconds));
    }

    /**
     * mock returns expiration: JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 10
     * minimum expire time: JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME * 2
     * @throws IOException
     */

    @Test
    public void testSettingMinimumExpiryByMinimumExpiryTimeGreaterThanDefault() throws IOException {
        long testStart = System.currentTimeMillis();
        int minimumExpiryTimeSeconds = (int)(JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME * 2);

        createHeadGetStub(WireMock.urlEqualTo("/test"), (JCSCachedTileLoaderJob.DEFAULT_EXPIRE_TIME / 10), testStart, "eTag", "mock entry");

        TestCachedTileLoaderJob job = new TestCachedTileLoaderJob(tileServer.url("/test"), "test", minimumExpiryTimeSeconds);
        Listener listener = submitJob(job, false);
        tileServer.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/test")));
        assertArrayEquals("mock entry".getBytes(StandardCharsets.UTF_8), listener.data);


        assertTrue("Cache entry expiration is " + (listener.attributes.getExpirationTime() - testStart) + " which is not larger than " +
                TimeUnit.SECONDS.toMillis(minimumExpiryTimeSeconds) + " (minimumExpireTime)",
                listener.attributes.getExpirationTime() >= testStart + TimeUnit.SECONDS.toMillis(minimumExpiryTimeSeconds) );

        assertTrue("Cache entry expiration is " + (listener.attributes.getExpirationTime() - System.currentTimeMillis()) + " which is not less than " +
                TimeUnit.SECONDS.toMillis(minimumExpiryTimeSeconds) + " (minimumExpireTime)",
                listener.attributes.getExpirationTime() <= System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(minimumExpiryTimeSeconds));
    }

    /**
     * Check if verifying cache entries using HEAD requests work properly
     * @throws IOException
     */
    @Test
    public void testCheckUsingHead() throws IOException {
        ICacheAccess<String, CacheEntry> cache = getCache();
        long expires = TimeUnit.DAYS.toMillis(1);
        long testStart = System.currentTimeMillis();
        cache.put("test",
                new CacheEntry("cached dummy".getBytes(StandardCharsets.UTF_8)),
                createEntryAttributes(-1 * expires, 200, testStart, "eTag--gzip") // Jetty adds --gzip to etags when compressing output
                );

        tileServer.stubFor(
                WireMock.get(WireMock.urlEqualTo("/test"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Expires", TestUtils.getHTTPDate(testStart + expires))
                        .withHeader("Last-Modified", Long.toString(testStart))
                        .withHeader("ETag", "eTag") // Jetty adds "--gzip" suffix for compressed content
                        .withBody("mock entry")
                        )
                );
        tileServer.stubFor(
                WireMock.head(WireMock.urlEqualTo("/test"))
                .willReturn(WireMock.aResponse()
                        .withHeader("Expires", TestUtils.getHTTPDate(testStart + expires))
                        .withHeader("Last-Modified", Long.toString(testStart))
                        .withHeader("ETag", "eTag--gzip") // but doesn't add to uncompressed
                        )
                );

        TestCachedTileLoaderJob job = new TestCachedTileLoaderJob(tileServer.url("/test"), "test");
        Listener listener = submitJob(job, false); // cache entry is expired, no need to force refetch
        tileServer.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/test")));
        assertArrayEquals("mock entry".getBytes(StandardCharsets.UTF_8), listener.data);

        // cache entry should be retrieved from cache
        listener = submitJob(job, false);
        tileServer.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/test")));
        assertArrayEquals("mock entry".getBytes(StandardCharsets.UTF_8), listener.data);

        // invalidate entry in cache
        ICacheElement<String, CacheEntry> cacheEntry = cache.getCacheElement("test");
        CacheEntryAttributes attributes = (CacheEntryAttributes)cacheEntry.getElementAttributes();
        attributes.setExpirationTime(testStart - TimeUnit.DAYS.toMillis(1));
        cache.put("test", cacheEntry.getVal(), attributes);

        // because cache entry is invalid - HEAD request shall be made
        tileServer.verify(0, WireMock.headRequestedFor(WireMock.urlEqualTo("/test"))); // no head requests were made until now
        listener = submitJob(job, false);
        tileServer.verify(1, WireMock.headRequestedFor(WireMock.urlEqualTo("/test"))); // verify head requests were made
        tileServer.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/test"))); // verify no more get requests were made
        assertArrayEquals("mock entry".getBytes(StandardCharsets.UTF_8), listener.data);
        assertTrue(listener.attributes.getExpirationTime() >= testStart + expires);

        // cache entry should be retrieved from cache
        listener = submitJob(job, false); // cache entry is expired, no need to force refetch
        tileServer.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/test")));
        tileServer.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/test")));
        assertArrayEquals("mock entry".getBytes(StandardCharsets.UTF_8), listener.data);
    }

    /**
     * Check if server returns 304 - it will update cache attributes and not ask again for it
     * @throws IOException
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

        tileServer.stubFor(
                WireMock.get(WireMock.urlEqualTo("/test"))
                .willReturn(WireMock.status(304)
                        .withHeader("Expires", TestUtils.getHTTPDate(testStart + expires))
                        .withHeader("Last-Modified", Long.toString(testStart))
                        .withHeader("ETag", "eTag")
                        )
                );

        TestCachedTileLoaderJob job = new TestCachedTileLoaderJob(tileServer.url("/test"), "test");
        Listener listener = submitJob(job, false);
        tileServer.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/test")));
        assertArrayEquals("cached dummy".getBytes(StandardCharsets.UTF_8), listener.data);
        assertTrue(testStart + expires <= listener.attributes.getExpirationTime());
        listener = submitJob(job, false);
        tileServer.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/test"))); // no more requests were made
    }

    private void createHeadGetStub(UrlPattern url, long expires, long lastModified, String eTag, String body) {
        tileServer.stubFor(
                WireMock.get(url)
                .willReturn(WireMock.aResponse()
                        .withHeader("Expires", TestUtils.getHTTPDate(lastModified + expires))
                        .withHeader("Last-Modified", Long.toString(lastModified))
                        .withHeader("ETag", eTag)
                        .withBody(body)
                        )
                );
        tileServer.stubFor(
                WireMock.head(url)
                .willReturn(WireMock.aResponse()
                        .withHeader("Expires", TestUtils.getHTTPDate(lastModified + expires))
                        .withHeader("Last-Modified", Long.toString(lastModified))
                        .withHeader("ETag", eTag)
                        )
                );
    }

    private CacheEntryAttributes createEntryAttributes(long maxAge, int responseCode, String eTag) {
        long validTo = maxAge + System.currentTimeMillis();
        return createEntryAttributes(maxAge, responseCode, validTo, eTag);
    }

    private CacheEntryAttributes createEntryAttributes(long expirationTime, int responseCode, long lastModification, String eTag) {
        CacheEntryAttributes entryAttributes = new CacheEntryAttributes();
        entryAttributes.setExpirationTime(lastModification + expirationTime);
        entryAttributes.setResponseCode(responseCode);
        entryAttributes.setLastModification(lastModification);
        entryAttributes.setEtag(eTag);
        return entryAttributes;
    }

    private static TestCachedTileLoaderJob getStatusLoaderJob(int responseCode)  {
        return new TestCachedTileLoaderJob("http://httpstat.us/" + responseCode, "key_" + responseCode);
    }

    private static ICacheAccess<String, CacheEntry> getCache() {
        return JCSCacheManager.getCache("test");
    }
}
