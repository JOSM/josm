// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.imagery;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.HttpClient.Response;
import org.openstreetmap.josm.tools.Utils;

/**
 * Provider of confidential imagery API keys.
 * @since 15855
 */
public final class ApiKeyProvider {

    private ApiKeyProvider() {
        // Hide public constructor
    }

    private static List<String> getApiKeySites() {
        return Preferences.main().getList("apikey.sites",
                Collections.singletonList(Config.getUrls().getJOSMWebsite()+"/mapkey/"));
    }

    /**
     * Retrieves the API key for the given imagery id.
     * @param imageryId imagery id
     * @return the API key for the given imagery id
     * @throws IOException in case of I/O error
     */
    public static String retrieveApiKey(String imageryId) throws IOException {
        for (String siteUrl : getApiKeySites()) {
            Response response = HttpClient.create(new URL(siteUrl + imageryId)).connect();
            try {
                if (response.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    return Utils.strip(response.fetchContent());
                }
            } finally {
                response.disconnect();
            }
        }
        return null;
    }
}
