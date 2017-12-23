// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;

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

    /**
     * Test that error message sent by Tomcat can be parsed.
     */
    @Test
    public void testTomcatErrorMessage() {
        Matcher m = JCSCachedTileLoaderJob.TOMCAT_ERR_MESSAGE.matcher(
            "<html><head><title>Apache Tomcat/DGFiP - Rapport d''erreur</title><style><!--"+
                "H1 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:22px;} "+
                "H2 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:16px;} "+
                "H3 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:14px;} "+
                "BODY {font-family:Tahoma,Arial,sans-serif;color:black;background-color:white;} "+
                "B {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;} "+
                "P {font-family:Tahoma,Arial,sans-serif;background:white;color:black;font-size:12px;}"+
                "A {color : black;}A.name {color : black;}HR {color : #525D76;}"+
            "--></style> </head><body><h1>Etat HTTP 400 - La commune demandée n'existe pas ou n'est pas accessible.</h1>"+
            "<HR size=\"1\" noshade=\"noshade\">"+
            "<p><b>type</b> Rapport d''état</p><p><b>message</b> <u>La commune demandée n'existe pas ou n'est pas accessible.</u></p>"+
            "<p><b>description</b> <u>La requête envoyée par le client était syntaxiquement incorrecte.</u></p>"+
            "<HR size=\"1\" noshade=\"noshade\"><h3>Apache Tomcat/DGFiP</h3></body></html>");
        assertTrue(m.matches());
        assertEquals("La commune demandée n'existe pas ou n'est pas accessible.", m.group(1));
    }
}
