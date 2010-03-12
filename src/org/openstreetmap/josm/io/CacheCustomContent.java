// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import org.openstreetmap.josm.Main;

/**
 * Use this class if you want to cache and store a single file that gets updated regularly.
 * Unless you flush() it will be kept in memory. If you want to cache a lot of data and/or files,
 * use CacheFiles
 * @author xeen
 *
 */
public abstract class CacheCustomContent {
    /**
     * Common intervals
     */
    final static public int INTERVAL_ALWAYS = -1;
    final static public int INTERVAL_HOURLY = 60*60;
    final static public int INTERVAL_DAILY = INTERVAL_HOURLY * 24;
    final static public int INTERVAL_WEEKLY = INTERVAL_DAILY * 7;
    final static public int INTERVAL_MONTHLY = INTERVAL_WEEKLY * 4;
    final static public int INTERVAL_NEVER = Integer.MAX_VALUE;

    /**
     * Where the data will be stored
     */
    private byte[] data = null;

    /**
     * The ident that identifies the stored file. Includes file-ending.
     */
    final private String ident;

    /**
     * The (file-)path where the data will be stored
     */
    final private File path;

    /**
     * How often to update the cached version
     */
    final private int updateInterval;

    /**
     * This function will be executed when an update is required. It has to be implemented by the
     * inheriting class and should use a worker if it has a long wall time as the function is
     * executed in the current thread.
     * @return the data to cache
     */
    protected abstract byte[] updateData();

    /**
     * This function serves as a comfort hook to perform additional checks if the cache is valid
     * @return True if the cached copy is still valid
     */
    protected boolean isCacheValid() {
        return true;
    }

    /**
     * Initializes the class. Note that all read data will be stored in memory until it is flushed
     * by flushData().
     * @param ident
     * @param updateInterval
     */
    public CacheCustomContent(String ident, int updateInterval) {
        this.ident = ident;
        this.updateInterval = updateInterval;
        this.path = new File(Main.pref.getPreferencesDir(), ident);
    }

    /**
     * Updates data if required
     * @return Returns the data
     */
    public byte[] updateIfRequired() {
        if(Main.pref.getInteger("cache." + ident, 0) + updateInterval < new Date().getTime()/1000
                || !isCacheValid())
            return updateForce();
        return getData();
    }

    /**
     * Updates data if required
     * @return Returns the data as string
     */
    public String updateIfRequiredString() {
        if(Main.pref.getInteger("cache." + ident, 0) + updateInterval < new Date().getTime()/1000
                || !isCacheValid())
            return updateForceString();
        return getDataString();
    }

    /**
     * Executes an update regardless of updateInterval
     * @return Returns the data
     */
    public byte[] updateForce() {
        this.data = updateData();
        saveToDisk();
        Main.pref.putInteger("cache." + ident, (int)(new Date().getTime()/1000));
        return data;
    }

    /**
     * Executes an update regardless of updateInterval
     * @return Returns the data as String
     */
    public String updateForceString() {
        updateForce();
        try {
            return new String(data,"utf-8");
        } catch(UnsupportedEncodingException e){
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Returns the data without performing any updates
     * @return the data
     */
    public byte[] getData() {
        if(data == null) {
            loadFromDisk();
        }
        return data;
    }

    /**
     * Returns the data without performing any updates
     * @return the data as String
     */
    public String getDataString() {
        try {
            return new String(getData(), "utf-8");
        } catch(UnsupportedEncodingException e){
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Tries to load the data using the given ident from disk. If this fails, data will be updated
     */
    private void loadFromDisk() {
        try {
            BufferedInputStream input = new BufferedInputStream(new FileInputStream(path));
            this.data = new byte[input.available()];
            input.read(this.data);
            input.close();
        } catch(IOException e) {
            this.data = updateForce();
        }
    }

    /**
     * Stores the data to disk
     */
    private void saveToDisk() {
        try {
            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(path));
            output.write(this.data);
            output.flush();
            output.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Flushes the data from memory. Class automatically reloads it from disk or updateData() if
     * required
     */
    public void flushData() {
        data = null;
    }
}
