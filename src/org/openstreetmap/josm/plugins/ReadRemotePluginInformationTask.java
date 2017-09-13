// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.SAXException;

/**
 * An asynchronous task for downloading plugin lists from the configured plugin download sites.
 * @since 2817
 */
public class ReadRemotePluginInformationTask extends PleaseWaitRunnable {

    private Collection<String> sites;
    private boolean canceled;
    private HttpClient connection;
    private List<PluginInformation> availablePlugins;
    private boolean displayErrMsg;

    protected final void init(Collection<String> sites, boolean displayErrMsg) {
        this.sites = Optional.ofNullable(sites).orElseGet(Collections::emptySet);
        this.availablePlugins = new LinkedList<>();
        this.displayErrMsg = displayErrMsg;
    }

    /**
     * Constructs a new {@code ReadRemotePluginInformationTask}.
     *
     * @param sites the collection of download sites. Defaults to the empty collection if null.
     */
    public ReadRemotePluginInformationTask(Collection<String> sites) {
        super(tr("Download plugin list..."), false /* don't ignore exceptions */);
        init(sites, true);
    }

    /**
     * Constructs a new {@code ReadRemotePluginInformationTask}.
     *
     * @param monitor the progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null
     * @param sites the collection of download sites. Defaults to the empty collection if null.
     * @param displayErrMsg if {@code true}, a blocking error message is displayed in case of I/O exception.
     */
    public ReadRemotePluginInformationTask(ProgressMonitor monitor, Collection<String> sites, boolean displayErrMsg) {
        super(tr("Download plugin list..."), monitor == null ? NullProgressMonitor.INSTANCE : monitor, false /* don't ignore exceptions */);
        init(sites, displayErrMsg);
    }

    @Override
    protected void cancel() {
        canceled = true;
        synchronized (this) {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    protected void finish() {
        // Do nothing
    }

    /**
     * Creates the file name for the cached plugin list and the icon cache file.
     *
     * @param pluginDir directory of plugin for data storage
     * @param site the name of the site
     * @return the file name for the cache file
     */
    protected File createSiteCacheFile(File pluginDir, String site) {
        String name;
        try {
            site = site.replaceAll("%<(.*)>", "");
            URL url = new URL(site);
            StringBuilder sb = new StringBuilder();
            sb.append("site-")
              .append(url.getHost()).append('-');
            if (url.getPort() != -1) {
                sb.append(url.getPort()).append('-');
            }
            String path = url.getPath();
            for (int i = 0; i < path.length(); i++) {
                char c = path.charAt(i);
                if (Character.isLetterOrDigit(c)) {
                    sb.append(c);
                } else {
                    sb.append('_');
                }
            }
            sb.append(".txt");
            name = sb.toString();
        } catch (MalformedURLException e) {
            name = "site-unknown.txt";
        }
        return new File(pluginDir, name);
    }

    /**
     * Downloads the list from a remote location
     *
     * @param site the site URL
     * @param monitor a progress monitor
     * @return the downloaded list
     */
    protected String downloadPluginList(String site, final ProgressMonitor monitor) {
        /* replace %<x> with empty string or x=plugins (separated with comma) */
        String pl = Utils.join(",", Config.getPref().getList("plugins"));
        String printsite = site.replaceAll("%<(.*)>", "");
        if (pl != null && !pl.isEmpty()) {
            site = site.replaceAll("%<(.*)>", "$1"+pl);
        } else {
            site = printsite;
        }

        String content = null;
        try {
            monitor.beginTask("");
            monitor.indeterminateSubTask(tr("Downloading plugin list from ''{0}''", printsite));

            final URL url = new URL(site);
            if ("https".equals(url.getProtocol()) || "http".equals(url.getProtocol())) {
                connection = HttpClient.create(url).useCache(false);
                final HttpClient.Response response = connection.connect();
                content = response.fetchContent();
                if (response.getResponseCode() != 200) {
                    throw new IOException(tr("Unsuccessful HTTP request"));
                }
                return content;
            } else {
                // e.g. when downloading from a file:// URL, we can't use HttpClient
                try (InputStreamReader in = new InputStreamReader(url.openConnection().getInputStream(), StandardCharsets.UTF_8)) {
                    final StringBuilder sb = new StringBuilder();
                    final char[] buffer = new char[8192];
                    int numChars;
                    while ((numChars = in.read(buffer)) >= 0) {
                        sb.append(buffer, 0, numChars);
                        if (canceled) {
                            return null;
                        }
                    }
                    return sb.toString();
                }
            }

        } catch (MalformedURLException e) {
            if (canceled) return null;
            Logging.error(e);
            return null;
        } catch (IOException e) {
            if (canceled) return null;
            handleIOException(monitor, e, content);
            return null;
        } finally {
            synchronized (this) {
                if (connection != null) {
                    connection.disconnect();
                }
                connection = null;
            }
            monitor.finishTask();
        }
    }

    private void handleIOException(final ProgressMonitor monitor, IOException e, String details) {
        final String msg = e.getMessage();
        if (details == null || details.isEmpty()) {
            Logging.error(e.getClass().getSimpleName()+": " + msg);
        } else {
            Logging.error(msg + " - Details:\n" + details);
        }

        if (displayErrMsg) {
            displayErrorMessage(monitor, msg, details, tr("Plugin list download error"), tr("JOSM failed to download plugin list:"));
        }
    }

    private static void displayErrorMessage(final ProgressMonitor monitor, final String msg, final String details, final String title,
            final String firstMessage) {
        GuiHelper.runInEDTAndWait(() -> {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.add(new JLabel(firstMessage), GBC.eol().insets(0, 0, 0, 10));
            StringBuilder b = new StringBuilder();
            for (String part : msg.split("(?<=\\G.{200})")) {
                b.append(part).append('\n');
            }
            panel.add(new JLabel("<html><body width=\"500\"><b>"+b.toString().trim()+"</b></body></html>"), GBC.eol().insets(0, 0, 0, 10));
            if (details != null && !details.isEmpty()) {
                panel.add(new JLabel(tr("Details:")), GBC.eol().insets(0, 0, 0, 10));
                JosmTextArea area = new JosmTextArea(details);
                area.setEditable(false);
                area.setLineWrap(true);
                area.setWrapStyleWord(true);
                JScrollPane scrollPane = new JScrollPane(area);
                scrollPane.setPreferredSize(new Dimension(500, 300));
                panel.add(scrollPane, GBC.eol().fill());
            }
            JOptionPane.showMessageDialog(monitor.getWindowParent(), panel, title, JOptionPane.ERROR_MESSAGE);
        });
    }

    /**
     * Writes the list of plugins to a cache file
     *
     * @param site the site from where the list was downloaded
     * @param list the downloaded list
     */
    protected void cachePluginList(String site, String list) {
        File pluginDir = Main.pref.getPluginsDirectory();
        if (!pluginDir.exists() && !pluginDir.mkdirs()) {
            Logging.warn(tr("Failed to create plugin directory ''{0}''. Cannot cache plugin list from plugin site ''{1}''.",
                    pluginDir.toString(), site));
        }
        File cacheFile = createSiteCacheFile(pluginDir, site);
        getProgressMonitor().subTask(tr("Writing plugin list to local cache ''{0}''", cacheFile.toString()));
        try (PrintWriter writer = new PrintWriter(cacheFile, StandardCharsets.UTF_8.name())) {
            writer.write(list);
            writer.flush();
        } catch (IOException e) {
            // just failed to write the cache file. No big deal, but log the exception anyway
            Logging.error(e);
        }
    }

    /**
     * Filter information about deprecated plugins from the list of downloaded
     * plugins
     *
     * @param plugins the plugin informations
     * @return the plugin informations, without deprecated plugins
     */
    protected List<PluginInformation> filterDeprecatedPlugins(List<PluginInformation> plugins) {
        List<PluginInformation> ret = new ArrayList<>(plugins.size());
        Set<String> deprecatedPluginNames = new HashSet<>();
        for (PluginHandler.DeprecatedPlugin p : PluginHandler.DEPRECATED_PLUGINS) {
            deprecatedPluginNames.add(p.name);
        }
        for (PluginInformation plugin: plugins) {
            if (deprecatedPluginNames.contains(plugin.name)) {
                continue;
            }
            ret.add(plugin);
        }
        return ret;
    }

    /**
     * Parses the plugin list
     *
     * @param site the site from where the list was downloaded
     * @param doc the document with the plugin list
     */
    protected void parsePluginListDocument(String site, String doc) {
        try {
            getProgressMonitor().subTask(tr("Parsing plugin list from site ''{0}''", site));
            InputStream in = new ByteArrayInputStream(doc.getBytes(StandardCharsets.UTF_8));
            List<PluginInformation> pis = new PluginListParser().parse(in);
            availablePlugins.addAll(filterDeprecatedPlugins(pis));
        } catch (PluginListParseException e) {
            Logging.error(tr("Failed to parse plugin list document from site ''{0}''. Skipping site. Exception was: {1}", site, e.toString()));
            Logging.error(e);
        }
    }

    @Override
    protected void realRun() throws SAXException, IOException, OsmTransferException {
        if (sites == null) return;
        getProgressMonitor().setTicksCount(sites.size() * 3);

        // collect old cache files and remove if no longer in use
        List<File> siteCacheFiles = new LinkedList<>();
        for (String location : PluginInformation.getPluginLocations()) {
            File[] f = new File(location).listFiles(
                    (FilenameFilter) (dir, name) -> name.matches("^([0-9]+-)?site.*\\.txt$") ||
                                                    name.matches("^([0-9]+-)?site.*-icons\\.zip$")
            );
            if (f != null && f.length > 0) {
                siteCacheFiles.addAll(Arrays.asList(f));
            }
        }

        File pluginDir = Main.pref.getPluginsDirectory();
        for (String site: sites) {
            String printsite = site.replaceAll("%<(.*)>", "");
            getProgressMonitor().subTask(tr("Processing plugin list from site ''{0}''", printsite));
            String list = downloadPluginList(site, getProgressMonitor().createSubTaskMonitor(0, false));
            if (canceled) return;
            siteCacheFiles.remove(createSiteCacheFile(pluginDir, site));
            if (list != null) {
                getProgressMonitor().worked(1);
                cachePluginList(site, list);
                if (canceled) return;
                getProgressMonitor().worked(1);
                parsePluginListDocument(site, list);
                if (canceled) return;
                getProgressMonitor().worked(1);
                if (canceled) return;
            }
        }
        // remove old stuff or whole update process is broken
        for (File file: siteCacheFiles) {
            Utils.deleteFile(file);
        }
    }

    /**
     * Replies true if the task was canceled
     * @return <code>true</code> if the task was stopped by the user
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Replies the list of plugins described in the downloaded plugin lists
     *
     * @return  the list of plugins
     * @since 5601
     */
    public List<PluginInformation> getAvailablePlugins() {
        return availablePlugins;
    }
}
