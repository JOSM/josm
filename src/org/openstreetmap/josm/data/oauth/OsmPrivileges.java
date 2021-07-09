// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

/**
 * List of permissions granted to the current OSM connection.
 * @since 2747
 */
public class OsmPrivileges {
    private boolean allowWriteApi;
    private boolean allowWriteGpx;
    private boolean allowReadGpx;
    private boolean allowReadPrefs;
    private boolean allowWritePrefs;
    private boolean allowModifyNotes;
    private boolean allowWriteDiary;

    /**
     * Determines if the client is allowed to modify the map.
     * @return {@code true} if the client is allowed to modify the map, {@code false} otherwise
     */
    public boolean isAllowWriteApi() {
        return allowWriteApi;
    }

    /**
     * Sets whether the client is allowed to modify the map.
     * @param allowWriteApi {@code true} if the client is allowed to modify the map, {@code false} otherwise
     */
    public void setAllowWriteApi(boolean allowWriteApi) {
        this.allowWriteApi = allowWriteApi;
    }

    /**
     * Determines if the client is allowed to upload GPS traces.
     * @return {@code true} if the client is allowed to upload GPS traces, {@code false} otherwise
     */
    public boolean isAllowWriteGpx() {
        return allowWriteGpx;
    }

    /**
     * Sets whether the client is allowed to upload GPS traces.
     * @param allowWriteGpx {@code true} if the client is allowed to upload GPS traces, {@code false} otherwise
     */
    public void setAllowWriteGpx(boolean allowWriteGpx) {
        this.allowWriteGpx = allowWriteGpx;
    }

    /**
     * Determines if the client is allowed to read private GPS traces.
     * @return {@code true} if the client is allowed to read private GPS traces, {@code false} otherwise
     */
    public boolean isAllowReadGpx() {
        return allowReadGpx;
    }

    /**
     * Sets whether the client is allowed to read private GPS traces.
     * @param allowReadGpx {@code true} if the client is allowed to read private GPS traces, {@code false} otherwise
     */
    public void setAllowReadGpx(boolean allowReadGpx) {
        this.allowReadGpx = allowReadGpx;
    }

    /**
     * Determines if the client is allowed to read user preferences.
     * @return {@code true} if the client is allowed to read user preferences, {@code false} otherwise
     */
    public boolean isAllowReadPrefs() {
        return allowReadPrefs;
    }

    /**
     * Sets whether the client is allowed to read user preferences.
     * @param allowReadPrefs {@code true} if the client is allowed to read user preferences, {@code false} otherwise
     */
    public void setAllowReadPrefs(boolean allowReadPrefs) {
        this.allowReadPrefs = allowReadPrefs;
    }

    /**
     * Determines if the client is allowed to modify user preferences.
     * @return {@code true} if the client is allowed to modify user preferences, {@code false} otherwise
     */
    public boolean isAllowWritePrefs() {
        return allowWritePrefs;
    }

    /**
     * Sets whether the client is allowed to modify user preferences.
     * @param allowWritePrefs {@code true} if the client is allowed to modify user preferences, {@code false} otherwise
     */
    public void setAllowWritePrefs(boolean allowWritePrefs) {
        this.allowWritePrefs = allowWritePrefs;
    }

    /**
     * Determines if the client is allowed to modify notes.
     * @return {@code true} if the client is allowed to modify notes, {@code false} otherwise
     */
    public boolean isAllowModifyNotes() {
        return allowModifyNotes;
    }

    /**
     * Sets whether the client is allowed to modify notes.
     * @param allowModifyNotes {@code true} if the client is allowed to modify notes, {@code false} otherwise
     */
    public void setAllowModifyNotes(boolean allowModifyNotes) {
        this.allowModifyNotes = allowModifyNotes;
    }

    /**
     * Determines if the client is allowed to write diary.
     * @return {@code true} if the client is allowed to write diary, {@code false} otherwise
     * @since 17972
     */
    public boolean isAllowWriteDiary() {
        return allowWriteDiary;
    }

    /**
     * Sets whether the client is allowed to write diary.
     * @param allowWriteDiary {@code true} if the client is allowed to write diary, {@code false} otherwise
     * @since 17972
     */
    public void setAllowWriteDiary(boolean allowWriteDiary) {
        this.allowWriteDiary = allowWriteDiary;
    }
}
