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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
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
    private Map<String, PluginInformation> localPlugins;

    public void updateDescription(JPanel pluginPanel) {
        int count = PluginDownloader.downloadDescription();
        if (count > 0) {
            JOptionPane.showMessageDialog(Main.parent,
                    trn("Downloaded plugin information from {0} site",
                            "Downloaded plugin information from {0} sites", count, count),
                            tr("Information"),
                            JOptionPane.INFORMATION_MESSAGE
            );
        } else {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("No plugin information found."),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
        }
        drawPanel(pluginPanel);
    }

    public void update(JPanel pluginPanel) {
        // refresh description
        int num = PluginDownloader.downloadDescription();
        Boolean done = false;
        drawPanel(pluginPanel);

        Set<PluginInformation> toUpdate = new HashSet<PluginInformation>();
        StringBuilder toUpdateStr = new StringBuilder();
        for (String pluginName : Main.pref.getCollection("plugins", Collections.<String>emptySet())) {
            PluginInformation local = localPlugins.get(pluginName);
            PluginInformation description = availablePlugins.get(pluginName);

            if (description == null) {
                System.out.println(tr("Plug-in named {0} is not available. Update skipped.", pluginName));
                continue;
            }

            if (local == null || (description.version != null && !description.version.equals(local.version))) {
                toUpdate.add(description);
                toUpdateStr.append(pluginName+"\n");
            }
        }
        if (toUpdate.isEmpty()) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("All installed plugins are up to date."),
                    tr("Information"),
                    JOptionPane.INFORMATION_MESSAGE
            );
            done = true;
        } else {
            ExtendedDialog ed = new ExtendedDialog(Main.parent,
                    tr("Update"),
                    new String[] {tr("Update Plugins"), tr("Cancel")});
            ed.setButtonIcons(new String[] {"dialogs/refresh.png", "cancel.png"});
            ed.setContent(tr("Update the following plugins:\n\n{0}", toUpdateStr.toString()));
            ed.showDialog();

            if (ed.getValue() == 1) {
                PluginDownloader.update(toUpdate);
                done = true;
            }
        }
        if (done && num >= 1) {
            Main.pref.put("pluginmanager.lastupdate", Long.toString(System.currentTimeMillis()));
        }
        drawPanel(pluginPanel);
    }

    public boolean finish() {
        Collection<PluginInformation> toDownload = new LinkedList<PluginInformation>();
        Collection<String> installedPlugins = Main.pref.getCollection("plugins", Collections.<String>emptySet());

        String msg = "";
        for (Entry<String, Boolean> entry : pluginMap.entrySet()) {
            if(entry.getValue() && !installedPlugins.contains(entry.getKey()))
            {
                String name = entry.getKey();
                PluginInformation ap = availablePlugins.get(name);
                PluginInformation pi = localPlugins.get(name);
                if(pi == null || (pi.version == null && ap.version != null)
                        || (pi.version != null && !pi.version.equals(ap.version)))
                {
                    toDownload.add(ap);
                    msg += name + "\n";
                }
            }
        }
        if (!toDownload.isEmpty()) {
            ExtendedDialog ed = new ExtendedDialog(Main.parent,
                    tr("Download missing plugins"),
                    new String[] {tr("Download Plugins"), tr("Cancel")});
            ed.setButtonIcons(new String[] {"download.png", "cancel.png"});
            ed.setContent(tr("Download the following plugins?\n\n{0}", msg));
            ed.showDialog();

            Collection<PluginInformation> error =
                (ed.getValue() != 1 ? toDownload : new PluginDownloader().download(toDownload));
            for (PluginInformation pd : error) {
                pluginMap.put(pd.name, false);
            }

        }
        LinkedList<String> plugins = new LinkedList<String>();
        for (Map.Entry<String, Boolean> d : pluginMap.entrySet()) {
            if (d.getValue()) {
                plugins.add(d.getKey());
            }
        }

        Collections.sort(plugins);
        return Main.pref.putCollection("plugins", plugins);
    }

    /* return true when plugin list changed */
    public void drawPanel(JPanel pluginPanel) {
        loadPlugins();
        Collection<String> enabledPlugins = Main.pref.getCollection("plugins", null);

        if (pluginMap == null) {
            pluginMap = new HashMap<String, Boolean>();
        } else {
            // Keep the map in bounds; possibly slightly pointless.
            Set<String> pluginsToRemove = new HashSet<String>();
            for (final String pname : pluginMap.keySet()) {
                if (availablePlugins.get(pname) == null) {
                    pluginsToRemove.add(pname);
                }
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
            if (pluginMap.get(plugin.name) == null) {
                pluginMap.put(plugin.name, enabled);
            }

            String remoteversion = plugin.version;
            if ((remoteversion == null) || remoteversion.equals("")) {
                remoteversion = tr("unknown");
            } else if(plugin.oldmode) {
                remoteversion += "*";
            }

            String localversion = "";
            PluginInformation p = localPlugins.get(plugin.name);
            if(p != null)
            {
                if (p.version != null && !p.version.equals("")) {
                    localversion = p.version;
                } else {
                    localversion = tr("unknown");
                }
                localversion = " (" + localversion + ")";
            }

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
            description.setText("<html><body bgcolor=\"#" + Integer.toHexString( UIManager.getColor("Panel.background").getRGB() & 0x00ffffff ) +"\"><i>"+plugin.getLinkDescription()+"</i></body></html>");
            description.setBorder(BorderFactory.createEmptyBorder());
            description.setBackground(UIManager.getColor("Panel.background"));
            description.addHyperlinkListener(new HyperlinkListener() {
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if(e.getEventType() == EventType.ACTIVATED) {
                        OpenBrowser.displayUrl(e.getURL().toString());
                    }
                }
            });

            gbc.gridy = row++;
            gbc.insets = new Insets(3,25,5,5);
            gbc.weighty = 0.9;
            gbc.weightx = 1.0;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            pluginPanel.add(description, gbc);

            pluginCheck.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e) {
                    pluginMap.put(plugin.name, pluginCheck.isSelected());
                }
            });
        }
        pluginPanel.updateUI();
    }

    private void loadPlugins() {
        availablePlugins = new TreeMap<String, PluginInformation>(new Comparator<String>(){
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        });
        localPlugins = new TreeMap<String, PluginInformation>(new Comparator<String>(){
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        });
        for (String location : PluginInformation.getPluginLocations()) {
            File[] pluginFiles = new File(location).listFiles();
            if (pluginFiles != null) {
                Arrays.sort(pluginFiles);
                for (File f : pluginFiles) {
                    if (!f.isFile()) {
                        continue;
                    }
                    String fname = f.getName();
                    if (fname.endsWith(".jar")) {
                        try {
                            PluginInformation info = new PluginInformation(f,fname.substring(0,fname.length()-4));
                            if (!availablePlugins.containsKey(info.name)) {
                                availablePlugins.put(info.name, info);
                            }
                            if (!localPlugins.containsKey(info.name)) {
                                localPlugins.put(info.name, info);
                            }
                        } catch (PluginException x) {
                        }
                    } else if (fname.endsWith(".jar.new")) {
                        try {
                            PluginInformation info = new PluginInformation(f,fname.substring(0,fname.length()-8));
                            availablePlugins.put(info.name, info);
                            localPlugins.put(info.name, info);
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
                                                    name.substring(0,name.length()-4), url);
                                            if(!availablePlugins.containsKey(info.name)) {
                                                availablePlugins.put(info.name, info);
                                            }
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
                                        name.substring(0,name.length()-4), url);
                                if(!availablePlugins.containsKey(info.name)) {
                                    availablePlugins.put(info.name, info);
                                }
                            }
                            r.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                            ++err;
                        }
                        if(err > 0)
                        {
                            JOptionPane.showMessageDialog(
                                    Main.parent,
                                    tr("Error reading plugin information file: {0}", f.getName()),
                                    tr("Error"),
                                    JOptionPane.ERROR_MESSAGE
                            );
                        }
                    }
                }
            }
        }
        for (PluginProxy proxy : PluginHandler.pluginList)
        {
            if (!availablePlugins.containsKey(proxy.info.name)) {
                availablePlugins.put(proxy.info.name, proxy.info);
            }
            if (!localPlugins.containsKey(proxy.info.name)) {
                localPlugins.put(proxy.info.name, proxy.info);
            }
        }
    }
}
