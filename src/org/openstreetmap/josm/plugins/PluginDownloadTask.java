// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.SAXException;


/**
 * Asynchronous task for downloading a collection of plugins.
 *
 * When the task is finished {@link #getDownloadedPlugins()} replies the list of downloaded plugins
 * and {@link #getFailedPlugins()} replies the list of failed plugins.
 *
 */
public class PluginDownloadTask extends PleaseWaitRunnable{
    private final Collection<PluginInformation> toUpdate = new LinkedList<PluginInformation>();
    private final Collection<PluginInformation> failed = new LinkedList<PluginInformation>();
    private final Collection<PluginInformation> downloaded = new LinkedList<PluginInformation>();
    private Exception lastException;
    private boolean canceled;
    private HttpURLConnection downloadConnection;

    /**
     * Creates the download task
     *
     * @param parent the parent component relative to which the {@link org.openstreetmap.josm.gui.PleaseWaitDialog} is displayed
     * @param toUpdate a collection of plugin descriptions for plugins to update/download. Must not be null.
     * @param title the title to display in the {@link org.openstreetmap.josm.gui.PleaseWaitDialog}
     * @throws IllegalArgumentException thrown if toUpdate is null
     */
    public PluginDownloadTask(Component parent, Collection<PluginInformation> toUpdate, String title) throws IllegalArgumentException{
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
     * @throws IllegalArgumentException thrown if toUpdate is null
     */
    public PluginDownloadTask(ProgressMonitor monitor, Collection<PluginInformation> toUpdate, String title) {
        super(title, monitor == null? NullProgressMonitor.INSTANCE: monitor, false /* don't ignore exceptions */);
        CheckParameterUtil.ensureParameterNotNull(toUpdate, "toUpdate");
        this.toUpdate.addAll(toUpdate);
    }

    /**
     * Sets the collection of plugins to update.
     *
     * @param toUpdate the collection of plugins to update. Must not be null.
     * @throws IllegalArgumentException thrown if toUpdate is null
     */
    public void setPluginsToDownload(Collection<PluginInformation> toUpdate) throws IllegalArgumentException{
        CheckParameterUtil.ensureParameterNotNull(toUpdate, "toUpdate");
        this.toUpdate.clear();
        this.toUpdate.addAll(toUpdate);
    }

    @Override protected void cancel() {
        this.canceled = true;
        synchronized(this) {
            if (downloadConnection != null) {
                downloadConnection.disconnect();
            }
        }
    }

    @Override protected void finish() {}

    protected void download(PluginInformation pi, File file) throws PluginDownloadException{
        if (pi.mainversion > Version.getInstance().getVersion()) {
            ExtendedDialog dialog = new ExtendedDialog(
                    progressMonitor.getWindowParent(),
                    tr("Skip download"),
                    new String[] {
                        tr("Download Plugin"),
                        tr("Skip Download") }
            );
            dialog.setContent(tr("JOSM version {0} required for plugin {1}.", pi.mainversion, pi.name));
            dialog.setButtonIcons(new String[] { "download.png", "cancel.png" });
            dialog.showDialog();
            int answer = dialog.getValue();
            if (answer != 1)
                throw new PluginDownloadException(tr("Download skipped"));
        }
        OutputStream out = null;
        InputStream in = null;
        try {
            if (pi.downloadlink == null) {
                String msg = tr("Cannot download plugin ''{0}''. Its download link is not known. Skipping download.", pi.name);
                Main.warn(msg);
                throw new PluginDownloadException(msg);
            }
            URL url = new URL(pi.downloadlink);
            synchronized(this) {
                downloadConnection = MirroredInputStream.connectFollowingRedirect(url);
            }
            in = downloadConnection.getInputStream();
            out = new FileOutputStream(file);
            byte[] buffer = new byte[8192];
            for (int read = in.read(buffer); read != -1; read = in.read(buffer)) {
                out.write(buffer, 0, read);
            }
        } catch (MalformedURLException e) {
            String msg = tr("Cannot download plugin ''{0}''. Its download link ''{1}'' is not a valid URL. Skipping download.", pi.name, pi.downloadlink);
            Main.warn(msg);
            throw new PluginDownloadException(msg);
        } catch (IOException e) {
            if (canceled)
                return;
            throw new PluginDownloadException(e);
        } finally {
            Utils.close(in);
            synchronized(this) {
                downloadConnection = null;
            }
            Utils.close(out);
        }
    }

    @Override protected void realRun() throws SAXException, IOException {
        File pluginDir = Main.pref.getPluginsDirectory();
        if (!pluginDir.exists()) {
            if (!pluginDir.mkdirs()) {
                lastException = new PluginDownloadException(tr("Failed to create plugin directory ''{0}''", pluginDir.toString()));
                failed.addAll(toUpdate);
                return;
            }
        }
        getProgressMonitor().setTicksCount(toUpdate.size());
        for (PluginInformation d : toUpdate) {
            if (canceled) return;
            progressMonitor.subTask(tr("Downloading Plugin {0}...", d.name));
            progressMonitor.worked(1);
            File pluginFile = new File(pluginDir, d.name + ".jar.new");
            try {
                download(d, pluginFile);
            } catch(PluginDownloadException e) {
                Main.error(e);
                failed.add(d);
                continue;
            }
            downloaded.add(d);
        }
        PluginHandler.installDownloadedPlugins(false);
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
     * Replies the list of successfully downloaded plugins
     *
     * @return the list of successfully downloaded plugins
     */
    public Collection<PluginInformation> getFailedPlugins() {
        return failed;
    }

    /**
     * Replies the list of plugins whose download has failed
     *
     * @return the list of plugins whose download has failed
     */
    public Collection<PluginInformation> getDownloadedPlugins() {
        return downloaded;
    }
}
