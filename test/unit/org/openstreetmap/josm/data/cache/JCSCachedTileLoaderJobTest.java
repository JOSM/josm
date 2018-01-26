// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.jcs.access.behavior.ICacheAccess;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.cache.ICachedLoaderListener.LoadResult;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link JCSCachedTileLoaderJob}.
 */
public class JCSCachedTileLoaderJobTest {

    private static class TestCachedTileLoaderJob extends JCSCachedTileLoaderJob<String, CacheEntry> {
        private String url;
        private String key;

        TestCachedTileLoaderJob(String url, String key) throws IOException {
            super(getCache(), 30000, 30000, null);

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
            return new CacheEntry("dummy".getBytes(StandardCharsets.UTF_8));
        }
    }

    private static class Listener implements ICachedLoaderListener {
        private CacheEntryAttributes attributes;
        private boolean ready;
        private LoadResult result;

        @Override
        public synchronized void loadingFinished(CacheEntry data, CacheEntryAttributes attributes, LoadResult result) {
            this.attributes = attributes;
            this.ready = true;
            this.result = result;
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
        Listener listener = new Listener();
        job.submit(listener, true);
        synchronized (listener) {
            while (!listener.ready) {
                try {
                    listener.wait();
                } catch (InterruptedException e1) {
                    // do nothing, still wait
                    Logging.trace(e1);
                }
            }
        }
        assertEquals(LoadResult.FAILURE, listener.result); // because response will be cached, and that is checked below
        assertEquals("java.net.UnknownHostException: unkownhost.unkownhost", listener.attributes.getErrorMessage());

        ICacheAccess<String, CacheEntry> cache = getCache();
        CacheEntry e = new CacheEntry(new byte[]{0, 1, 2, 3});
        CacheEntryAttributes attributes = new CacheEntryAttributes();
        attributes.setExpirationTime(2);
        cache.put(key, e, attributes);

        job = new TestCachedTileLoaderJob("http://unkownhost.unkownhost/unkown", key);
        listener = new Listener();
        job.submit(listener, true);
        synchronized (listener) {
            while (!listener.ready) {
                try {
                    listener.wait();
                } catch (InterruptedException e1) {
                    // do nothing, wait
                    Logging.trace(e1);
                }
            }
        }
        assertEquals(LoadResult.SUCCESS, listener.result);
        assertFalse(job.isCacheElementValid());
    }

    @SuppressFBWarnings(value = "WA_NOT_IN_LOOP")
    private void doTestStatusCode(int responseCode) throws IOException, InterruptedException {
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
        return new TestCachedTileLoaderJob("http://httpstat.us/" + responseCode, "key_" + responseCode);
    }

    private static ICacheAccess<String, CacheEntry> getCache() throws IOException {
        return JCSCacheManager.getCache("test");
    }
}
