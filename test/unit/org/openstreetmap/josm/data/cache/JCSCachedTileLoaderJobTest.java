// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.cache;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.jcs.access.behavior.ICacheAccess;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests for class {@link JCSCachedTileLoaderJob}.
 */
public class JCSCachedTileLoaderJobTest {

    private static class TestCachedTileLoaderJob extends JCSCachedTileLoaderJob<String, CacheEntry> {
        private String url;

        TestCachedTileLoaderJob(String url) throws IOException {
            super(getCache(), 30000, 30000, null);
            this.url = url;
        }

        private static ICacheAccess<String, CacheEntry> getCache() throws IOException {
         return JCSCacheManager.getCache("test");
        }

        @Override
        public String getCacheKey() {
            return "cachekey";
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
            return new CacheEntry("dummy".getBytes());
        }
    }

    private static class Listener implements ICachedLoaderListener {
        private CacheEntryAttributes attributes;
        private boolean ready;

        @Override
        public synchronized void loadingFinished(CacheEntry data, CacheEntryAttributes attributes, LoadResult result) {
            this.attributes = attributes;
            this.ready = true;
            this.notify();
        }
    }

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    @Test
    public void testStatusCodes() throws Exception {
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

    @Test
    public void testUnknownHost() throws Exception {
        TestCachedTileLoaderJob job = new TestCachedTileLoaderJob("http://unkownhost.unkownhost/unkown");
        Listener listener = new Listener();
        job.submit(listener, true);
        synchronized (listener) {
            if (!listener.ready) {
                listener.wait();
            }
        }
        assertEquals("java.net.UnknownHostException: unkownhost.unkownhost", listener.attributes.getErrorMessage());
    }

    private void doTestStatusCode(int responseCode) throws Exception {
        TestCachedTileLoaderJob job = getStatusLoaderJob(responseCode);
        Listener listener = new Listener();
        job.submit(listener, true);
        synchronized (listener) {
            if (!listener.ready) {
                listener.wait();
            }
        }
        assertEquals(responseCode, listener.attributes.getResponseCode());
    }

    private static TestCachedTileLoaderJob getStatusLoaderJob(int responseCode) throws IOException {
        return new TestCachedTileLoaderJob("http://httpstat.us/" + responseCode);
    }
}
