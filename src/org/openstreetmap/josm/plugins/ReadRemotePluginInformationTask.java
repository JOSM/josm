// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

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
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.SAXException;

/**
 * An asynchronous task for downloading plugin lists from the configured plugin download sites.
 * @since 2817
 */
public class ReadRemotePluginInformationTask extends PleaseWaitRunnable {

    private Collection<String> sites;
    private boolean canceled;
    private HttpURLConnection connection;
    private List<PluginInformation> availablePlugins;
    private boolean displayErrMsg;

    protected enum CacheType {PLUGIN_LIST, ICON_LIST}

    protected void init(Collection<String> sites, boolean displayErrMsg){
        this.sites = sites;
        if (sites == null) {
            this.sites = Collections.emptySet();
        }
        this.availablePlugins = new LinkedList<PluginInformation>();
        this.displayErrMsg = displayErrMsg;
    }
    /**
     * Creates the task
     *
     * @param sites the collection of download sites. Defaults to the empty collection if null.
     */
    public ReadRemotePluginInformationTask(Collection<String> sites) {
        super(tr("Download plugin list..."), false /* don't ignore exceptions */);
        init(sites, true);
    }

    /**
     * Creates the task
     *
     * @param monitor the progress monitor. Defaults to {@link NullProgressMonitor#INSTANCE} if null
     * @param sites the collection of download sites. Defaults to the empty collection if null.
     * @param displayErrMsg if {@code true}, a blocking error message is displayed in case of I/O exception.
     */
    public ReadRemotePluginInformationTask(ProgressMonitor monitor, Collection<String> sites, boolean displayErrMsg) {
        super(tr("Download plugin list..."), monitor == null ? NullProgressMonitor.INSTANCE: monitor, false /* don't ignore exceptions */);
        init(sites, displayErrMsg);
    }


    @Override
    protected void cancel() {
        canceled = true;
        synchronized(this) {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    protected void finish() {}

    /**
     * Creates the file name for the cached plugin list and the icon cache
     * file.
     *
     * @param site the name of the site
     * @param type icon cache or plugin list cache
     * @return the file name for the cache file
     */
    protected File createSiteCacheFile(File pluginDir, String site, CacheType type) {
        String name;
        try {
            site = site.replaceAll("%<(.*)>", "");
            URL url = new URL(site);
            StringBuilder sb = new StringBuilder();
            sb.append("site-");
            sb.append(url.getHost()).append("-");
            if (url.getPort() != -1) {
                sb.append(url.getPort()).append("-");
            }
            String path = url.getPath();
            for (int i =0;i<path.length(); i++) {
                char c = path.charAt(i);
                if (Character.isLetterOrDigit(c)) {
                    sb.append(c);
                } else {
                    sb.append("_");
                }
            }
            switch (type) {
            case PLUGIN_LIST:
                sb.append(".txt");
                break;
            case ICON_LIST:
                sb.append("-icons.zip");
                break;
            }
            name = sb.toString();
        } catch(MalformedURLException e) {
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
        BufferedReader in = null;

        /* replace %<x> with empty string or x=plugins (separated with comma) */
        String pl = Utils.join(",", Main.pref.getCollection("plugins"));
        String printsite = site.replaceAll("%<(.*)>", "");
        if (pl != null && pl.length() != 0) {
            site = site.replaceAll("%<(.*)>", "$1"+pl);
        } else {
            site = printsite;
        }

        try {
            monitor.beginTask("");
            monitor.indeterminateSubTask(tr("Downloading plugin list from ''{0}''", printsite));

            URL url = new URL(site);
            synchronized(this) {
                connection = Utils.openHttpConnection(url);
                connection.setRequestProperty("Cache-Control", "no-cache");
                connection.setRequestProperty("Accept-Charset", "utf-8");
            }
            in = new BufferedReader(new InputStreamReader(connection.getInputStream(), Utils.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (MalformedURLException e) {
            if (canceled) return null;
            Main.error(e);
            return null;
        } catch (IOException e) {
            if (canceled) return null;
            Main.addNetworkError(site, e);
            handleIOException(monitor, e, tr("Plugin list download error"), tr("JOSM failed to download plugin list:"), displayErrMsg);
            return null;
        } finally {
            synchronized(this) {
                if (connection != null) {
                    connection.disconnect();
                }
                connection = null;
            }
            Utils.close(in);
            monitor.finishTask();
        }
    }

    private void handleIOException(final ProgressMonitor monitor, IOException e, final String title, final String firstMessage, boolean displayMsg) {
        InputStream errStream = connection.getErrorStream();
        StringBuilder sb = new StringBuilder();
        if (errStream != null) {
            BufferedReader err = null;
            try {
                String line;
                err = new BufferedReader(new InputStreamReader(errStream, Utils.UTF_8));
                while ((line = err.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } catch (Exception ex) {
                Main.error(e);
                Main.error(ex);
            } finally {
                Utils.close(err);
            }
        }
        final String msg = e.getMessage();
        final String details = sb.toString();
        if (details.isEmpty()) {
            Main.error(e.getClass().getSimpleName()+": " + msg);
        } else {
            Main.error(msg + " - Details:\n" + details);
        }

        if (displayMsg) {
            displayErrorMessage(monitor, msg, details, title, firstMessage);
        }
    }

    private void displayErrorMessage(final ProgressMonitor monitor, final String msg, final String details, final String title, final String firstMessage) {
        GuiHelper.runInEDTAndWait(new Runnable() {
            @Override public void run() {
                JPanel panel = new JPanel(new GridBagLayout());
                panel.add(new JLabel(firstMessage), GBC.eol().insets(0, 0, 0, 10));
                StringBuilder b = new StringBuilder();
                for (String part : msg.split("(?<=\\G.{200})")) {
                    b.append(part).append("\n");
                }
                panel.add(new JLabel("<html><body width=\"500\"><b>"+b.toString().trim()+"</b></body></html>"), GBC.eol().insets(0, 0, 0, 10));
                if (!details.isEmpty()) {
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
            }
        });
    }

    /**
     * Downloads the icon archive from a remote location
     *
     * @param site the site URL
     * @param monitor a progress monitor
     */
    protected void downloadPluginIcons(String site, File destFile, ProgressMonitor monitor) {
        InputStream in = null;
        OutputStream out = null;
        try {
            site = site.replaceAll("%<(.*)>", "");

            monitor.beginTask("");
            monitor.indeterminateSubTask(tr("Downloading plugin list from ''{0}''", site));

            URL url = new URL(site);
            synchronized(this) {
                connection = Utils.openHttpConnection(url);
                connection.setRequestProperty("Cache-Control", "no-cache");
            }
            in = connection.getInputStream();
            out = new FileOutputStream(destFile);
            byte[] buffer = new byte[8192];
            for (int read = in.read(buffer); read != -1; read = in.read(buffer)) {
                out.write(buffer, 0, read);
            }
        } catch (MalformedURLException e) {
            if (canceled) return;
            Main.error(e);
            return;
        } catch (IOException e) {
            if (canceled) return;
            handleIOException(monitor, e, tr("Plugin icons download error"), tr("JOSM failed to download plugin icons:"), displayErrMsg);
            return;
        } finally {
            Utils.close(out);
            synchronized(this) {
                if (connection != null) {
                    connection.disconnect();
                }
                connection = null;
            }
            Utils.close(in);
            monitor.finishTask();
        }
        for (PluginInformation pi : availablePlugins) {
            if (pi.icon == null && pi.iconPath != null) {
                pi.icon = new ImageProvider(pi.name+".jar/"+pi.iconPath)
                                .setArchive(destFile)
                                .setMaxWidth(24)
                                .setMaxHeight(24)
                                .setOptional(true).get();
            }
        }
    }

    /**
     * Writes the list of plugins to a cache file
     *
     * @param site the site from where the list was downloaded
     * @param list the downloaded list
     */
    protected void cachePluginList(String site, String list) {
        PrintWriter writer = null;
        try {
            File pluginDir = Main.pref.getPluginsDirectory();
            if (!pluginDir.exists()) {
                if (! pluginDir.mkdirs()) {
                    Main.warn(tr("Failed to create plugin directory ''{0}''. Cannot cache plugin list from plugin site ''{1}''.", pluginDir.toString(), site));
                }
            }
            File cacheFile = createSiteCacheFile(pluginDir, site, CacheType.PLUGIN_LIST);
            getProgressMonitor().subTask(tr("Writing plugin list to local cache ''{0}''", cacheFile.toString()));
            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(cacheFile), Utils.UTF_8));
            writer.write(list);
        } catch(IOException e) {
            // just failed to write the cache file. No big deal, but log the exception anyway
            Main.error(e);
        } finally {
            if (writer != null) {
                writer.flush();
                Utils.close(writer);
            }
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
        List<PluginInformation> ret = new ArrayList<PluginInformation>(plugins.size());
        HashSet<String> deprecatedPluginNames = new HashSet<String>();
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
            InputStream in = new ByteArrayInputStream(doc.getBytes(Utils.UTF_8));
            List<PluginInformation> pis = new PluginListParser().parse(in);
            availablePlugins.addAll(filterDeprecatedPlugins(pis));
        } catch (PluginListParseException e) {
            Main.error(tr("Failed to parse plugin list document from site ''{0}''. Skipping site. Exception was: {1}", site, e.toString()));
            Main.error(e);
        }
    }

    @Override
    protected void realRun() throws SAXException, IOException, OsmTransferException {
        if (sites == null) return;
        getProgressMonitor().setTicksCount(sites.size() * 3);
        File pluginDir = Main.pref.getPluginsDirectory();

        // collect old cache files and remove if no longer in use
        List<File> siteCacheFiles = new LinkedList<File>();
        for (String location : PluginInformation.getPluginLocations()) {
            File [] f = new File(location).listFiles(
                    new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            return name.matches("^([0-9]+-)?site.*\\.txt$") ||
                            name.matches("^([0-9]+-)?site.*-icons\\.zip$");
                        }
                    }
            );
            if(f != null && f.length > 0) {
                siteCacheFiles.addAll(Arrays.asList(f));
            }
        }

        for (String site: sites) {
            String printsite = site.replaceAll("%<(.*)>", "");
            getProgressMonitor().subTask(tr("Processing plugin list from site ''{0}''", printsite));
            String list = downloadPluginList(site, getProgressMonitor().createSubTaskMonitor(0, false));
            if (canceled) return;
            siteCacheFiles.remove(createSiteCacheFile(pluginDir, site, CacheType.PLUGIN_LIST));
            siteCacheFiles.remove(createSiteCacheFile(pluginDir, site, CacheType.ICON_LIST));
            if(list != null)
            {
                getProgressMonitor().worked(1);
                cachePluginList(site, list);
                if (canceled) return;
                getProgressMonitor().worked(1);
                parsePluginListDocument(site, list);
                if (canceled) return;
                getProgressMonitor().worked(1);
                if (canceled) return;
            }
            downloadPluginIcons(site+"-icons.zip", createSiteCacheFile(pluginDir, site, CacheType.ICON_LIST), getProgressMonitor().createSubTaskMonitor(0, false));
        }
        for (File file: siteCacheFiles) /* remove old stuff or whole update process is broken */
        {
            file.delete();
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
