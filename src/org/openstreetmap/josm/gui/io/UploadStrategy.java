// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.Main;

public enum UploadStrategy {
    /**
     * Uploads the objects individually, one request per object
     */
    INDIVIDUAL_OBJECTS_STRATEGY("individualobjects"),
    /**
     * Upload the objects in junks of n objects using m diff uploads
     */
    CHUNKED_DATASET_STRATEGY("chunked"),
    /**
     * Upload the objects in one request using 1 diff upload
     */
    SINGLE_REQUEST_STRATEGY("singlerequest");

    private String preferenceValue;

    UploadStrategy(String preferenceValue) {
        this.preferenceValue = preferenceValue;
    }

    public static UploadStrategy fromPreference(String preferenceValue) {
        if (preferenceValue == null) return null;
        preferenceValue = preferenceValue.trim().toLowerCase();
        for (UploadStrategy strategy: values()) {
            if (strategy.getPreferenceValue().equals(preferenceValue))
                return strategy;
        }
        return null;
    }

    /**
     * Replies the value which is written to the preferences for a specific
     * upload strategy
     *
     * @return the value which is written to the preferences for a specific
     * upload strategy
     */
    public String getPreferenceValue() {
        return preferenceValue;
    }

    /**
     * the default upload strategy
     */
    public final static UploadStrategy DEFAULT_UPLOAD_STRATEGY = SINGLE_REQUEST_STRATEGY;

    /**
     * Replies the upload strategy currently configured in the preferences.
     *
     * First checks for the preference key <pre>osm-server.upload-strategy</pre>. If not
     * present, checks for the legacy preference key <pre>osm-server.atomic-upload</pre>.
     *
     * If both are missing or if the preference value is illegal, {@link #DEFAULT_UPLOAD_STRATEGY}
     * is replied.
     *
     * @return the upload strategy currently configured in the preferences.
     */
    public static UploadStrategy getFromPreferences() {
        String v = Main.pref.get("osm-server.upload-strategy", null);
        if (v == null) {
            // legacy support. Until 12/2009 we had osm-server.atomic-upload only.
            // If we still find "osm-server.atomic-upload" we use it and remove it.
            // When the preferences are saved the next time, "osm-server.upload-strategy"
            // will be inserted.
            v = Main.pref.get("osm-server.atomic-upload", null);
            if (v != null) {
                Main.pref.removeFromCollection("osm-server.atomic-upload", v);
            } else {
                v = "";
            }
            v = v.trim().toLowerCase();
            if (v.equals("true"))
                return SINGLE_REQUEST_STRATEGY;
            else if (v.equals("false"))
                return INDIVIDUAL_OBJECTS_STRATEGY;
            else
                return DEFAULT_UPLOAD_STRATEGY;
        }
        UploadStrategy strategy = fromPreference(v);
        if (strategy == null) {
            Main.warn(tr("Unexpected value for key ''{0}'' in preferences, got ''{1}''", "osm-server.upload-strategy", v ));
            return DEFAULT_UPLOAD_STRATEGY;
        }
        return strategy;
    }

    /**
     * Saves the upload strategy <code>strategy</code> to the preferences.
     *
     * @param strategy the strategy to save
     */
    public static void saveToPreferences(UploadStrategy strategy) {
        Main.pref.put("osm-server.upload-strategy", strategy.getPreferenceValue());
    }
}
