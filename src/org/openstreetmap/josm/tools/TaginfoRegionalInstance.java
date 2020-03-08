// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.Objects;
import java.util.Set;

/**
 * Describes a regional Taginfo instance.
 * @since 15876
 */
public class TaginfoRegionalInstance {

    /** Instance URL */
    private final String url;
    /** Set of ISO3166 codes for the covered areas */
    private final Set<String> isoCodes;
    /** Optional suffix to distinguish them in UI */
    private final String suffix;

    /**
     * Constructs a new {@code TaginfoRegionalInstance}.
     * @param url Instance URL. Must not be null
     * @param isoCodes Set of ISO3166 codes for the covered areas. Must not be null
     */
    public TaginfoRegionalInstance(String url, Set<String> isoCodes) {
        this(url, isoCodes, null);
    }

    /**
     * Constructs a new {@code TaginfoRegionalInstance}.
     * @param url Instance URL. Must not be null
     * @param isoCodes Set of ISO3166 codes for the covered areas. Must not be null
     * @param suffix Optional suffix to distinguish them in UI. Can be null
     */
    public TaginfoRegionalInstance(String url, Set<String> isoCodes, String suffix) {
        this.url = Objects.requireNonNull(url);
        this.isoCodes = Objects.requireNonNull(isoCodes);
        this.suffix = suffix;
    }

    /**
     * Returns the instance URL.
     * @return instance URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the set of ISO3166 codes for the covered areas.
     * @return set of ISO3166 codes for the covered areas
     */
    public Set<String> getIsoCodes() {
        return isoCodes;
    }

    /**
     * Returns the optional suffix to distinguish them in UI.
     * @return optional suffix to distinguish them in UI. Can be null
     */
    public String getSuffix() {
        return suffix;
    }

    @Override
    public String toString() {
        return (suffix == null ? "" : suffix + " ") + String.join("/", isoCodes);
    }
}
