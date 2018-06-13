// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.concurrent.Future;

import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.GeoUrlToBounds;
import org.openstreetmap.josm.tools.OsmUrlToBounds;

/**
 * Task allowing to download an OSM url containing coordinates
 * @since 4996
 */
public class DownloadOsmUrlTask extends DownloadOsmTask {

    @Override
    public Future<?> loadUrl(DownloadParams settings, String url, ProgressMonitor progressMonitor) {
        return download(settings, OsmUrlToBounds.parse(url), null);
    }

    @Override
    public String[] getPatterns() {
        return new String[]{
                "https?://www\\.(osm|openstreetmap)\\.org/\\?lat=.*&lon=.*",
                "https?://www\\.(osm|openstreetmap)\\.org/(.*)?#map=\\p{Digit}+/.*/.*",
                GeoUrlToBounds.PATTERN.toString(),
        };
    }

    @Override
    public String getTitle() {
        return tr("Download OSM URL");
    }
}
