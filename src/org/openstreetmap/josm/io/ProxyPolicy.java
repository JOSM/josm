// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.util.Locale;

/**
 * The proxy policy is how JOSM will use proxy information.
 * @since 12805 (extracted from {@code ProxyPreferencesPanel})
 */
public enum ProxyPolicy {
    /** No proxy: JOSM will access Internet resources directly */
    NO_PROXY("no-proxy"),
    /** Use system settings: JOSM will use system proxy settings */
    USE_SYSTEM_SETTINGS("use-system-settings"),
    /** Use HTTP proxy: JOSM will use the given HTTP proxy, configured manually */
    USE_HTTP_PROXY("use-http-proxy"),
    /** Use HTTP proxy: JOSM will use the given SOCKS proxy */
    USE_SOCKS_PROXY("use-socks-proxy");

    private final String policyName;

    ProxyPolicy(String policyName) {
        this.policyName = policyName;
    }

    /**
     * Replies the policy name, to be stored in proxy preferences.
     * @return the policy unique name
     */
    public String getName() {
        return policyName;
    }

    /**
     * Retrieves a proxy policy from its name found in preferences.
     * @param policyName The policy name
     * @return The proxy policy matching the given name, or {@code null}
     */
    public static ProxyPolicy fromName(String policyName) {
        if (policyName == null) return null;
        policyName = policyName.trim().toLowerCase(Locale.ENGLISH);
        for (ProxyPolicy pp: values()) {
            if (pp.getName().equals(policyName))
                return pp;
        }
        return null;
    }
}
