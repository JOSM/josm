//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.plugins;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.tools.OpenBrowser;

public class PluginSelection {

    private Map<String, Boolean> pluginMap;
    private Map<String, PluginInformation> availablePlugins;

    public void updateDescription(JPanel pluginPanel) {
        int count = PluginDownloader.downloadDescription();
        if (count > 0)
            JOptionPane.showMessageDialog(Main.parent,
                trn("Downloaded plugin information from {0} site",
                    "Downloaded plugin information from {0} sites", count, count));
        else
            JOptionPane.showMessageDialog(Main.parent, tr("No plugin information found."));
        drawPanel(pluginPanel);
    }

    public void update(JPanel pluginPanel) {
        // refresh description
        int num = PluginDownloader.downloadDescription();
        Boolean done = false;
        drawPanel(pluginPanel);

        Set<PluginInformation> toUpdate = new HashSet<PluginInformation>();
        StringBuilder toUpdateStr = new StringBuilder();
        for (PluginProxy proxy : PluginHandler.pluginList) {
            PluginInformation description = availablePlugins.get(proxy.info.name);
            if (description != null && (description.version == null || description.version.equals("")) ?
            (proxy.info.version != null && proxy.info.version.equals("")) : !description.version.equals(proxy.info.version)) {
                toUpdate.add(description);
                toUpdateStr.append(description.name+"\n");
            }
        }
        if (toUpdate.isEmpty()) {
            JOptionPane.showMessageDialog(Main.parent, tr("All installed plugins are up to date."));
            done = true;
        } else {
            int answer = new ExtendedDialog(Main.parent,
                        tr("Update"),
                        tr("Update the following plugins:\n\n{0}", toUpdateStr.toString()),
                        new String[] {tr("Update Plugins"), tr("Cancel")},
                        new String[] {"dialogs/refresh.png", "cancel.png"}).getValue();
            if (answer == 1) {
                PluginDownloader.update(toUpdate);
                done = true;
            }
        }
        if (done && num >= 1)
            Main.pref.put("pluginmanager.lastupdate", Long.toString(System.currentTimeMillis()));
        drawPanel(pluginPanel);
    }

    public Boolean finish() {
        Collection<PluginInformation> toDownload = new LinkedList<PluginInformation>();
        String msg = "";
        for (Entry<String, Boolean> entry : pluginMap.entrySet()) {
            if (entry.getValue() && PluginInformation.findPlugin(entry.getKey()) == null) {
                toDownload.add(availablePlugins.get(entry.getKey()));
                msg += entry.getKey() + "\n";
            }
        }
        if (!toDownload.isEmpty()) {
            int answer = new ExtendedDialog(Main.parent,
                        tr("Download missing plugins"),
                        tr("Download the following plugins?\n\n{0}", msg),
                        new String[] {tr("Download Plugins"), tr("Cancel")},
                        new String[] {"download.png", "cancel.png"}).getValue();
            if (answer != 1)
                for (PluginInformation pd : toDownload)
                    pluginMap.put(pd.name, false);
            else
                for (PluginInformation pd : toDownload)
                    if (!PluginDownloader.downloadPlugin(pd))
                        pluginMap.put(pd.name, false);

        }
        LinkedList<String> plugins = new LinkedList<String>();
        for (Map.Entry<String, Boolean> d : pluginMap.entrySet()) {
            if (d.getValue())
                plugins.add(d.getKey());
        }

        Collections.sort(plugins);
        return Main.pref.putCollection("plugins", plugins);
    }

    /* return true when plugin list changed */
    public void drawPanel(JPanel pluginPanel) {
        availablePlugins = getAvailablePlugins();
        Collection<String> enabledPlugins = Main.pref.getCollection("plugins", null);

        if (pluginMap == null)
            pluginMap = new HashMap<String, Boolean>();
        else {
            // Keep the map in bounds; possibly slightly pointless.
            Set<String> pluginsToRemove = new HashSet<String>();
            for (final String pname : pluginMap.keySet()) {
                if (availablePlugins.get(pname) == null) pluginsToRemove.add(pname);
            }

            for (String pname : pluginsToRemove) {
                pluginMap.remove(pname);
            }
        }

        pluginPanel.removeAll();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        int row = 0;
        for (final PluginInformation plugin : availablePlugins.values()) {
            boolean enabled = (enabledPlugins != null) && enabledPlugins.contains(plugin.name);
            if (pluginMap.get(plugin.name) == null)
                pluginMap.put(plugin.name, enabled);

            String remoteversion = plugin.version;
            if ((remoteversion == null) || remoteversion.equals(""))
                remoteversion = tr("unknown");

            String localversion;
            PluginInformation p = PluginInformation.findPlugin(plugin.name);
            if (p != null) {
                if (p.version != null && !p.version.equals(""))
                    localversion = p.version;
                else
                    localversion = tr("unknown");
                localversion = " (" + localversion + ")";
            } else
                localversion = "";

            final JCheckBox pluginCheck = new JCheckBox(
                    tr("{0}: Version {1}{2}", plugin.name, remoteversion, localversion),
                    pluginMap.get(plugin.name));
            gbc.gridy = row++;
            gbc.insets = new Insets(5,5,0,5);
            gbc.weighty = 0.1;
            gbc.fill = GridBagConstraints.NONE;
            pluginPanel.add(pluginCheck, gbc);

            pluginCheck.setToolTipText(plugin.downloadlink != null ? ""+plugin.downloadlink : tr("Plugin bundled with JOSM"));

            JEditorPane description = new JEditorPane();
            description.setContentType("text/html");
            description.setEditable(false);
            description.setText("<html><i>"+plugin.getLinkDescription()+"</i></html>");
            description.setBorder(BorderFactory.createEmptyBorder(0,20,0,0));
            description.setBackground(UIManager.getColor("Panel.background"));
            description.addHyperlinkListener(new HyperlinkListener() {
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if(e.getEventType() == EventType.ACTIVATED) {
                        OpenBrowser.displayUrl(e.getURL().toString());
                    }
                }
            });

            gbc.gridy = row++;
            gbc.insets = new Insets(3,5,5,5);
            gbc.weighty = 0.9;
            gbc.weightx = 1.0;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            pluginPanel.add(description, gbc);

            pluginCheck.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    // if user enabled a plugin, it is not loaded but found somewhere on disk: offer to delete jar
                    if (pluginCheck.isSelected()) {
                        PluginInformation plinfo = PluginInformation.findPlugin(plugin.name);
                        if ((getLoaded(plugin.name) == null) && (plinfo != null)) {
                            try {
                                int answer = new ExtendedDialog(Main.parent,
                                    tr("Plugin already exists"),
                                    tr("Plugin archive already available. Do you want to download"
                                        + " the current version by deleting existing archive?\n\n{0}",
                                        plinfo.file.getCanonicalPath()),
                                    new String[] {tr("Delete and Download"), tr("Cancel")},
                                    new String[] {"download.png", "cancel.png"}).getValue();

                                if (answer == 1) {
                                    if (!plinfo.file.delete()) {
                                        JOptionPane.showMessageDialog(Main.parent, tr("Error deleting plugin file: {0}", plinfo.file.getCanonicalPath()));
                                    }
                                }
                            } catch (IOException e1) {
                                e1.printStackTrace();
                                JOptionPane.showMessageDialog(Main.parent, tr("Error deleting plugin file: {0}", e1.getMessage()));
                            }
                        }
                    }
                    pluginMap.put(plugin.name, pluginCheck.isSelected());
                }
            });
        }
        pluginPanel.updateUI();
    }

    /**
     * Return information about a loaded plugin.
     *
     * Note that if you call this in your plugins bootstrap, you may get <code>null</code> if
     * the plugin requested is not loaded yet.
     *
     * @return The PluginInformation to a specific plugin, but only if the plugin is loaded.
     * If it is not loaded, <code>null</code> is returned.
     */
    private static PluginInformation getLoaded(String pluginName) {
        for (PluginProxy p : PluginHandler.pluginList)
            if (p.info.name.equals(pluginName))
                return p.info;
        return null;
    }

    private Map<String, PluginInformation> getAvailablePlugins() {
        SortedMap<String, PluginInformation> availablePlugins = new TreeMap<String, PluginInformation>(new Comparator<String>(){
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        });
        for (String location : PluginInformation.getPluginLocations()) {
            File[] pluginFiles = new File(location).listFiles();
            if (pluginFiles != null) {
                Arrays.sort(pluginFiles);
                for (File f : pluginFiles) {
                    if (!f.isFile())
                        continue;
                    String fname = f.getName();
                    if (fname.endsWith(".jar")) {
                        try {
                            PluginInformation info = new PluginInformation(f,fname.substring(0,fname.length()-4));
                            if (!availablePlugins.containsKey(info.name))
                                availablePlugins.put(info.name, info);
                        } catch (PluginException x) {
                        }
                    } else if (fname.matches("^[0-9]+-site.*\\.txt$")) {
                        int err = 0;
                        try {
                            BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "utf-8"));
                            String name = null;
                            String url = null;
                            String manifest = null;
                            for (String line = r.readLine(); line != null; line = r.readLine())
                            {
                                if(line.startsWith("\t"))
                                {
                                    line = line.substring(1);
                                    if(line.length() > 70)
                                    {
                                        manifest += line.substring(0,70)+"\n";
                                        line = " " + line.substring(70);
                                    }
                                    manifest += line+"\n";
                                }
                                else
                                {
                                    if(name != null)
                                    {
                                        try
                                        {
                                            PluginInformation info = new PluginInformation(
                                            new ByteArrayInputStream(manifest.getBytes("utf-8")),
                                            name.substring(0,name.length()-4));
                                            info.downloadlink = url;
                                            if(!availablePlugins.containsKey(info.name))
                                                availablePlugins.put(info.name, info);
                                        }
                                        catch (Exception e)
                                        {
                                            e.printStackTrace();
                                            ++err;
                                        }
                                    }
                                    String x[] = line.split(";");
                                    name = x[0];
                                    url = x[1];
                                    manifest = null;
                                }
                            }
                            if(name != null)
                            {
                                PluginInformation info = new PluginInformation(
                                new ByteArrayInputStream(manifest.getBytes("utf-8")),
                                name.substring(0,name.length()-4));
                                info.downloadlink = url;
                                if(!availablePlugins.containsKey(info.name))
                                    availablePlugins.put(info.name, info);
                            }
                            r.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                            ++err;
                        }
                        if(err > 0)
                        {
                          JOptionPane.showMessageDialog(Main.parent, tr("Error reading plugin information file: {0}", f.getName()));
                        }
                    }
                }
            }
        }
        for (PluginProxy proxy : PluginHandler.pluginList)
            if (!availablePlugins.containsKey(proxy.info.name))
                availablePlugins.put(proxy.info.name, proxy.info);
        return availablePlugins;
    }
}
