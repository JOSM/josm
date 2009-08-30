//License: GPL. Copyright 2007 by Immanuel Scholz and others
/**
 *
 */
package org.openstreetmap.josm.plugins;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AboutAction;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.xml.sax.SAXException;

public class PluginDownloader {

    private static final class UpdateTask extends PleaseWaitRunnable {
        private final Collection<PluginInformation> toUpdate;
        public final Collection<PluginInformation> failed = new LinkedList<PluginInformation>();
        private String errors = "";
        private int count = 0;

        private UpdateTask(Collection<PluginInformation> toUpdate, boolean up) {
            super(up ? tr("Update Plugins") : tr("Download Plugins"));
            this.toUpdate = toUpdate;
        }

        @Override protected void cancel() {
            finish();
        }

        @Override protected void finish() {
            if (errors.length() > 0) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("There were problems with the following plugins:\n\n {0}",errors),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
            } else {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        trn("{0} Plugin successfully downloaded. Please restart JOSM.", "{0} Plugins successfully downloaded. Please restart JOSM.", count, count),
                        tr("Information"),
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        }

        @Override protected void realRun() throws SAXException, IOException {
            File pluginDir = Main.pref.getPluginsDirFile();
            if (!pluginDir.exists()) {
                pluginDir.mkdirs();
            }
            progressMonitor.setTicksCount(toUpdate.size());
            for (PluginInformation d : toUpdate) {
                progressMonitor.subTask(tr("Downloading Plugin {0}...", d.name));
                progressMonitor.worked(1);
                File pluginFile = new File(pluginDir, d.name + ".jar.new");
                if(download(d, pluginFile)) {
                    count++;
                } else
                {
                    errors += d.name + "\n";
                    failed.add(d);
                }
            }
            PluginDownloader.moveUpdatedPlugins();
        }
    }

    private final static String[] pluginSites = {"http://josm.openstreetmap.de/plugin"};

    public static Collection<String> getSites() {
        return Main.pref.getCollection("pluginmanager.sites", Arrays.asList(pluginSites));
    }
    public static void setSites(Collection<String> c) {
        Main.pref.putCollection("pluginmanager.sites", c);
    }

    public static int downloadDescription() {
        int count = 0;
        for (String site : getSites()) {
            /* TODO: remove old site files (everything except .jar) */
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(new URL(site).openStream(), "utf-8"));
                new File(Main.pref.getPreferencesDir()+"plugins").mkdir();
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(new File(Main.pref.getPluginsDirFile(),
                                count + "-site-" + site.replaceAll("[/:\\\\ <>|]", "_") + ".txt")), "utf-8"));
                for (String line = r.readLine(); line != null; line = r.readLine()) {
                    out.append(line+"\n");
                }
                r.close();
                out.close();
                count++;
            } catch (IOException x) {
            }
        }
        return count;
    }

    private static boolean download(PluginInformation pd, File file) {
        if(pd.mainversion > AboutAction.getVersionNumber())
        {
            int answer = new ExtendedDialog(Main.parent,
                    tr("Skip download"),
                    tr("JOSM version {0} required for plugin {1}.", pd.mainversion, pd.name),
                    new String[] {tr("Download Plugin"), tr("Skip Download")},
                    new String[] {"download.png", "cancel.png"}).getValue();
            if (answer != 1)
                return false;
        }

        try {
            InputStream in = new URL(pd.downloadlink).openStream();
            OutputStream out = new FileOutputStream(file);
            byte[] buffer = new byte[8192];
            for (int read = in.read(buffer); read != -1; read = in.read(buffer)) {
                out.write(buffer, 0, read);
            }
            out.close();
            in.close();
            new PluginInformation(file);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        file.delete(); /* cleanup */
        return false;
    }

    public static void update(Collection<PluginInformation> update) {
        Main.worker.execute(new UpdateTask(update, true));
    }

    public Collection<PluginInformation> download(Collection<PluginInformation> download) {
        // Execute task in current thread instead of executing it in other thread and waiting for result
        // Waiting for result is not a good idea because the waiting thread will probably be either EDT
        // or worker thread. Blocking one of these threads will cause deadlock
        UpdateTask t = new UpdateTask(download, false);
        t.run();
        return t.failed;
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
