// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import java.util.concurrent.Future;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.OsmUrlToBounds;

/**
 * Task allowing to download an OSM url containing coordinates
 * @since 4996
 */
public class DownloadOsmUrlTask extends DownloadOsmTask {

    @Override
    public Future<?> loadUrl(boolean newLayer, String url, ProgressMonitor progressMonitor) {
        return download(newLayer, OsmUrlToBounds.parse(url), null);
    }
    
    @Override
    public String[] getPatterns() {
        return new String[]{
                "http://www\\.(osm|openstreetmap)\\.org/\\?lat=.*&lon=.*",
                "http://www\\.(osm|openstreetmap)\\.org/#map=\\p{Digit}+/.*/.*"};
    }

    @Override
    public String getTitle() {
        return tr("Download OSM URL");
    }
}
