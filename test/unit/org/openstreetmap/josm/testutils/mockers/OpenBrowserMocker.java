// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.mockers;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OpenBrowser;

import mockit.Mock;
import mockit.MockUp;

/**
 * MockUp for {@link OpenBrowser}.
 *
 * @author Taylor Smock
 */
public class OpenBrowserMocker extends MockUp<OpenBrowser> {
    private static final List<URI> calledUris = new ArrayList<>();

    /**
     * A Mock for {@link OpenBrowser#displayUrl} that doesn't actually open a
     * browser window, for use in headless environments.
     *
     * @param uri The URI to display (theoretically)
     * @return <code>null</code> for success.
     */
    @Mock
    public static String displayUrl(URI uri) {
        CheckParameterUtil.ensureParameterNotNull(uri, "uri");

        Logging.info(tr("Opening URL: {0}", uri));
        calledUris.add(uri);

        return null;
    }

    /**
     * Get the called URIs
     *
     * @return A list of called URIs
     */
    public static List<URI> getCalledURIs() {
        return calledUris;
    }
}
