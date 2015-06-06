// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.SystemOfMeasurement;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.types.EntryType;
import org.openstreetmap.josm.data.imagery.types.ProjectionType;
import org.openstreetmap.josm.data.imagery.types.WmsCacheType;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.layer.WMSLayer;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.date.DateUtils;

public class WmsCache {
    //TODO Property for maximum cache size
    //TODO Property for maximum age of tile, automatically remove old tiles
    //TODO Measure time for partially loading from cache, compare with time to download tile. If slower, disable partial cache
    //TODO Do loading from partial cache and downloading at the same time, don't wait for partial cache to load

    private static final StringProperty PROP_CACHE_PATH = new StringProperty("imagery.wms-cache.path", "wms");
    private static final String INDEX_FILENAME = "index.xml";
    private static final String LAYERS_INDEX_FILENAME = "layers.properties";

    private static class CacheEntry {
        private final double pixelPerDegree;
        private final double east;
        private final double north;
        private final ProjectionBounds bounds;
        private final String filename;

        private long lastUsed;
        private long lastModified;

        CacheEntry(double pixelPerDegree, double east, double north, int tileSize, String filename) {
            this.pixelPerDegree = pixelPerDegree;
            this.east = east;
            this.north = north;
            this.bounds = new ProjectionBounds(east, north, east + tileSize / pixelPerDegree, north + tileSize / pixelPerDegree);
            this.filename = filename;
        }

        @Override
        public String toString() {
            return "CacheEntry [pixelPerDegree=" + pixelPerDegree + ", east=" + east + ", north=" + north + ", bounds="
                    + bounds + ", filename=" + filename + ", lastUsed=" + lastUsed + ", lastModified=" + lastModified
                    + "]";
        }
    }

    private static class ProjectionEntries {
        private final String projection;
        private final String cacheDirectory;
        private final List<CacheEntry> entries = new ArrayList<>();

        ProjectionEntries(String projection, String cacheDirectory) {
            this.projection = projection;
            this.cacheDirectory = cacheDirectory;
        }
    }

    private final Map<String, ProjectionEntries> entries = new HashMap<>();
    private final File cacheDir;
    private final int tileSize; // Should be always 500
    private int totalFileSize;
    private boolean totalFileSizeDirty; // Some file was missing - size needs to be recalculated
    // No need for hashCode/equals on CacheEntry, object identity is enough. Comparing by values can lead to error - CacheEntry for wrong projection could be found
    private Map<CacheEntry, SoftReference<BufferedImage>> memoryCache = new HashMap<>();
    private Set<ProjectionBounds> areaToCache;

    protected String cacheDirPath() {
        String cPath = PROP_CACHE_PATH.get();
        if (!(new File(cPath).isAbsolute())) {
            cPath = Main.pref.getCacheDirectory() + File.separator + cPath;
        }
        return cPath;
    }

    public WmsCache(String url, int tileSize) {
        File globalCacheDir = new File(cacheDirPath());
        if (!globalCacheDir.mkdirs()) {
            Main.warn("Unable to create global cache directory: "+globalCacheDir.getAbsolutePath());
        }
        cacheDir = new File(globalCacheDir, getCacheDirectory(url));
        cacheDir.mkdirs();
        this.tileSize = tileSize;
    }

    private String getCacheDirectory(String url) {
        String cacheDirName = null;
        Properties layersIndex = new Properties();
        File layerIndexFile = new File(cacheDirPath(), LAYERS_INDEX_FILENAME);
        try (InputStream fis = new FileInputStream(layerIndexFile)) {
            layersIndex.load(fis);
        } catch (FileNotFoundException e) {
            Main.error("Unable to load layers index for wms cache (file " + layerIndexFile + " not found)");
        } catch (IOException e) {
            Main.error("Unable to load layers index for wms cache");
            Main.error(e);
        }

        for (Object propKey: layersIndex.keySet()) {
            String s = (String)propKey;
            if (url.equals(layersIndex.getProperty(s))) {
                cacheDirName = s;
                break;
            }
        }

        if (cacheDirName == null) {
            int counter = 0;
            while (true) {
                counter++;
                if (!layersIndex.keySet().contains(String.valueOf(counter))) {
                    break;
                }
            }
            cacheDirName = String.valueOf(counter);
            layersIndex.setProperty(cacheDirName, url);
            try (OutputStream fos = new FileOutputStream(layerIndexFile)) {
                layersIndex.store(fos, "");
            } catch (IOException e) {
                Main.error("Unable to save layer index for wms cache");
                Main.error(e);
            }
        }

        return cacheDirName;
    }

    private ProjectionEntries getProjectionEntries(Projection projection) {
        return getProjectionEntries(projection.toCode(), projection.getCacheDirectoryName());
    }

    private ProjectionEntries getProjectionEntries(String projection, String cacheDirectory) {
        ProjectionEntries result = entries.get(projection);
        if (result == null) {
            result = new ProjectionEntries(projection, cacheDirectory);
            entries.put(projection, result);
        }

        return result;
    }

    public synchronized void loadIndex() {
        File indexFile = new File(cacheDir, INDEX_FILENAME);
        try {
            JAXBContext context = JAXBContext.newInstance(
                    WmsCacheType.class.getPackage().getName(),
                    WmsCacheType.class.getClassLoader());
            Unmarshaller unmarshaller = context.createUnmarshaller();
            WmsCacheType cacheEntries;
            try (InputStream is = new FileInputStream(indexFile)) {
                cacheEntries = (WmsCacheType)unmarshaller.unmarshal(is);
            }
            totalFileSize = cacheEntries.getTotalFileSize();
            if (cacheEntries.getTileSize() != tileSize) {
                Main.info("Cache created with different tileSize, cache will be discarded");
                return;
            }
            for (ProjectionType projectionType: cacheEntries.getProjection()) {
                ProjectionEntries projection = getProjectionEntries(projectionType.getName(), projectionType.getCacheDirectory());
                for (EntryType entry: projectionType.getEntry()) {
                    CacheEntry ce = new CacheEntry(entry.getPixelPerDegree(), entry.getEast(), entry.getNorth(), tileSize, entry.getFilename());
                    ce.lastUsed = entry.getLastUsed().getTimeInMillis();
                    ce.lastModified = entry.getLastModified().getTimeInMillis();
                    projection.entries.add(ce);
                }
            }
        } catch (Exception e) {
            if (indexFile.exists()) {
                Main.error(e);
                Main.info("Unable to load index for wms-cache, new file will be created");
            } else {
                Main.info("Index for wms-cache doesn't exist, new file will be created");
            }
        }

        removeNonReferencedFiles();
    }

    private void removeNonReferencedFiles() {

        Set<String> usedProjections = new HashSet<>();

        for (ProjectionEntries projectionEntries: entries.values()) {

            usedProjections.add(projectionEntries.cacheDirectory);

            File projectionDir = new File(cacheDir, projectionEntries.cacheDirectory);
            if (projectionDir.exists()) {
                Set<String> referencedFiles = new HashSet<>();

                for (CacheEntry ce: projectionEntries.entries) {
                    referencedFiles.add(ce.filename);
                }

                File[] files = projectionDir.listFiles();
                if (files != null) {
                    for (File file: files) {
                        if (!referencedFiles.contains(file.getName()) && !file.delete()) {
                            Main.warn("Unable to delete file: "+file.getAbsolutePath());
                        }
                    }
                }
            }
        }

        File[] files = cacheDir.listFiles();
        if (files != null) {
            for (File projectionDir: files) {
                if (projectionDir.isDirectory() && !usedProjections.contains(projectionDir.getName())) {
                    Utils.deleteDirectory(projectionDir);
                }
            }
        }
    }

    private int calculateTotalFileSize() {
        int result = 0;
        for (ProjectionEntries projectionEntries: entries.values()) {
            Iterator<CacheEntry> it = projectionEntries.entries.iterator();
            while (it.hasNext()) {
                CacheEntry entry = it.next();
                File imageFile = getImageFile(projectionEntries, entry);
                if (!imageFile.exists()) {
                    it.remove();
                } else {
                    result += imageFile.length();
                }
            }
        }
        return result;
    }

    public synchronized void saveIndex() {
        WmsCacheType index = new WmsCacheType();

        if (totalFileSizeDirty) {
            totalFileSize = calculateTotalFileSize();
        }

        index.setTileSize(tileSize);
        index.setTotalFileSize(totalFileSize);
        for (ProjectionEntries projectionEntries: entries.values()) {
            if (!projectionEntries.entries.isEmpty()) {
                ProjectionType projectionType = new ProjectionType();
                projectionType.setName(projectionEntries.projection);
                projectionType.setCacheDirectory(projectionEntries.cacheDirectory);
                index.getProjection().add(projectionType);
                for (CacheEntry ce: projectionEntries.entries) {
                    EntryType entry = new EntryType();
                    entry.setPixelPerDegree(ce.pixelPerDegree);
                    entry.setEast(ce.east);
                    entry.setNorth(ce.north);
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(ce.lastUsed);
                    entry.setLastUsed(c);
                    c = Calendar.getInstance();
                    c.setTimeInMillis(ce.lastModified);
                    entry.setLastModified(c);
                    entry.setFilename(ce.filename);
                    projectionType.getEntry().add(entry);
                }
            }
        }
        try {
            JAXBContext context = JAXBContext.newInstance(
                    WmsCacheType.class.getPackage().getName(),
                    WmsCacheType.class.getClassLoader());
            Marshaller marshaller = context.createMarshaller();
            try (OutputStream fos = new FileOutputStream(new File(cacheDir, INDEX_FILENAME))) {
                marshaller.marshal(index, fos);
            }
        } catch (Exception e) {
            Main.error("Failed to save wms-cache file");
            Main.error(e);
        }
    }

    private File getImageFile(ProjectionEntries projection, CacheEntry entry) {
        return new File(cacheDir, projection.cacheDirectory + "/" + entry.filename);
    }

    private BufferedImage loadImage(ProjectionEntries projectionEntries, CacheEntry entry, boolean enforceTransparency) throws IOException {
        synchronized (this) {
            entry.lastUsed = System.currentTimeMillis();

            SoftReference<BufferedImage> memCache = memoryCache.get(entry);
            if (memCache != null) {
                BufferedImage result = memCache.get();
                if (result != null) {
                    if (enforceTransparency == ImageProvider.isTransparencyForced(result)) {
                        return result;
                    } else if (Main.isDebugEnabled()) {
                        Main.debug("Skipping "+entry+" from memory cache (transparency enforcement)");
                    }
                }
            }
        }

        try {
            // Reading can't be in synchronized section, it's too slow
            BufferedImage result = ImageProvider.read(getImageFile(projectionEntries, entry), true, enforceTransparency);
            synchronized (this) {
                if (result == null) {
                    projectionEntries.entries.remove(entry);
                    totalFileSizeDirty = true;
                }
                return result;
            }
        } catch (IOException e) {
            synchronized (this) {
                projectionEntries.entries.remove(entry);
                totalFileSizeDirty = true;
                throw e;
            }
        }
    }

    private CacheEntry findEntry(ProjectionEntries projectionEntries, double pixelPerDegree, double east, double north) {
        for (CacheEntry entry: projectionEntries.entries) {
            if (Utils.equalsEpsilon(entry.pixelPerDegree, pixelPerDegree)
                    && Utils.equalsEpsilon(entry.east, east) && Utils.equalsEpsilon(entry.north, north))
                return entry;
        }
        return null;
    }

    public synchronized boolean hasExactMatch(Projection projection, double pixelPerDegree, double east, double north) {
        ProjectionEntries projectionEntries = getProjectionEntries(projection);
        return findEntry(projectionEntries, pixelPerDegree, east, north) != null;
    }

    public BufferedImage getExactMatch(Projection projection, double pixelPerDegree, double east, double north) {
        CacheEntry entry = null;
        ProjectionEntries projectionEntries = null;
        synchronized (this) {
            projectionEntries = getProjectionEntries(projection);
            entry = findEntry(projectionEntries, pixelPerDegree, east, north);
        }
        if (entry != null) {
            try {
                return loadImage(projectionEntries, entry, WMSLayer.PROP_ALPHA_CHANNEL.get());
            } catch (IOException e) {
                Main.error("Unable to load file from wms cache");
                Main.error(e);
                return null;
            }
        }
        return null;
    }

    public BufferedImage getPartialMatch(Projection projection, double pixelPerDegree, double east, double north) {
        ProjectionEntries projectionEntries;
        List<CacheEntry> matches;
        synchronized (this) {
            matches = new ArrayList<>();

            double minPPD = pixelPerDegree / 5;
            double maxPPD = pixelPerDegree * 5;
            projectionEntries = getProjectionEntries(projection);

            double size2 = tileSize / pixelPerDegree;
            double border = tileSize * 0.01; // Make sure not to load neighboring tiles that intersects this tile only slightly
            ProjectionBounds bounds = new ProjectionBounds(east + border, north + border,
                    east + size2 - border, north + size2 - border);

            //TODO Do not load tile if it is completely overlapped by other tile with better ppd
            for (CacheEntry entry: projectionEntries.entries) {
                if (entry.pixelPerDegree >= minPPD && entry.pixelPerDegree <= maxPPD && entry.bounds.intersects(bounds)) {
                    entry.lastUsed = System.currentTimeMillis();
                    matches.add(entry);
                }
            }

            if (matches.isEmpty())
                return null;

            Collections.sort(matches, new Comparator<CacheEntry>() {
                @Override
                public int compare(CacheEntry o1, CacheEntry o2) {
                    return Double.compare(o2.pixelPerDegree, o1.pixelPerDegree);
                }
            });
        }

        // Use alpha layer only when enabled on wms layer
        boolean alpha = WMSLayer.PROP_ALPHA_CHANNEL.get();
        BufferedImage result = new BufferedImage(tileSize, tileSize,
                alpha ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = result.createGraphics();

        boolean drawAtLeastOnce = false;
        Map<CacheEntry, SoftReference<BufferedImage>> localCache = new HashMap<>();
        for (CacheEntry ce: matches) {
            BufferedImage img;
            try {
                // Enforce transparency only when alpha enabled on wms layer too
                img = loadImage(projectionEntries, ce, alpha);
                localCache.put(ce, new SoftReference<>(img));
            } catch (IOException e) {
                continue;
            }

            drawAtLeastOnce = true;

            int xDiff = (int)((ce.east - east) * pixelPerDegree);
            int yDiff = (int)((ce.north - north) * pixelPerDegree);
            int size = (int)(pixelPerDegree / ce.pixelPerDegree  * tileSize);

            int x = xDiff;
            int y = -size + tileSize - yDiff;

            g.drawImage(img, x, y, size, size, null);
        }

        if (drawAtLeastOnce) {
            synchronized (this) {
                memoryCache.putAll(localCache);
            }
            return result;
        } else
            return null;
    }

    private String generateFileName(ProjectionEntries projectionEntries, double pixelPerDegree, Projection projection, double east, double north, String mimeType) {
        LatLon ll1 = projection.eastNorth2latlon(new EastNorth(east, north));
        LatLon ll2 = projection.eastNorth2latlon(new EastNorth(east + 100 / pixelPerDegree, north));
        LatLon ll3 = projection.eastNorth2latlon(new EastNorth(east + tileSize / pixelPerDegree, north + tileSize / pixelPerDegree));

        double deltaLat = Math.abs(ll3.lat() - ll1.lat());
        double deltaLon = Math.abs(ll3.lon() - ll1.lon());
        int precisionLat = Math.max(0, -(int)Math.ceil(Math.log10(deltaLat)) + 1);
        int precisionLon = Math.max(0, -(int)Math.ceil(Math.log10(deltaLon)) + 1);

        String zoom = SystemOfMeasurement.METRIC.getDistText(ll1.greatCircleDistance(ll2));
        String extension = "dat";
        if (mimeType != null) {
            switch(mimeType) {
            case "image/jpeg":
            case "image/jpg":
                extension = "jpg";
                break;
            case "image/png":
                extension = "png";
                break;
            case "image/gif":
                extension = "gif";
                break;
            default:
                Main.warn("Unrecognized MIME type: "+mimeType);
            }
        }

        int counter = 0;
        FILENAME_LOOP:
            while (true) {
                String result = String.format("%s_%." + precisionLat + "f_%." + precisionLon +"f%s.%s", zoom, ll1.lat(), ll1.lon(), counter==0?"":"_" + counter, extension);
                for (CacheEntry entry: projectionEntries.entries) {
                    if (entry.filename.equals(result)) {
                        counter++;
                        continue FILENAME_LOOP;
                    }
                }
                return result;
            }
    }

    /**
     *
     * @param img Used only when overlapping is used, when not used, used raw from imageData
     * @param imageData input stream to raw image data
     * @param projection current projection
     * @param pixelPerDegree number of pixels per degree
     * @param east easting
     * @param north northing
     * @throws IOException if any I/O error occurs
     */
    public synchronized void saveToCache(BufferedImage img, InputStream imageData, Projection projection, double pixelPerDegree, double east, double north)
            throws IOException {
        ProjectionEntries projectionEntries = getProjectionEntries(projection);
        CacheEntry entry = findEntry(projectionEntries, pixelPerDegree, east, north);
        File imageFile;
        if (entry == null) {

            String mimeType;
            if (img != null) {
                mimeType = "image/png";
            } else {
                mimeType = URLConnection.guessContentTypeFromStream(imageData);
            }
            entry = new CacheEntry(pixelPerDegree, east, north,
                    tileSize,generateFileName(projectionEntries, pixelPerDegree, projection, east, north, mimeType));
            entry.lastUsed = System.currentTimeMillis();
            entry.lastModified = entry.lastUsed;
            projectionEntries.entries.add(entry);
            imageFile = getImageFile(projectionEntries, entry);
        } else {
            imageFile = getImageFile(projectionEntries, entry);
            totalFileSize -= imageFile.length();
        }

        if (!imageFile.getParentFile().mkdirs()) {
            Main.warn("Unable to create parent directory: "+imageFile.getParentFile().getAbsolutePath());
        }

        if (img != null) {
            BufferedImage copy = new BufferedImage(tileSize, tileSize, img.getType());
            copy.createGraphics().drawImage(img, 0, 0, tileSize, tileSize, 0, img.getHeight() - tileSize, tileSize, img.getHeight(), null);
            ImageIO.write(copy, "png", imageFile);
            totalFileSize += imageFile.length();
        } else {
            try (OutputStream os = new BufferedOutputStream(new FileOutputStream(imageFile))) {
                totalFileSize += Utils.copyStream(imageData, os);
            }
        }
    }

    public synchronized void cleanSmallFiles(int size) {
        for (ProjectionEntries projectionEntries: entries.values()) {
            Iterator<CacheEntry> it = projectionEntries.entries.iterator();
            while (it.hasNext()) {
                File file = getImageFile(projectionEntries, it.next());
                long length = file.length();
                if (length <= size) {
                    if (length == 0) {
                        totalFileSizeDirty = true; // File probably doesn't exist
                    }
                    totalFileSize -= size;
                    if (!file.delete()) {
                        Main.warn("Unable to delete file: "+file.getAbsolutePath());
                    }
                    it.remove();
                }
            }
        }
    }

    public static String printDate(Calendar c) {
        return DateUtils.newIsoDateFormat().format(c.getTime());
    }

    private boolean isInsideAreaToCache(CacheEntry cacheEntry) {
        for (ProjectionBounds b: areaToCache) {
            if (cacheEntry.bounds.intersects(b))
                return true;
        }
        return false;
    }

    public synchronized void setAreaToCache(Set<ProjectionBounds> areaToCache) {
        this.areaToCache = areaToCache;
        Iterator<CacheEntry> it = memoryCache.keySet().iterator();
        while (it.hasNext()) {
            if (!isInsideAreaToCache(it.next())) {
                it.remove();
            }
        }
    }
}
