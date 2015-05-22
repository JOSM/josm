// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.openstreetmap.josm.Main;

/**
 * Use this class if you want to cache and store a single file that gets updated regularly.
 * Unless you flush() it will be kept in memory. If you want to cache a lot of data and/or files, use CacheFiles.
 * @author xeen
 * @param <T> a {@link Throwable} that may be thrown during {@link #updateData()},
 * use {@link RuntimeException} if no exception must be handled.
 * @since 1450
 */
public abstract class CacheCustomContent<T extends Throwable> {

    /** Update interval meaning an update is always needed */
    public static final int INTERVAL_ALWAYS = -1;
    /** Update interval meaning an update is needed each hour */
    public static final int INTERVAL_HOURLY = 60*60;
    /** Update interval meaning an update is needed each day */
    public static final int INTERVAL_DAILY = INTERVAL_HOURLY * 24;
    /** Update interval meaning an update is needed each week */
    public static final int INTERVAL_WEEKLY = INTERVAL_DAILY * 7;
    /** Update interval meaning an update is needed each month */
    public static final int INTERVAL_MONTHLY = INTERVAL_WEEKLY * 4;
    /** Update interval meaning an update is never needed */
    public static final int INTERVAL_NEVER = Integer.MAX_VALUE;

    /**
     * Where the data will be stored
     */
    private byte[] data = null;

    /**
     * The ident that identifies the stored file. Includes file-ending.
     */
    private final String ident;

    /**
     * The (file-)path where the data will be stored
     */
    private final File path;

    /**
     * How often to update the cached version
     */
    private final int updateInterval;

    /**
     * This function will be executed when an update is required. It has to be implemented by the
     * inheriting class and should use a worker if it has a long wall time as the function is
     * executed in the current thread.
     * @return the data to cache
     */
    protected abstract byte[] updateData() throws T;

    /**
     * Initializes the class. Note that all read data will be stored in memory until it is flushed
     * by flushData().
     * @param ident ident that identifies the stored file. Includes file-ending.
     * @param updateInterval update interval in seconds. -1 means always
     */
    public CacheCustomContent(String ident, int updateInterval) {
        this.ident = ident;
        this.updateInterval = updateInterval;
        this.path = new File(Main.pref.getCacheDirectory(), ident);
    }

    /**
     * This function serves as a comfort hook to perform additional checks if the cache is valid
     * @return True if the cached copy is still valid
     */
    protected boolean isCacheValid() {
        return true;
    }

    private boolean needsUpdate() {
        if (isOffline()) {
            return false;
        }
        return Main.pref.getInteger("cache." + ident, 0) + updateInterval < System.currentTimeMillis()/1000
                || !isCacheValid();
    }

    private boolean isOffline() {
        try {
            checkOfflineAccess();
            return false;
        } catch (OfflineAccessException e) {
            return true;
        }
    }

    protected void checkOfflineAccess() {
        // To be overriden by subclasses
    }

    /**
     * Updates data if required
     * @return Returns the data
     * @throws T if an error occurs
     */
    public byte[] updateIfRequired() throws T {
        if (needsUpdate())
            return updateForce();
        return getData();
    }

    /**
     * Updates data if required
     * @return Returns the data as string
     * @throws T if an error occurs
     */
    public String updateIfRequiredString() throws T {
        if (needsUpdate())
            return updateForceString();
        return getDataString();
    }

    /**
     * Executes an update regardless of updateInterval
     * @return Returns the data
     * @throws T if an error occurs
     */
    public byte[] updateForce() throws T {
        this.data = updateData();
        saveToDisk();
        Main.pref.putInteger("cache." + ident, (int)(System.currentTimeMillis()/1000));
        return data;
    }

    /**
     * Executes an update regardless of updateInterval
     * @return Returns the data as String
     * @throws T if an error occurs
     */
    public String updateForceString() throws T {
        updateForce();
        return new String(data, StandardCharsets.UTF_8);
    }

    /**
     * Returns the data without performing any updates
     * @return the data
     * @throws T if an error occurs
     */
    public byte[] getData() throws T {
        if (data == null) {
            loadFromDisk();
        }
        return data;
    }

    /**
     * Returns the data without performing any updates
     * @return the data as String
     * @throws T if an error occurs
     */
    public String getDataString() throws T {
        byte[] array = getData();
        if (array == null) {
            return null;
        }
        return new String(array, StandardCharsets.UTF_8);
    }

    /**
     * Tries to load the data using the given ident from disk. If this fails, data will be updated, unless run in offline mode
     */
    private void loadFromDisk() throws T {
        try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(path))) {
            this.data = new byte[input.available()];
            if (input.read(this.data) < this.data.length) {
                Main.error("Failed to read expected contents from "+path);
            }
        } catch (IOException e) {
            if (!isOffline()) {
                this.data = updateForce();
            }
        }
    }

    /**
     * Stores the data to disk
     */
    private void saveToDisk() {
        try (BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(path))) {
            output.write(this.data);
            output.flush();
        } catch (IOException e) {
            Main.error(e);
        }
    }

    /**
     * Flushes the data from memory. Class automatically reloads it from disk or updateData() if required
     */
    public void flushData() {
        data = null;
    }
}
