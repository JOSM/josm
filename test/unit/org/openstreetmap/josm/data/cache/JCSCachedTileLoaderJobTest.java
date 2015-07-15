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

public class JCSCachedTileLoaderJobTest {
    private static class TestCachedTileLoaderJob extends JCSCachedTileLoaderJob<String, CacheEntry> {

        private int responseCode;

        public TestCachedTileLoaderJob(int responseCode) throws IOException {
            super(getCache(), 30000, 30000, null);
            this.responseCode = responseCode;
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
                return new URL("http://httpstat.us/" + Integer.toString(responseCode));
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
        private CacheEntry data;
        private CacheEntryAttributes attributes;
        private LoadResult result;
        private boolean ready;

        @Override
        public synchronized void loadingFinished(CacheEntry data, CacheEntryAttributes attributes, LoadResult result) {
            this.data = data;
            this.attributes = attributes;
            this.result = result;
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
        testStatusCode(200);
        // can't test for 3xx, as httpstat.us redirects finally to 200 page
        testStatusCode(401);
        testStatusCode(402);
        testStatusCode(403);
        testStatusCode(404);
        testStatusCode(405);
        testStatusCode(500);
        testStatusCode(501);
        testStatusCode(502);
    }

    public void testStatusCode(int responseCode) throws Exception {
        TestCachedTileLoaderJob job = new TestCachedTileLoaderJob(responseCode);
        Listener listener = new Listener();
        job.submit(listener, true);
        synchronized (listener) {
            if (!listener.ready) {
                listener.wait();
            }
        }
        assertEquals(responseCode, listener.attributes.getResponseCode());

    }
}

