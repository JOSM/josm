// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

public class OsmPrivileges {
    private boolean allowWriteApi;
    private boolean allowWriteGpx;
    private boolean allowReadGpx;
    private boolean allowReadPrefs;
    private boolean allowWritePrefs;
    private boolean allowModifyNotes;

    public boolean isAllowWriteApi() {
        return allowWriteApi;
    }
    public void setAllowWriteApi(boolean allowWriteApi) {
        this.allowWriteApi = allowWriteApi;
    }
    public boolean isAllowWriteGpx() {
        return allowWriteGpx;
    }
    public void setAllowWriteGpx(boolean allowWriteGpx) {
        this.allowWriteGpx = allowWriteGpx;
    }
    public boolean isAllowReadGpx() {
        return allowReadGpx;
    }
    public void setAllowReadGpx(boolean allowReadGpx) {
        this.allowReadGpx = allowReadGpx;
    }
    public boolean isAllowReadPrefs() {
        return allowReadPrefs;
    }
    public void setAllowReadPrefs(boolean allowReadPrefs) {
        this.allowReadPrefs = allowReadPrefs;
    }
    public boolean isAllowWritePrefs() {
        return allowWritePrefs;
    }
    public void setAllowWritePrefs(boolean allowWritePrefs) {
        this.allowWritePrefs = allowWritePrefs;
    }

    public boolean isAllowModifyNotes() {
        return allowModifyNotes;
    }

    public void setAllowModifyNotes(boolean allowModifyNotes) {
        this.allowModifyNotes = allowModifyNotes;
    }
}
