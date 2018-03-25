// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

/**
 * Upload policy.
 *
 * Determines if upload to the OSM server is intended, discouraged, or disabled / blocked.
 * @see DownloadPolicy
 * @since 13559 (extracted from {@link DataSet})
 */
public enum UploadPolicy {
    /**
     * Normal dataset, upload intended.
     */
    NORMAL("true"),
    /**
     * Upload discouraged, for example when using or distributing a private dataset.
     */
    DISCOURAGED("false"),
    /**
     * Upload blocked.
     * Upload options completely disabled. Intended for special cases
     * where a warning dialog is not enough, see #12731.
     *
     * For the user, it shouldn't be too easy to disable this flag.
     */
    BLOCKED("never");

    final String xmlFlag;

    UploadPolicy(String xmlFlag) {
        this.xmlFlag = xmlFlag;
    }

    /**
     * Get the corresponding value of the <code>upload='...'</code> XML-attribute in the .osm file.
     * @return value of the <code>upload</code> attribute
     */
    public String getXmlFlag() {
        return xmlFlag;
    }

    /**
     * Returns the {@code UploadPolicy} for the given <code>upload='...'</code> XML-attribute
     * @param xmlFlag <code>upload='...'</code> XML-attribute to convert
     * @return {@code UploadPolicy} value
     * @throws IllegalArgumentException for invalid values
     * @since 13434
     */
    public static UploadPolicy of(String xmlFlag) {
        for (UploadPolicy policy : values()) {
            if (policy.getXmlFlag().equalsIgnoreCase(xmlFlag)) {
                return policy;
            }
        }
        throw new IllegalArgumentException(xmlFlag);
    }
}
