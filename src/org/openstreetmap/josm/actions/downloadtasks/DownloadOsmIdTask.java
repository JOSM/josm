// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collections;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.DownloadPrimitivesWithReferrersTask;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * Specialized task for downloading OSM objects by ID.
 * <p>
 * It handles one URL pattern: openstreetmap website URL with {@code /(node|way|relation)/<id>} argument.
 * @since 8240
 */
public class DownloadOsmIdTask extends DownloadOsmTask {

    private static final String URL_ID_PATTERN = "https?://www\\.(osm|openstreetmap)\\.org/(node|way|relation)/(\\p{Digit}+).*";

    @Override
    public String[] getPatterns() {
        return new String[]{URL_ID_PATTERN};
    }

    @Override
    public Future<?> loadUrl(boolean newLayer, String url, ProgressMonitor progressMonitor) {
        final Matcher matcher = Pattern.compile(URL_ID_PATTERN).matcher(url);
        if (matcher.matches()) {
            final OsmPrimitiveType type = OsmPrimitiveType.from(matcher.group(2));
            final long id = Long.parseLong(matcher.group(3));
            final PrimitiveId primitiveId = new SimplePrimitiveId(id, type);
            final DownloadPrimitivesWithReferrersTask downloadTask = new DownloadPrimitivesWithReferrersTask(
                    newLayer, Collections.singletonList(primitiveId), true, true, null, null);
            return MainApplication.worker.submit(downloadTask);
        } else {
            throw new IllegalStateException("Failed to parse id from " + url);
        }
    }

    @Override
    public String getTitle() {
        return tr("Download OSM object by ID");
    }
}
