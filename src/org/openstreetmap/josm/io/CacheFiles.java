// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.Utils;

/**
 * Use this class if you want to cache a lot of files that shouldn't be kept in memory. You can
 * specify how much data should be stored and after which date the files should be expired.
 * This works on a last-access basis, so files get deleted after they haven't been used for x days.
 * You can turn this off by calling setUpdateModTime(false). Files get deleted on a first-in-first-out
 * basis.
 * @author xeen
 *
 */
public class CacheFiles {
    /**
     * Common expirey dates
     */
    final static public int EXPIRE_NEVER = -1;
    final static public int EXPIRE_DAILY = 60 * 60 * 24;
    final static public int EXPIRE_WEEKLY = EXPIRE_DAILY * 7;
    final static public int EXPIRE_MONTHLY = EXPIRE_WEEKLY * 4;

    final private File dir;
    final private String ident;
    final private boolean enabled;

    private long expire;  // in seconds
    private long maxsize; // in megabytes
    private boolean updateModTime = true;

    // If the cache is full, we don't want to delete just one file
    private static final int CLEANUP_TRESHOLD = 20;
    // We don't want to clean after every file-write
    private static final int CLEANUP_INTERVAL = 5;
    // Stores how many files have been written
    private int writes = 0;

    /**
     * Creates a new cache class. The ident will be used to store the files on disk and to save
     * expire/space settings. Set plugin state to <code>true</code>.
     * @param ident cache identifier
     */
    public CacheFiles(String ident) {
        this(ident, true);
    }

    /**
     * Creates a new cache class. The ident will be used to store the files on disk and to save
     * expire/space settings.
     * @param ident cache identifier
     * @param isPlugin Whether this is a plugin or not (changes cache path)
     */
    public CacheFiles(String ident, boolean isPlugin) {
        String pref = isPlugin ?
                Main.pref.getPluginsDirectory().getPath() + File.separator + "cache" :
                Main.pref.getCacheDirectory().getPath();

        boolean dir_writeable;
        this.ident = ident;
        String cacheDir = Main.pref.get("cache." + ident + "." + "path", pref + File.separator + ident + File.separator);
        this.dir = new File(cacheDir);
        try {
            this.dir.mkdirs();
            dir_writeable = true;
        } catch(Exception e) {
            // We have no access to this directory, so don't do anything
            dir_writeable = false;
        }
        this.enabled = dir_writeable;
        this.expire = Main.pref.getLong("cache." + ident + "." + "expire", EXPIRE_DAILY);
        if(this.expire < 0) {
            this.expire = CacheFiles.EXPIRE_NEVER;
        }
        this.maxsize = Main.pref.getLong("cache." + ident + "." + "maxsize", 50);
        if(this.maxsize < 0) {
            this.maxsize = -1;
        }
    }

    /**
     * Loads the data for the given ident as an byte array. Returns null if data not available.
     * @param ident cache identifier
     * @return stored data
     */
    public byte[] getData(String ident) {
        if(!enabled) return null;
        try {
            File data = getPath(ident);
            if(!data.exists())
                return null;

            if(isExpired(data)) {
                data.delete();
                return null;
            }

            // Update last mod time so we don't expire recently used data
            if(updateModTime) {
                data.setLastModified(System.currentTimeMillis());
            }

            byte[] bytes = new byte[(int) data.length()];
            new RandomAccessFile(data, "r").readFully(bytes);
            return bytes;
        } catch (Exception e) {
            Main.warn(e);
        }
        return null;
    }

    /**
     * Writes an byte-array to disk
     * @param ident cache identifier
     * @param data data to store
     */
    public void saveData(String ident, byte[] data) {
        if(!enabled) return;
        try {
            File f = getPath(ident);
            if (f.exists()) {
                f.delete();
            }
            // rws also updates the file meta-data, i.e. last mod time
            RandomAccessFile raf = new RandomAccessFile(f, "rws");
            try {
                raf.write(data);
            } finally {
                Utils.close(raf);
            }
        } catch (Exception e) {
            Main.warn(e);
        }

        writes++;
        checkCleanUp();
    }

    /**
     * Loads the data for the given ident as an image. If no image is found, null is returned
     * @param ident cache identifier
     * @return BufferedImage or null
     */
    public BufferedImage getImg(String ident) {
        if(!enabled) return null;
        try {
            File img = getPath(ident, "png");
            if(!img.exists())
                return null;

            if(isExpired(img)) {
                img.delete();
                return null;
            }
            // Update last mod time so we don't expire recently used images
            if(updateModTime) {
                img.setLastModified(System.currentTimeMillis());
            }
            return ImageIO.read(img);
        } catch (Exception e) {
            Main.warn(e);
        }
        return null;
    }

    /**
     * Saves a given image and ident to the cache
     * @param ident cache identifier
     * @param image imaga data for storage
     */
    public void saveImg(String ident, BufferedImage image) {
        if (!enabled) return;
        try {
            ImageIO.write(image, "png", getPath(ident, "png"));
        } catch (Exception e) {
            Main.warn(e);
        }

        writes++;
        checkCleanUp();
    }

    /**
     * Sets the amount of time data is stored before it gets expired
     * @param amount of time in seconds
     * @param force will also write it to the preferences
     */
    public void setExpire(int amount, boolean force) {
        String key = "cache." + ident + "." + "expire";
        if(!Main.pref.get(key).isEmpty() && !force)
            return;

        this.expire = amount > 0 ? amount : EXPIRE_NEVER;
        Main.pref.putLong(key, this.expire);
    }

    /**
     * Sets the amount of data stored in the cache
     * @param amount in Megabytes
     * @param force will also write it to the preferences
     */
    public void setMaxSize(int amount, boolean force) {
        String key = "cache." + ident + "." + "maxsize";
        if(!Main.pref.get(key).isEmpty() && !force)
            return;

        this.maxsize = amount > 0 ? amount : -1;
        Main.pref.putLong(key, this.maxsize);
    }

    /**
     * Call this with <code>true</code> to update the last modification time when a file it is read.
     * Call this with <code>false</code> to not update the last modification time when a file is read.
     * @param to update state
     */
    public void setUpdateModTime(boolean to) {
        updateModTime = to;
    }

    /**
     * Checks if a clean up is needed and will do so if necessary
     */
    public void checkCleanUp() {
        if(this.writes > CLEANUP_INTERVAL) {
            cleanUp();
        }
    }

    /**
     * Performs a default clean up with the set values (deletes oldest files first)
     */
    public void cleanUp() {
        if(!this.enabled || maxsize == -1) return;

        TreeMap<Long, File> modtime = new TreeMap<Long, File>();
        long dirsize = 0;

        for(File f : dir.listFiles()) {
            if(isExpired(f)) {
                f.delete();
            } else {
                dirsize += f.length();
                modtime.put(f.lastModified(), f);
            }
        }

        if(dirsize < maxsize*1000*1000) return;

        Set<Long> keySet = modtime.keySet();
        Iterator<Long> it = keySet.iterator();
        int i=0;
        while (it.hasNext()) {
            i++;
            modtime.get(it.next()).delete();

            // Delete a couple of files, then check again
            if(i % CLEANUP_TRESHOLD == 0 && getDirSize() < maxsize)
                return;
        }
        writes = 0;
    }

    final static public int CLEAN_ALL = 0;
    final static public int CLEAN_SMALL_FILES = 1;
    final static public int CLEAN_BY_DATE = 2;
    /**
     * Performs a non-default, specified clean up
     * @param type any of the CLEAN_XX constants.
     * @param size for CLEAN_SMALL_FILES: deletes all files smaller than (size) bytes
     */
    public void customCleanUp(int type, int size) {
        switch(type) {
        case CLEAN_ALL:
            for(File f : dir.listFiles()) {
                f.delete();
            }
            break;
        case CLEAN_SMALL_FILES:
            for(File f: dir.listFiles())
                if(f.length() < size) {
                    f.delete();
                }
            break;
        case CLEAN_BY_DATE:
            cleanUp();
            break;
        }
    }

    /**
     * Calculates the size of the directory
     * @return long Size of directory in bytes
     */
    private long getDirSize() {
        if(!enabled) return -1;
        long dirsize = 0;

        for(File f : this.dir.listFiles()) {
            dirsize += f.length();
        }
        return dirsize;
    }

    /**
     * Returns a short and unique file name for a given long identifier
     * @return String short filename
     */
    private static String getUniqueFilename(String ident) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            BigInteger number = new BigInteger(1, md.digest(ident.getBytes()));
            return number.toString(16);
        } catch(Exception e) {
            // Fall back. Remove unsuitable characters and some random ones to shrink down path length.
            // Limit it to 70 characters, that leaves about 190 for the path on Windows/NTFS
            ident = ident.replaceAll("[^a-zA-Z0-9]", "");
            ident = ident.replaceAll("[acegikmoqsuwy]", "");
            return ident.substring(ident.length() - 70);
        }
    }

    /**
     * Gets file path for ident with customizable file-ending
     * @param ident cache identifier
     * @param ending file extension
     * @return file structure
     */
    private File getPath(String ident, String ending) {
        return new File(dir, getUniqueFilename(ident) + "." + ending);
    }

    /**
     * Gets file path for ident
     * @param ident cache identifier
     * @return file structure
     */
    private File getPath(String ident) {
        return new File(dir, getUniqueFilename(ident));
    }

    /**
     * Checks whether a given file is expired
     * @param file file description structure
     * @return expired state
     */
    private boolean isExpired(File file) {
        if(CacheFiles.EXPIRE_NEVER == this.expire)
            return false;
        return (file.lastModified() < (System.currentTimeMillis() - expire*1000));
    }
}
