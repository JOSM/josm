//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
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

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.plugins.PluginDownloader;
import org.openstreetmap.josm.plugins.PluginException;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.PluginProxy;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.XmlObjectParser.Uniform;

public class PluginPreference implements PreferenceSetting {

    /**
     * Only the plugin name, its jar location and the description.
     * In other words, this is the minimal requirement the plugin preference page
     * needs to show the plugin as available
     *
     * @author imi
     */
    public static class PluginDescription implements Comparable<Object> {
        // Note: All the following need to be public instance variables of
        // type String.  (Plugin description XMLs from the server are parsed
        // with tools.XmlObjectParser, which uses reflection to access them.)
        public String name;
        public String description;
        public String resource;
        public String version;
        public PluginDescription(String name, String description, String resource, String version) {
            this.name = name;
            this.description = description;
            this.resource = resource;
            this.version = version;
        }
        public PluginDescription() {
        }
        public int compareTo(Object n) {
            if(n instanceof PluginDescription)
                return name.compareToIgnoreCase(((PluginDescription)n).name);
            return -1;
        }
    }

    private Map<String, Boolean> pluginMap;
    private Map<String, PluginDescription> availablePlugins;
    private JPanel plugin;
    private JPanel pluginPanel = new NoHorizontalScrollPanel(new GridBagLayout());
    private PreferenceDialog gui;
    private JScrollPane pluginPane;

    public void addGui(final PreferenceDialog gui) {
        this.gui = gui;
        plugin = gui.createPreferenceTab("plugin", tr("Plugins"), tr("Configure available plugins."), false);
        pluginPane = new JScrollPane(pluginPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pluginPane.setBorder(null);
        plugin.add(pluginPane, GBC.eol().fill(GBC.BOTH));
        plugin.add(GBC.glue(0,10), GBC.eol());
        JButton morePlugins = new JButton(tr("Download List"));
        morePlugins.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                int count = PluginDownloader.downloadDescription();
                if (count > 0)
                    JOptionPane.showMessageDialog(Main.parent,
                        trn("Downloaded plugin information from {0} site",
                            "Downloaded plugin information from {0} sites", count, count));
                else
                    JOptionPane.showMessageDialog(Main.parent, tr("No plugin information found."));
                refreshPluginPanel(gui);
            }
        });
        plugin.add(morePlugins, GBC.std().insets(0,0,10,0));

        JButton update = new JButton(tr("Update"));
        update.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                update();
                refreshPluginPanel(gui);
            }
        });
        plugin.add(update, GBC.std().insets(0,0,10,0));

        JButton configureSites = new JButton(tr("Configure Sites ..."));
        configureSites.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                configureSites();
            }
        });
        plugin.add(configureSites, GBC.std());

        refreshPluginPanel(gui);
    }

    private void configureSites() {
        JPanel p = new JPanel(new GridBagLayout());
        p.add(new JLabel(tr("Add either site-josm.xml or Wiki Pages.")), GBC.eol());
        final DefaultListModel model = new DefaultListModel();
        for (String s : PluginDownloader.getSites())
            model.addElement(s);
        final JList list = new JList(model);
        p.add(new JScrollPane(list), GBC.std().fill());
        JPanel buttons = new JPanel(new GridBagLayout());
        buttons.add(new JButton(new AbstractAction(tr("Add")){
            public void actionPerformed(ActionEvent e) {
                String s = JOptionPane.showInputDialog(gui, tr("Add either site-josm.xml or Wiki Pages."));
                if (s != null)
                    model.addElement(s);
            }
        }), GBC.eol().fill(GBC.HORIZONTAL));
        buttons.add(new JButton(new AbstractAction(tr("Edit")){
            public void actionPerformed(ActionEvent e) {
                if (list.getSelectedValue() == null) {
                    JOptionPane.showMessageDialog(gui, tr("Please select an entry."));
                    return;
                }
                String s = JOptionPane.showInputDialog(gui, tr("Add either site-josm.xml or Wiki Pages."), list.getSelectedValue());
                model.setElementAt(s, list.getSelectedIndex());
            }
        }), GBC.eol().fill(GBC.HORIZONTAL));
        buttons.add(new JButton(new AbstractAction(tr("Delete")){
            public void actionPerformed(ActionEvent event) {
                if (list.getSelectedValue() == null) {
                    JOptionPane.showMessageDialog(gui, tr("Please select an entry."));
                    return;
                }
                model.removeElement(list.getSelectedValue());
            }
        }), GBC.eol().fill(GBC.HORIZONTAL));
        p.add(buttons, GBC.eol());
        int answer = JOptionPane.showConfirmDialog(gui, p, tr("Configure Plugin Sites"), JOptionPane.OK_CANCEL_OPTION);
        if (answer != JOptionPane.OK_OPTION)
            return;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < model.getSize(); ++i) {
            b.append(model.getElementAt(i));
            if (i < model.getSize()-1)
                b.append(" ");
        }
        Main.pref.put("pluginmanager.sites", b.toString());
    }

    private void update() {
        // refresh description
        int num = PluginDownloader.downloadDescription();
        Boolean done = false;
        refreshPluginPanel(gui);

        Set<PluginDescription> toUpdate = new HashSet<PluginDescription>();
        StringBuilder toUpdateStr = new StringBuilder();
        for (PluginProxy proxy : Main.plugins) {
            PluginDescription description = availablePlugins.get(proxy.info.name);
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
            int answer = JOptionPane.showConfirmDialog(Main.parent, tr("Update the following plugins:\n\n{0}",
            toUpdateStr.toString()), tr("Update"), JOptionPane.OK_CANCEL_OPTION);
            if (answer == JOptionPane.OK_OPTION) {
                PluginDownloader.update(toUpdate);
                done = true;
            }
        }
        if (done && num >= 1)
            Main.pref.put("pluginmanager.lastupdate", Long.toString(System.currentTimeMillis()));
    }

    private void refreshPluginPanel(final PreferenceDialog gui) {
        availablePlugins = getAvailablePlugins();
        Collection<String> enabledPlugins = Main.pref.getCollection("plugins", null);

        if (pluginMap == null)
            pluginMap = new HashMap<String, Boolean>();
        else
            // Keep the map in bounds; possibly slightly pointless.
            for (final String pname : pluginMap.keySet())
                if (availablePlugins.get(pname) == null) pluginMap.remove(pname);

        pluginPanel.removeAll();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        int row = 0;
        for (final PluginDescription plugin : availablePlugins.values()) {
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

            pluginCheck.setToolTipText(plugin.resource != null ? ""+plugin.resource : tr("Plugin bundled with JOSM"));

            JEditorPane description = new JEditorPane();
            description.setContentType("text/html");
            description.setEditable(false);
            description.setText("<html><i>"+(plugin.description==null?tr("no description available"):plugin.description)+"</i></html>");
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
                        if ((PluginInformation.getLoaded(plugin.name) == null) && (plinfo != null)) {
                            try {
                                int answer = JOptionPane.showConfirmDialog(Main.parent,
                                    tr("Plugin archive already available. Do you want to download current version by deleting existing archive?\n\n{0}",
                                    plinfo.file.getCanonicalPath()), tr("Plugin already exists"), JOptionPane.OK_CANCEL_OPTION);
                                if (answer == JOptionPane.OK_OPTION) {
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
        plugin.updateUI();
    }

    private Map<String, PluginDescription> getAvailablePlugins() {
        SortedMap<String, PluginDescription> availablePlugins = new TreeMap<String, PluginDescription>(new Comparator<String>(){
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
                    if (f.getName().endsWith(".jar")) {
                        try {
                            PluginInformation info = new PluginInformation(f);
                            if (!availablePlugins.containsKey(info.name))
                                availablePlugins.put(info.name, new PluginDescription(
                                    info.name,
                                    info.description,
                                    PluginInformation.fileToURL(f).toString(),
                                    info.version));
                        } catch (PluginException x) {
                        }
                    } else if (f.getName().matches("^[0-9]+-site.*\\.xml$")) {
                        try {
                            Uniform<PluginDescription> parser = new Uniform<PluginDescription>(new FileReader(f), "plugin", PluginDescription.class);
                            for (PluginDescription pd : parser)
                                if (!availablePlugins.containsKey(pd.name))
                                    availablePlugins.put(pd.name, pd);
                        } catch (Exception e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(Main.parent, tr("Error reading plugin information file: {0}", f.getName()));
                        }
                    }
                }
            }
        }
        for (PluginProxy proxy : Main.plugins)
            if (!availablePlugins.containsKey(proxy.info.name))
                availablePlugins.put(proxy.info.name, new PluginDescription(
                        proxy.info.name,
                        proxy.info.description,
                        proxy.info.file == null ? null :
                            PluginInformation.fileToURL(proxy.info.file).toString(),
                        proxy.info.version));
        return availablePlugins;
    }

    public boolean ok() {
        Collection<PluginDescription> toDownload = new LinkedList<PluginDescription>();
        String msg = "";
        for (Entry<String, Boolean> entry : pluginMap.entrySet()) {
            if (entry.getValue() && PluginInformation.findPlugin(entry.getKey()) == null) {
                toDownload.add(availablePlugins.get(entry.getKey()));
                msg += entry.getKey() + "\n";
            }
        }
        if (!toDownload.isEmpty()) {
            int answer = JOptionPane.showConfirmDialog(Main.parent,
                    tr("Download the following plugins?\n\n{0}", msg),
                    tr("Download missing plugins"),
                    JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.OK_OPTION)
                for (PluginDescription pd : toDownload)
                    pluginMap.put(pd.name, false);
            else
                for (PluginDescription pd : toDownload)
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
    
    class NoHorizontalScrollPanel extends JPanel implements Scrollable {
        public NoHorizontalScrollPanel(GridBagLayout gridBagLayout) {
            super(gridBagLayout);
        }

        public Dimension getPreferredScrollableViewportSize() {
            return super.getPreferredSize();
        }

        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 30;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 10;
        }
    }
}
