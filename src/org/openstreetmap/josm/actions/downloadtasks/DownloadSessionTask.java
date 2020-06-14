// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.Future;

import org.openstreetmap.josm.actions.SessionLoadAction.Loader;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Logging;

/**
 * Task allowing to download JOSM session (*.jos, *.joz file).
 * @since 6215
 */
public class DownloadSessionTask extends AbstractDownloadTask<Object> {

    private static final String PATTERN_SESSION = "https?://.*/.*\\.jo(s|z)";

    private Loader loader;

    @Override
    public String getTitle() {
        return tr("Download session");
    }

    @Override
    public String[] getPatterns() {
        return new String[]{PATTERN_SESSION};
    }

    @Override
    public Future<?> download(DownloadParams settings, Bounds downloadArea, ProgressMonitor progressMonitor) {
        return null;
    }

    @Override
    public Future<?> loadUrl(DownloadParams settings, String url, ProgressMonitor progressMonitor) {
        if (url != null && url.matches(PATTERN_SESSION)) {
            try {
                URL u = new URL(url);
                loader = new Loader(HttpClient.create(u).connect().getContent(), u.toURI(), url.endsWith(".joz"));
                return MainApplication.worker.submit(loader);
            } catch (URISyntaxException | IOException e) {
                Logging.error(e);
            }
        }
        return null;
    }

    @Override
    public void cancel() {
        if (loader != null) {
            loader.cancel();
        }
    }

    @Override
    public String getConfirmationMessage(URL url) {
        // TODO
        return null;
    }

    /**
     * Do not allow to load a session file via remotecontrol.
     *
     * Session importers can be added by plugins and there is currently
     * no way to ensure that these are safe for remotecontol.
     * @return <code>true</code> if session import is allowed
     */
    @Override
    public boolean isSafeForRemotecontrolRequests() {
        return Config.getPref().getBoolean("remotecontrol.import.allow_session", false);
    }
}
