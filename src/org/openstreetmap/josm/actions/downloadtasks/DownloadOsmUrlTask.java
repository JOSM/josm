// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import java.util.concurrent.Future;

import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.OsmUrlToBounds;

public class DownloadOsmUrlTask extends DownloadOsmTask {

    @Override
    public Future<?> loadUrl(boolean newLayer, String url, ProgressMonitor progressMonitor) {
        return download(newLayer, OsmUrlToBounds.parse(url), null);
    }

    @Override
    public boolean acceptsUrl(String url) {
        return url != null && (
                url.matches("http://www\\.openstreetmap\\.org/\\?lat=.*&lon=.*")
                );
    }
}
