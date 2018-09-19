// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.jcs.access.behavior.ICacheAccess;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.imagery.TMSCachedTileLoader;
import org.openstreetmap.josm.data.imagery.TileJobOptions;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Simple tests for ThreadPoolExecutor / HostLimitQueue veryfing, that this pair works OK
 * @author Wiktor Niesiobedzki
 */
public class HostLimitQueueTest {
    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().timeout(20 * 1000);


    /**
     * Mock class for tests
     */
    static class Task extends JCSCachedTileLoaderJob<String, CacheEntry> {
        private URL url;
        private AtomicInteger counter;

        Task(ICacheAccess<String, CacheEntry> cache, URL url, AtomicInteger counter) {
            super(cache, new TileJobOptions(1, 1, null, 10));
            this.url = url;
            this.counter = counter;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Logging.trace(e);
            } finally {
                this.counter.incrementAndGet();
                executionFinished();
            }
        }

        @Override
        public String getCacheKey() {
            return "";
        }

        @Override
        public URL getUrl() throws IOException {
            return this.url;
       }

        @Override
        protected CacheEntry createCacheEntry(byte[] content) {
            return null;
        }
    }

    /**
     * Check if single threaded execution works properly
     * @throws Exception in case of error
     */
    @Test
    public void testSingleThreadPerHost() throws Exception {
        ThreadPoolExecutor tpe = TMSCachedTileLoader.getNewThreadPoolExecutor("test-%d", 3, 1);
        ICacheAccess<String, CacheEntry> cache = JCSCacheManager.getCache("test", 3, 0, "");
        AtomicInteger counter = new AtomicInteger(0);
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            tpe.execute(new Task(cache, new URL("http://localhost/"+i), counter));
        }
        tpe.shutdown();
        tpe.awaitTermination(15, TimeUnit.SECONDS); // at most it should take ~10 seconds, so after 15 it's already failed
        long duration = System.currentTimeMillis() - start;
        // check that all tasks were executed
        assertEquals(10, counter.get());
        // although there are 3 threads, we can make only 1 parallel call to localhost
        // first three jobs will be not limited, as they spawn the thread
        // so it should take ~8 seconds to finish
        // if it's shorter, it means that host limit does not work
        assertTrue("Expected duration between 8 and 11 seconds not met. Actual duration: " + (duration /1000),
                duration < 11*1000 & duration > 8*1000);
    }

    /**
     * Check if two threaded execution work properly
     * @throws Exception in case of error
     */
    @Test
    public void testMultipleThreadPerHost() throws Exception {
        ThreadPoolExecutor tpe = TMSCachedTileLoader.getNewThreadPoolExecutor("test-%d", 3, 2);
        ICacheAccess<String, CacheEntry> cache = JCSCacheManager.getCache("test", 3, 0, "");
        AtomicInteger counter = new AtomicInteger(0);
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            tpe.execute(new Task(cache, new URL("http://hostlocal/"+i), counter));
        }
        tpe.shutdown();
        tpe.awaitTermination(15, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - start;
        // check that all tasks were executed
        assertEquals(10, counter.get());
        // although there are 3 threads, we can make only 2 parallel call to localhost
        // so it should take ~5 seconds to finish
        // if it's shorter, it means that host limit does not work
        assertTrue("Expected duration between 4 and 6 seconds not met. Actual duration: " + (duration /1000),
                duration < 6*1000 & duration > 4*1000);
    }

    /**
     * Check two hosts
     * @throws Exception in case of error
     */
    @Test
    public void testTwoHosts() throws Exception {
        ThreadPoolExecutor tpe = TMSCachedTileLoader.getNewThreadPoolExecutor("test-%d", 3, 1);
        ICacheAccess<String, CacheEntry> cache = JCSCacheManager.getCache("test", 3, 0, "");
        AtomicInteger counter = new AtomicInteger(0);
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            String url = (i % 2 == 0) ? "http://localhost" : "http://hostlocal";
            tpe.execute(new Task(cache, new URL(url+i), counter));
        }
        tpe.shutdown();
        tpe.awaitTermination(15, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - start;
        // check that all tasks were executed
        assertEquals(10, counter.get());
        // although there are 3 threads, we can make only 1 parallel per host, and we have 2 hosts
        // so it should take ~5 seconds to finish
        // if it's shorter, it means that host limit does not work
        assertTrue("Expected duration between 4 and 6 seconds not met. Actual duration: " + (duration /1000),
                duration < 6*1000 & duration > 4*1000);
    }
}
