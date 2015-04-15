// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import org.openstreetmap.josm.gui.progress.ProgressMonitor;

import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openstreetmap.josm.tools.I18n.tr;

public class DownloadNotesUrlIdTask extends DownloadNotesTask {

    private final String URL_ID_PATTERN = "https?://www\\.(osm|openstreetmap)\\.org/note/(\\p{Digit}+).*";

    @Override
    public Future<?> loadUrl(boolean newLayer, String url, ProgressMonitor progressMonitor) {
        final Matcher matcher = Pattern.compile(URL_ID_PATTERN).matcher(url);
        final long id;
        try {
            matcher.matches();
            return download(newLayer, Long.parseLong(matcher.group(2)), null);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Failed to parse note id from " + url);
        }
    }

    @Override
    public String[] getPatterns() {
        return new String[]{URL_ID_PATTERN};
    }

    public boolean acceptsUrl(String url) {
        return super.acceptsUrl(url);
    }

    @Override
    public String getTitle() {
        return tr("Download OSM Note by ID");
    }
}
