// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.LinkedList;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.SAXException;

/**
 * Asynchronous task for downloading a collection of plugins.
 *
 * When the task is finished {@link #getDownloadedPlugins()} replies the list of downloaded plugins
 * and {@link #getFailedPlugins()} replies the list of failed plugins.
 * @since 2817
 */
public class PluginDownloadTask extends PleaseWaitRunnable {

    /**
     * The accepted MIME types sent in the HTTP Accept header.
     * @since 6867
     */
    public static final String PLUGIN_MIME_TYPES = "application/java-archive, application/zip; q=0.9, application/octet-stream; q=0.5";

    private final Collection<PluginInformation> toUpdate = new LinkedList<>();
    private final Collection<PluginInformation> failed = new LinkedList<>();
    private final Collection<PluginInformation> downloaded = new LinkedList<>();
    private Exception lastException;
    private boolean canceled;
    private HttpClient downloadConnection;

    /**
     * Creates the download task
     *
     * @param parent the parent component relative to which the {@link org.openstreetmap.josm.gui.PleaseWaitDialog} is displayed
     * @param toUpdate a collection of plugin descriptions for plugins to update/download. Must not be null.
     * @param title the title to display in the {@link org.openstreetmap.josm.gui.PleaseWaitDialog}
     * @throws IllegalArgumentException if toUpdate is null
     */
    public PluginDownloadTask(Component parent, Collection<PluginInformation> toUpdate, String title) {
        super(parent, title == null ? "" : title, false /* don't ignore exceptions */);
        CheckParameterUtil.ensureParameterNotNull(toUpdate, "toUpdate");
        this.toUpdate.addAll(toUpdate);
    }

    /**
     * Creates the task
     *
     * @param monitor a progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null
     * @param toUpdate a collection of plugin descriptions for plugins to update/download. Must not be null.
     * @param title the title to display in the {@link org.openstreetmap.josm.gui.PleaseWaitDialog}
     * @throws IllegalArgumentException if toUpdate is null
     */
    public PluginDownloadTask(ProgressMonitor monitor, Collection<PluginInformation> toUpdate, String title) {
        super(title, monitor == null ? NullProgressMonitor.INSTANCE : monitor, false /* don't ignore exceptions */);
        CheckParameterUtil.ensureParameterNotNull(toUpdate, "toUpdate");
        this.toUpdate.addAll(toUpdate);
    }

    /**
     * Sets the collection of plugins to update.
     *
     * @param toUpdate the collection of plugins to update. Must not be null.
     * @throws IllegalArgumentException if toUpdate is null
     */
    public void setPluginsToDownload(Collection<PluginInformation> toUpdate) {
        CheckParameterUtil.ensureParameterNotNull(toUpdate, "toUpdate");
        this.toUpdate.clear();
        this.toUpdate.addAll(toUpdate);
    }

    @Override
    protected void cancel() {
        this.canceled = true;
        synchronized (this) {
            if (downloadConnection != null) {
                downloadConnection.disconnect();
            }
        }
    }

    @Override
    protected void finish() {
        // Do nothing. Error/success feedback is managed in PluginPreference.notifyDownloadResults()
    }

    protected void download(PluginInformation pi, File file) throws PluginDownloadException {
        if (pi.mainversion > Version.getInstance().getVersion()) {
            ExtendedDialog dialog = new ExtendedDialog(
                    progressMonitor.getWindowParent(),
                    tr("Skip Download"),
                    tr("Download Plugin"), tr("Skip Download")
            );
            dialog.setContent(tr("JOSM version {0} required for plugin {1}.", pi.mainversion, pi.name));
            dialog.setButtonIcons("download", "cancel");
            if (dialog.showDialog().getValue() != 1)
                throw new PluginDownloadException(tr("Download skipped"));
        }
        try {
            if (pi.downloadlink == null) {
                String msg = tr("Cannot download plugin ''{0}''. Its download link is not known. Skipping download.", pi.name);
                Logging.warn(msg);
                throw new PluginDownloadException(msg);
            }
            URL url = new URL(pi.downloadlink);
            Logging.debug("Download plugin {0} from {1}...", pi.name, url);
            if ("https".equals(url.getProtocol()) || "http".equals(url.getProtocol())) {
                synchronized (this) {
                    downloadConnection = HttpClient.create(url).setAccept(PLUGIN_MIME_TYPES);
                    downloadConnection.connect();
                }
                try (InputStream in = downloadConnection.getResponse().getContent()) {
                    Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } else {
                // this is an alternative for e.g. file:// URLs where HttpClient doesn't work
                try (InputStream in = url.openConnection().getInputStream()) {
                    Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (MalformedURLException e) {
            String msg = tr("Cannot download plugin ''{0}''. Its download link ''{1}'' is not a valid URL. Skipping download.",
                    pi.name, pi.downloadlink);
            Logging.warn(msg);
            throw new PluginDownloadException(msg, e);
        } catch (IOException e) {
            if (canceled)
                return;
            throw new PluginDownloadException(e);
        } finally {
            synchronized (this) {
                downloadConnection = null;
            }
        }
    }

    @Override
    protected void realRun() throws SAXException, IOException {
        File pluginDir = Preferences.main().getPluginsDirectory();
        if (!pluginDir.exists() && !pluginDir.mkdirs()) {
            String message = tr("Failed to create plugin directory ''{0}''", pluginDir.toString());
            lastException = new PluginDownloadException(message);
            Logging.error(message);
            failed.addAll(toUpdate);
            return;
        }
        getProgressMonitor().setTicksCount(toUpdate.size());
        for (PluginInformation d : toUpdate) {
            if (canceled)
                return;
            String message = tr("Downloading Plugin {0}...", d.name);
            Logging.info(message);
            progressMonitor.subTask(message);
            progressMonitor.worked(1);
            File pluginFile = new File(pluginDir, d.name + ".jar.new");
            try {
                download(d, pluginFile);
            } catch (PluginDownloadException e) {
                lastException = e;
                Logging.error(e);
                failed.add(d);
                continue;
            }
            downloaded.add(d);
        }
        PluginHandler.installDownloadedPlugins(toUpdate, false);
    }

    /**
     * Replies true if the task was canceled by the user
     *
     * @return <code>true</code> if the task was stopped by the user
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Replies the list of plugins whose download has failed.
     *
     * @return the list of plugins whose download has failed
     */
    public Collection<PluginInformation> getFailedPlugins() {
        return failed;
    }

    /**
     * Replies the list of successfully downloaded plugins.
     *
     * @return the list of successfully downloaded plugins
     */
    public Collection<PluginInformation> getDownloadedPlugins() {
        return downloaded;
    }

    /**
     * Replies the last exception that occurred during download, or {@code null}.
     * @return the last exception that occurred during download, or {@code null}
     * @since 9621
     */
    public Exception getLastException() {
        return lastException;
    }
}
