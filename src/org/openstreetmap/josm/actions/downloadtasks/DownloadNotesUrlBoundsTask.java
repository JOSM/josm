// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.concurrent.Future;

import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.OsmUrlToBounds;

/**
 * Specialized task for downloading OSM notes within bounds.
 * <p>
 * It handles one URL pattern: openstreetmap website URL with {@code #map?} argument.
 * @since 8195
 */
public class DownloadNotesUrlBoundsTask extends DownloadNotesTask {

    @Override
    public Future<?> loadUrl(DownloadParams settings, String url, ProgressMonitor progressMonitor) {
        return download(settings, OsmUrlToBounds.parse(url), null);
    }

    @Override
    public String[] getPatterns() {
        return new String[]{
                "https?://www\\.(osm|openstreetmap)\\.org/(.*)?#map=\\p{Digit}+/.*/.*&layers=[A-MO-Z]*N[A-MO-Z]*"};
    }

    @Override
    public String getTitle() {
        return tr("Download OSM Notes within Bounds");
    }
}
