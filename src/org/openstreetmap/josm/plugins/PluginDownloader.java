//License: GPL. Copyright 2007 by Immanuel Scholz and others
/**
 *
 */
package org.openstreetmap.josm.plugins;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.preferences.PluginPreference.PluginDescription;
import org.xml.sax.SAXException;

public class PluginDownloader {

    private static final class UpdateTask extends PleaseWaitRunnable {
        private final Collection<PluginDescription> toUpdate;
        private String errors = "";
        private int count = 0;

        private UpdateTask(Collection<PluginDescription> toUpdate) {
            super(tr("Update Plugins"));
            this.toUpdate = toUpdate;
        }

        @Override protected void cancel() {
            finish();
        }

        @Override protected void finish() {
            if (errors.length() > 0)
                JOptionPane.showMessageDialog(Main.parent, tr("There were problems with the following plugins:\n\n {0}",errors));
            else
                JOptionPane.showMessageDialog(Main.parent, trn("{0} Plugin successfully updated. Please restart JOSM.", "{0} Plugins successfully updated. Please restart JOSM.", count, count));
        }

        @Override protected void realRun() throws SAXException, IOException {
            File pluginDir = Main.pref.getPluginsDirFile();
            if (!pluginDir.exists())
                pluginDir.mkdirs();
            for (PluginDescription d : toUpdate) {
                File pluginFile = new File(pluginDir, d.name + ".jar.new");
                if (download(d.resource, pluginFile))
                    count++;
                else
                    errors += d.name + "\n";
            }
            PluginDownloader.moveUpdatedPlugins();
        }
    }

    private static final Pattern wiki = Pattern.compile("^</td></tr><tr><td><a class=\"ext-link\" href=\"([^\"]*)\"><span class=\"icon\">([^<]*)</span></a></td><td>([^<]*)</td><td>([^<].*)</td><td>(.*)");

    private final static String[] pluginSites = {"http://josm.openstreetmap.de/wiki/Plugins"};

    public static Collection<String> getSites() {
        return Main.pref.getCollection("pluginmanager.sites", Arrays.asList(pluginSites));
    }

    public static int downloadDescription() {
        int count = 0;
        for (String site : getSites()) {
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(new URL(site).openStream()));
                CharSequence txt;
                if (site.toLowerCase().endsWith(".xml"))
                    txt = readXml(r);
                else
                    txt = readWiki(r);
                r.close();
                new File(Main.pref.getPreferencesDir()+"plugins").mkdir();
                FileWriter out = new FileWriter(new File(Main.pref
                        .getPluginsDirFile(), count + "-site-"
                        + site.replaceAll("[/:\\\\ <>|]", "_") + ".xml"));
                out.append(txt);
                out.close();
                count++;
            } catch (IOException x) {
            }
        }
        return count;
    }

    private static CharSequence readXml(BufferedReader r) throws IOException {
        StringBuilder b = new StringBuilder();
        for (String line = r.readLine(); line != null; line = r.readLine())
            b.append(line+"\n");
        return b;
    }

    private static CharSequence readWiki(BufferedReader r) throws IOException {
        StringBuilder b = new StringBuilder("<plugins>\n");
        for (String line = r.readLine(); line != null; line = r.readLine()) {
            Matcher m = wiki.matcher(line);
            if (!m.matches())
                continue;
            b.append("  <plugin>\n");
            b.append("    <name>"+escape(m.group(2))+"</name>\n");
            b.append("    <resource>"+escape(m.group(1))+"</resource>\n");
            b.append("    <author>"+escape(m.group(3))+"</author>\n");
            b.append("    <description>"+escape(m.group(4))+"</description>\n");
            b.append("    <version>"+escape(m.group(5))+"</version>\n");
            b.append("  </plugin>\n");
        }
        b.append("</plugins>\n");
        return b;
    }

    private static String escape(String s) {
        return s.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    }

    public static boolean downloadPlugin(PluginDescription pd) {
        File file = new File(Main.pref.getPluginsDirFile(), pd.name + ".jar");
        if (!download(pd.resource, file)) {
            JOptionPane.showMessageDialog(Main.parent, tr("Could not download plugin: {0} from {1}", pd.name, pd.resource));
        } else {
            try {
                PluginInformation.findPlugin(pd.name);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(Main.parent, tr("The plugin {0} seem to be broken or could not be downloaded automatically.", pd.name));
            }
        }
        if (file.exists())
            file.delete();
        return false;
    }

    private static boolean download(String url, File file) {
        try {
            InputStream in = new URL(url).openStream();
            OutputStream out = new FileOutputStream(file);
            byte[] buffer = new byte[8192];
            for (int read = in.read(buffer); read != -1; read = in.read(buffer))
                out.write(buffer, 0, read);
            out.close();
            in.close();
            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void update(Collection<PluginDescription> update) {
        Main.worker.execute(new UpdateTask(update));
    }

    public static boolean moveUpdatedPlugins() {
        File pluginDir = Main.pref.getPluginsDirFile();
        boolean ok = true;
        if (pluginDir.exists() && pluginDir.isDirectory() && pluginDir.canWrite()) {
            final File[] files = pluginDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".new");
                }});
            for (File updatedPlugin : files) {
                final String filePath = updatedPlugin.getPath();
                File plugin = new File(filePath.substring(0, filePath.length() - 4));
                ok = (plugin.delete() || !plugin.exists()) && updatedPlugin.renameTo(plugin) && ok;
            }
        }
        return ok;
    }
}
