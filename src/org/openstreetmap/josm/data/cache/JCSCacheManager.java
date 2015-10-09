// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.cache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.commons.jcs.access.CacheAccess;
import org.apache.commons.jcs.auxiliary.AuxiliaryCache;
import org.apache.commons.jcs.auxiliary.AuxiliaryCacheFactory;
import org.apache.commons.jcs.auxiliary.disk.behavior.IDiskCacheAttributes;
import org.apache.commons.jcs.auxiliary.disk.indexed.IndexedDiskCacheAttributes;
import org.apache.commons.jcs.auxiliary.disk.indexed.IndexedDiskCacheFactory;
import org.apache.commons.jcs.engine.CompositeCacheAttributes;
import org.apache.commons.jcs.engine.behavior.ICompositeCacheAttributes.DiskUsagePattern;
import org.apache.commons.jcs.engine.control.CompositeCache;
import org.apache.commons.jcs.engine.control.CompositeCacheManager;
import org.apache.commons.jcs.utils.serialization.StandardSerializer;
import org.openstreetmap.gui.jmapviewer.FeatureAdapter;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.preferences.IntegerProperty;

/**
 * @author Wiktor Niesiobędzki
 *
 * Wrapper class for JCS Cache. Sets some sane environment and returns instances of cache objects.
 * Static configuration for now assumes some small LRU cache in memory and larger LRU cache on disk
 * @since 8168
 */
public final class JCSCacheManager {
    private static final Logger LOG = FeatureAdapter.getLogger(JCSCacheManager.class.getCanonicalName());

    private static volatile CompositeCacheManager cacheManager;
    private static long maxObjectTTL        = -1;
    private static final String PREFERENCE_PREFIX = "jcs.cache";
    private static final AuxiliaryCacheFactory diskCacheFactory = new IndexedDiskCacheFactory();
    private static FileLock cacheDirLock;

    /**
     * default objects to be held in memory by JCS caches (per region)
     */
    public static final IntegerProperty DEFAULT_MAX_OBJECTS_IN_MEMORY  = new IntegerProperty(PREFERENCE_PREFIX + ".max_objects_in_memory", 1000);

    private JCSCacheManager() {
        // Hide implicit public constructor for utility classes
    }

    @SuppressWarnings("resource")
    private static void initialize() throws IOException {
        File cacheDir = new File(Main.pref.getCacheDirectory(), "jcs");

        if (!cacheDir.exists() && !cacheDir.mkdirs())
            throw new IOException("Cannot access cache directory");

        File cacheDirLockPath = new File(cacheDir, ".lock");
        if (!cacheDirLockPath.exists() && !cacheDirLockPath.createNewFile()) {
            LOG.log(Level.WARNING, "Cannot create cache dir lock file");
        }
        cacheDirLock = new FileOutputStream(cacheDirLockPath).getChannel().tryLock();

        if (cacheDirLock == null)
            LOG.log(Level.WARNING, "Cannot lock cache directory. Will not use disk cache");

        // raising logging level gives ~500x performance gain
        // http://westsworld.dk/blog/2008/01/jcs-and-performance/
        final Logger jcsLog = Logger.getLogger("org.apache.commons.jcs");
        jcsLog.setLevel(Level.INFO);
        jcsLog.setUseParentHandlers(false);
        // we need a separate handler from Main's, as we downgrade LEVEL.INFO to DEBUG level
        jcsLog.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                String msg = MessageFormat.format(record.getMessage(), record.getParameters());
                if (record.getLevel().intValue() >= Level.SEVERE.intValue()) {
                    Main.error(msg);
                } else if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                    Main.warn(msg);
                    // downgrade INFO level to debug, as JCS is too verbose at INFO level
                } else if (record.getLevel().intValue() >= Level.INFO.intValue()) {
                    Main.debug(msg);
                } else {
                    Main.trace(msg);
                }
            }

            @Override
            public void flush() {
                // nothing to be done on flush
            }

            @Override
            public void close() {
                // nothing to be done on close
            }
        });

        // this could be moved to external file
        Properties props = new Properties();
        // these are default common to all cache regions
        // use of auxiliary cache and sizing of the caches is done with giving proper geCache(...) params
        props.setProperty("jcs.default.cacheattributes",                      CompositeCacheAttributes.class.getCanonicalName());
        props.setProperty("jcs.default.cacheattributes.MaxObjects",           DEFAULT_MAX_OBJECTS_IN_MEMORY.get().toString());
        props.setProperty("jcs.default.cacheattributes.UseMemoryShrinker",    "true");
        props.setProperty("jcs.default.cacheattributes.DiskUsagePatternName", "UPDATE"); // store elements on disk on put
        props.setProperty("jcs.default.elementattributes",                    CacheEntryAttributes.class.getCanonicalName());
        props.setProperty("jcs.default.elementattributes.IsEternal",          "false");
        props.setProperty("jcs.default.elementattributes.MaxLife",            Long.toString(maxObjectTTL));
        props.setProperty("jcs.default.elementattributes.IdleTime",           Long.toString(maxObjectTTL));
        props.setProperty("jcs.default.elementattributes.IsSpool",            "true");
        CompositeCacheManager cm = CompositeCacheManager.getUnconfiguredInstance();
        cm.configure(props);
        cacheManager = cm;
    }

    /**
     * Returns configured cache object for named cache region
     * @param cacheName region name
     * @return cache access object
     * @throws IOException if directory is not found
     */
    public static <K, V> CacheAccess<K, V> getCache(String cacheName) throws IOException {
        return getCache(cacheName, DEFAULT_MAX_OBJECTS_IN_MEMORY.get().intValue(), 0, null);
    }

    /**
     * Returns configured cache object with defined limits of memory cache and disk cache
     * @param cacheName         region name
     * @param maxMemoryObjects  number of objects to keep in memory
     * @param maxDiskObjects    maximum size of the objects stored on disk in kB
     * @param cachePath         path to disk cache. if null, no disk cache will be created
     * @return cache access object
     * @throws IOException if directory is not found
     */
    public static <K, V> CacheAccess<K, V> getCache(String cacheName, int maxMemoryObjects, int maxDiskObjects, String cachePath)
            throws IOException {
        if (cacheManager != null)
            return getCacheInner(cacheName, maxMemoryObjects, maxDiskObjects, cachePath);

        synchronized (JCSCacheManager.class) {
            if (cacheManager == null)
                initialize();
            return getCacheInner(cacheName, maxMemoryObjects, maxDiskObjects, cachePath);
        }
    }

    @SuppressWarnings("unchecked")
    private static <K, V> CacheAccess<K, V> getCacheInner(String cacheName, int maxMemoryObjects, int maxDiskObjects, String cachePath) {
        CompositeCache<K, V> cc = cacheManager.getCache(cacheName, getCacheAttributes(maxMemoryObjects));

        if (cachePath != null && cacheDirLock != null) {
            IDiskCacheAttributes diskAttributes = getDiskCacheAttributes(maxDiskObjects, cachePath);
            diskAttributes.setCacheName(cacheName);
            try {
                if (cc.getAuxCaches().length == 0) {
                    AuxiliaryCache<K, V> diskCache = diskCacheFactory.createCache(diskAttributes, cacheManager, null, new StandardSerializer());
                    cc.setAuxCaches(new AuxiliaryCache[]{diskCache});
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return new CacheAccess<K, V>(cc);
    }

    /**
     * Close all files to ensure, that all indexes and data are properly written
     */
    public static void shutdown() {
        // use volatile semantics to get consistent object
        CompositeCacheManager localCacheManager = cacheManager;
        if (localCacheManager != null) {
            localCacheManager.shutDown();
        }
    }

    private static IDiskCacheAttributes getDiskCacheAttributes(int maxDiskObjects, String cachePath) {
        IndexedDiskCacheAttributes ret = new IndexedDiskCacheAttributes();
        ret.setDiskLimitType(IDiskCacheAttributes.DiskLimitType.SIZE);
        ret.setMaxKeySize(maxDiskObjects);
        if (cachePath != null) {
            File path = new File(cachePath);
            if (!path.exists() && !path.mkdirs()) {
                LOG.log(Level.WARNING, "Failed to create cache path: {0}", cachePath);
            } else {
                ret.setDiskPath(path);
            }
        }
        return ret;
    }

    private static CompositeCacheAttributes getCacheAttributes(int maxMemoryElements) {
        CompositeCacheAttributes ret = new CompositeCacheAttributes();
        ret.setMaxObjects(maxMemoryElements);
        ret.setDiskUsagePattern(DiskUsagePattern.UPDATE);
        return ret;
    }
}
