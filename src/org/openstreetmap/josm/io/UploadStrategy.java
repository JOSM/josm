// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Locale;

import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;

/**
 * The chunk mode to use when uploading
 * @since 12687 (moved from {@code gui.io} package)
 */
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

    private final String preferenceValue;

    UploadStrategy(String preferenceValue) {
        this.preferenceValue = preferenceValue;
    }

    /**
     * Reads the value from preferences
     * @param preferenceValue The preference value
     * @return The {@link UploadStrategy} for that preference or <code>null</code> if unknown
     */
    public static UploadStrategy fromPreference(String preferenceValue) {
        if (preferenceValue == null) return null;
        preferenceValue = preferenceValue.trim().toLowerCase(Locale.ENGLISH);
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
    public static final UploadStrategy DEFAULT_UPLOAD_STRATEGY = SINGLE_REQUEST_STRATEGY;

    /**
     * Replies the upload strategy currently configured in the preferences.
     *
     * Checks for the preference key <pre>osm-server.upload-strategy</pre>.
     *
     * If missing or if the preference value is illegal, {@link #DEFAULT_UPLOAD_STRATEGY}
     * is replied.
     *
     * @return the upload strategy currently configured in the preferences.
     */
    public static UploadStrategy getFromPreferences() {
        String v = Config.getPref().get("osm-server.upload-strategy", null);
        if (v == null) {
            return DEFAULT_UPLOAD_STRATEGY;
        }
        UploadStrategy strategy = fromPreference(v);
        if (strategy == null) {
            Logging.warn(tr("Unexpected value for key ''{0}'' in preferences, got ''{1}''", "osm-server.upload-strategy", v));
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
        Config.getPref().put("osm-server.upload-strategy", strategy.getPreferenceValue());
    }
}
