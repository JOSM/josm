// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

/**
 * Download policy.
 *
 * Determines if download from the OSM server is intended, discouraged, or disabled / blocked.
 * @see UploadPolicy
 * @since 13559 (extracted from {@link DataSet})
 */
public enum DownloadPolicy {
    /**
     * Normal dataset, download intended.
     */
    NORMAL("true"),
    /**
     * Download blocked.
     * Download options completely disabled. Intended for private layers, see #8039.
     */
    BLOCKED("never");

    final String xmlFlag;

    DownloadPolicy(String xmlFlag) {
        this.xmlFlag = xmlFlag;
    }

    /**
     * Get the corresponding value of the <code>upload='...'</code> XML-attribute
     * in the .osm file.
     * @return value of the <code>download</code> attribute
     */
    public String getXmlFlag() {
        return xmlFlag;
    }

    /**
     * Returns the {@code DownloadPolicy} for the given <code>upload='...'</code> XML-attribute
     * @param xmlFlag <code>download='...'</code> XML-attribute to convert
     * @return {@code DownloadPolicy} value
     * @throws IllegalArgumentException for invalid values
     */
    public static DownloadPolicy of(String xmlFlag) {
        for (DownloadPolicy policy : values()) {
            if (policy.getXmlFlag().equalsIgnoreCase(xmlFlag)) {
                return policy;
            }
        }
        throw new IllegalArgumentException(xmlFlag);
    }
}
