// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.OsmUrlToBounds;

import java.util.concurrent.Future;

import static org.openstreetmap.josm.tools.I18n.tr;

public class DownloadNotesUrlBoundsTask extends DownloadNotesTask {

    @Override
    public Future<?> loadUrl(boolean newLayer, String url, ProgressMonitor progressMonitor) {
        return download(newLayer, OsmUrlToBounds.parse(url), null);
    }

    @Override
    public String[] getPatterns() {
        return new String[]{
                "https?://www\\.(osm|openstreetmap)\\.org/(.*)?#map=\\p{Digit}+/.*/.*&layers=N"};
    }
    @Override
    public String getTitle() {
        return tr("Download OSM Notes within Bounds");
    }
}
