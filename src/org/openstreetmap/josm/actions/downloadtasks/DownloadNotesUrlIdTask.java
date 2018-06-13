// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * Specialized task for downloading OSM notes by ID.
 * <p>
 * It handles one URL pattern: openstreetmap website URL with {@code /node/<id>} argument.
 * @since 8195
 */
public class DownloadNotesUrlIdTask extends DownloadNotesTask {

    private static final String URL_ID_PATTERN = "https?://www\\.(osm|openstreetmap)\\.org/note/(\\p{Digit}+).*";

    @Override
    public Future<?> loadUrl(DownloadParams settings, String url, ProgressMonitor progressMonitor) {
        final Matcher matcher = Pattern.compile(URL_ID_PATTERN).matcher(url);
        if (matcher.matches()) {
            return download(Long.parseLong(matcher.group(2)), null);
        } else {
            throw new IllegalStateException("Failed to parse note id from " + url);
        }
    }

    @Override
    public String[] getPatterns() {
        return new String[]{URL_ID_PATTERN};
    }

    @Override
    public String getTitle() {
        return tr("Download OSM Note by ID");
    }
}
