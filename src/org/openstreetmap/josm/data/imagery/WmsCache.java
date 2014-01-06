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
import java.text.SimpleDateFormat;
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
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.imagery.types.EntryType;
import org.openstreetmap.josm.data.imagery.types.ProjectionType;
import org.openstreetmap.josm.data.imagery.types.WmsCacheType;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.tools.Utils;



public class WmsCache {
    //TODO Property for maximum cache size
    //TODO Property for maximum age of tile, automatically remove old tiles
    //TODO Measure time for partially loading from cache, compare with time to download tile. If slower, disable partial cache
    //TODO Do loading from partial cache and downloading at the same time, don't wait for partical cache to load

    private static final StringProperty PROP_CACHE_PATH = new StringProperty("imagery.wms-cache.path", "wms");
    private static final String INDEX_FILENAME = "index.xml";
    private static final String LAYERS_INDEX_FILENAME = "layers.properties";

    private static class CacheEntry {
        final double pixelPerDegree;
        final double east;
        final double north;
        final ProjectionBounds bounds;
        final String filename;

        long lastUsed;
        long lastModified;

        CacheEntry(double pixelPerDegree, double east, double north, int tileSize, String filename) {
            this.pixelPerDegree = pixelPerDegree;
            this.east = east;
            this.north = north;
            this.bounds = new ProjectionBounds(east, north, east + tileSize / pixelPerDegree, north + tileSize / pixelPerDegree);
            this.filename = filename;
        }
    }

    private static class ProjectionEntries {
        final String projection;
        final String cacheDirectory;
        final List<CacheEntry> entries = new ArrayList<WmsCache.CacheEntry>();

        ProjectionEntries(String projection, String cacheDirectory) {
            this.projection = projection;
            this.cacheDirectory = cacheDirectory;
        }
    }

    private final Map<String, ProjectionEntries> entries = new HashMap<String, ProjectionEntries>();
    private final File cacheDir;
    private final int tileSize; // Should be always 500
    private int totalFileSize;
    private boolean totalFileSizeDirty; // Some file was missing - size needs to be recalculated
    // No need for hashCode/equals on CacheEntry, object identity is enough. Comparing by values can lead to error - CacheEntry for wrong projection could be found
    private Map<CacheEntry, SoftReference<BufferedImage>> memoryCache = new HashMap<WmsCache.CacheEntry, SoftReference<BufferedImage>>();
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
        globalCacheDir.mkdirs();
        cacheDir = new File(globalCacheDir, getCacheDirectory(url));
        cacheDir.mkdirs();
        this.tileSize = tileSize;
    }

    private String getCacheDirectory(String url) {
        String cacheDirName = null;
        InputStream fis = null;
        OutputStream fos = null;
        try {
            Properties layersIndex = new Properties();
            File layerIndexFile = new File(cacheDirPath(), LAYERS_INDEX_FILENAME);
            try {
                fis = new FileInputStream(layerIndexFile);
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
                try {
                    fos = new FileOutputStream(layerIndexFile);
                    layersIndex.store(fos, "");
                } catch (IOException e) {
                    Main.error("Unable to save layer index for wms cache");
                    Main.error(e);
                }
            }
        } finally {
            Utils.close(fos);
            Utils.close(fis);
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
            WmsCacheType cacheEntries = (WmsCacheType)unmarshaller.unmarshal(new FileInputStream(indexFile));
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

        Set<String> usedProjections = new HashSet<String>();

        for (ProjectionEntries projectionEntries: entries.values()) {

            usedProjections.add(projectionEntries.cacheDirectory);

            File projectionDir = new File(cacheDir, projectionEntries.cacheDirectory);
            if (projectionDir.exists()) {
                Set<String> referencedFiles = new HashSet<String>();

                for (CacheEntry ce: projectionEntries.entries) {
                    referencedFiles.add(ce.filename);
                }

                for (File file: projectionDir.listFiles()) {
                    if (!referencedFiles.contains(file.getName())) {
                        file.delete();
                    }
                }
            }
        }

        for (File projectionDir: cacheDir.listFiles()) {
            if (projectionDir.isDirectory() && !usedProjections.contains(projectionDir.getName())) {
                Utils.deleteDirectory(projectionDir);
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
            marshaller.marshal(index, new FileOutputStream(new File(cacheDir, INDEX_FILENAME)));
        } catch (Exception e) {
            Main.error("Failed to save wms-cache file");
            Main.error(e);
        }
    }

    private File getImageFile(ProjectionEntries projection, CacheEntry entry) {
        return new File(cacheDir, projection.cacheDirectory + "/" + entry.filename);
    }


    private BufferedImage loadImage(ProjectionEntries projectionEntries, CacheEntry entry) throws IOException {

        synchronized (this) {
            entry.lastUsed = System.currentTimeMillis();

            SoftReference<BufferedImage> memCache = memoryCache.get(entry);
            if (memCache != null) {
                BufferedImage result = memCache.get();
                if (result != null)
                    return result;
            }
        }

        try {
            // Reading can't be in synchronized section, it's too slow
            BufferedImage result = ImageIO.read(getImageFile(projectionEntries, entry));
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
            if (entry.pixelPerDegree == pixelPerDegree && entry.east == east && entry.north == north)
                return entry;
        }
        return null;
    }

    public synchronized boolean hasExactMatch(Projection projection, double pixelPerDegree, double east, double north) {
        ProjectionEntries projectionEntries = getProjectionEntries(projection);
        CacheEntry entry = findEntry(projectionEntries, pixelPerDegree, east, north);
        return (entry != null);
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
                return loadImage(projectionEntries, entry);
            } catch (IOException e) {
                Main.error("Unable to load file from wms cache");
                Main.error(e);
                return null;
            }
        }
        return null;
    }

    public  BufferedImage getPartialMatch(Projection projection, double pixelPerDegree, double east, double north) {
        ProjectionEntries projectionEntries;
        List<CacheEntry> matches;
        synchronized (this) {
            matches = new ArrayList<WmsCache.CacheEntry>();

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

        //TODO Use alpha layer only when enabled on wms layer
        BufferedImage result = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = result.createGraphics();


        boolean drawAtLeastOnce = false;
        Map<CacheEntry, SoftReference<BufferedImage>> localCache = new HashMap<WmsCache.CacheEntry, SoftReference<BufferedImage>>();
        for (CacheEntry ce: matches) {
            BufferedImage img;
            try {
                img = loadImage(projectionEntries, ce);
                localCache.put(ce, new SoftReference<BufferedImage>(img));
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

        String zoom = NavigatableComponent.METRIC_SOM.getDistText(ll1.greatCircleDistance(ll2));
        String extension;
        if ("image/jpeg".equals(mimeType) || "image/jpg".equals(mimeType)) {
            extension = "jpg";
        } else if ("image/png".equals(mimeType)) {
            extension = "png";
        } else if ("image/gif".equals(mimeType)) {
            extension = "gif";
        } else {
            extension = "dat";
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
     * @param imageData
     * @param projection
     * @param pixelPerDegree
     * @param east
     * @param north
     * @throws IOException
     */
    public synchronized void saveToCache(BufferedImage img, InputStream imageData, Projection projection, double pixelPerDegree, double east, double north) throws IOException {
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
            entry = new CacheEntry(pixelPerDegree, east, north, tileSize,generateFileName(projectionEntries, pixelPerDegree, projection, east, north, mimeType));
            entry.lastUsed = System.currentTimeMillis();
            entry.lastModified = entry.lastUsed;
            projectionEntries.entries.add(entry);
            imageFile = getImageFile(projectionEntries, entry);
        } else {
            imageFile = getImageFile(projectionEntries, entry);
            totalFileSize -= imageFile.length();
        }

        imageFile.getParentFile().mkdirs();

        if (img != null) {
            BufferedImage copy = new BufferedImage(tileSize, tileSize, img.getType());
            copy.createGraphics().drawImage(img, 0, 0, tileSize, tileSize, 0, img.getHeight() - tileSize, tileSize, img.getHeight(), null);
            ImageIO.write(copy, "png", imageFile);
            totalFileSize += imageFile.length();
        } else {
            OutputStream os = new BufferedOutputStream(new FileOutputStream(imageFile));
            try {
                totalFileSize += Utils.copyStream(imageData, os);
            } finally {
                Utils.close(os);
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
                    file.delete();
                    it.remove();
                }
            }
        }
    }

    public static String printDate(Calendar c) {
        return (new SimpleDateFormat("yyyy-MM-dd")).format(c.getTime());
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
