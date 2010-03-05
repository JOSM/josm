// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
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

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmTransferException;
import org.xml.sax.SAXException;

/**
 * An asynchronous task for downloading plugin lists from the configured plugin download
 * sites.
 *
 */
public class ReadRemotePluginInformationTask extends PleaseWaitRunnable{

    private Collection<String> sites;
    private boolean canceled;
    private HttpURLConnection connection;
    private List<PluginInformation> availabePlugins;

    /**
     * Creates the task
     * 
     * @param sites the collection of download sites. Defaults to the empty collection if null.
     */
    public ReadRemotePluginInformationTask(Collection<String> sites) {
        super(tr("Download plugin list..."), false /* don't ignore exceptions */);
        this.sites = sites;
        if (sites == null) {
            this.sites = Collections.emptySet();
        }
        availabePlugins = new LinkedList<PluginInformation>();
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
     * Creates the file name for the cached plugin list.
     * 
     * @param site the name of the site
     * @return the file name for the cache file
     */
    protected String createSiteCacheFileName(String site) {
        try {
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
            sb.append(".txt");
            return sb.toString();
        } catch(MalformedURLException e) {
            return "site-unknown.txt";
        }
    }

    /**
     * Downloads the list from a remote location
     * 
     * @param site the site URL
     * @param monitor a progress monitor
     * @return the downloaded list
     */
    protected String downloadPluginList(String site, ProgressMonitor monitor) {
        BufferedReader in = null;
        StringBuilder sb = new StringBuilder();
        try {
            monitor.beginTask("");
            monitor.indeterminateSubTask(tr("Downloading plugin list from ''{0}''", site));

            URL url = new URL(site);
            synchronized(this) {
                connection = (HttpURLConnection)url.openConnection();
                connection.setRequestProperty("Cache-Control", "no-cache");
                connection.setRequestProperty("User-Agent",Version.getInstance().getAgentString());
                connection.setRequestProperty("Host", url.getHost());
                connection.setRequestProperty("Accept-Charset", "utf-8");
            }
            in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            String line;
            while((line = in.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch(MalformedURLException e) {
            if (canceled) return null;
            e.printStackTrace();
            return null;
        } catch(IOException e) {
            if (canceled) return null;
            e.printStackTrace();
            return null;
        } finally {
            synchronized(this) {
                if (connection != null) {
                    connection.disconnect();
                }
                connection = null;
            }
            if (in != null) {
                try {
                    in.close();
                } catch(IOException e){/* ignore */}
            }
            monitor.finishTask();
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
                    System.err.println(tr("Warning: failed to create plugin directory ''{0}''. Cannot cache plugin list from plugin site ''{1}''.", pluginDir.toString(), site));
                }
            }
            File cacheFile = new File(pluginDir, createSiteCacheFileName(site));
            getProgressMonitor().subTask(tr("Writing plugin list to local cache ''{0}''", cacheFile.toString()));
            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(cacheFile), "utf-8"));
            writer.write(list);
        } catch(IOException e) {
            // just failed to write the cache file. No big deal, but log the exception anyway
            e.printStackTrace();
        } finally {
            if (writer != null) {
                writer.flush();
                writer.close();
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
        HashSet<String> deprecatedPluginNames = new HashSet<String>(Arrays.asList(PluginHandler.DEPRECATED_PLUGINS));
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
            InputStream in = new ByteArrayInputStream(doc.getBytes("UTF-8"));
            List<PluginInformation> pis = new PluginListParser().parse(in);
            availabePlugins.addAll(filterDeprecatedPlugins(pis));
        } catch(UnsupportedEncodingException e) {
            System.err.println(tr("Failed to parse plugin list document from site ''{0}''. Skipping site. Exception was: {1}", site, e.toString()));
            e.printStackTrace();
        } catch(PluginListParseException e) {
            System.err.println(tr("Failed to parse plugin list document from site ''{0}''. Skipping site. Exception was: {1}", site, e.toString()));
            e.printStackTrace();
        }
    }

    @Override
    protected void realRun() throws SAXException, IOException, OsmTransferException {
        if (sites == null) return;
        getProgressMonitor().setTicksCount(sites.size() * 3);
        for (String site: sites) {
            getProgressMonitor().subTask(tr("Processing plugin list from site ''{0}''", site));
            String list = downloadPluginList(site, getProgressMonitor().createSubTaskMonitor(0, false));
            if (canceled) return;
            getProgressMonitor().worked(1);
            cachePluginList(site, list);
            if (canceled) return;
            getProgressMonitor().worked(1);
            parsePluginListDocument(site, list);
            if (canceled) return;
            getProgressMonitor().worked(1);
        }
    }

    /**
     * Replies true if the task was canceled
     * @return
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Replies the list of plugins described in the downloaded plugin lists
     * 
     * @return  the list of plugins
     */
    public List<PluginInformation> getAvailabePlugins() {
        return availabePlugins;
    }
}
