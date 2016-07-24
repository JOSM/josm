// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.GeoUrlToBounds;

import java.util.concurrent.Future;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * Task allowing to download a Geo URL (as specified in <a href="https://tools.ietf.org/html/rfc5870">RFC 5870</a>).
 */
public class DownloadGeoUrlTask extends DownloadOsmTask {

    @Override
    public Future<?> loadUrl(boolean newLayer, String url, ProgressMonitor progressMonitor) {
        return download(newLayer, GeoUrlToBounds.parse(url), null);
    }

    @Override
    public String[] getPatterns() {
        return new String[]{GeoUrlToBounds.PATTERN.toString()};
    }

    @Override
    public String getTitle() {
        return tr("Download Geo URL");
    }
}
