//License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.plugin;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane.PreferencePanel;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.SelectAllOnFocusGainedDecorator;
import org.openstreetmap.josm.plugins.PluginDownloadTask;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.ReadLocalPluginInformationTask;
import org.openstreetmap.josm.plugins.ReadRemotePluginInformationTask;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Preference settings for plugins.
 * @since 168
 */
public final class PluginPreference extends DefaultTabPreferenceSetting {

    /**
     * Factory used to create a new {@code PluginPreference}.
     */
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new PluginPreference();
        }
    }

    private PluginPreference() {
        super("plugin", tr("Plugins"), tr("Configure available plugins."), false, new JTabbedPane());
    }

    /**
     * Returns the download summary string to be shown.
     * @param task The plugin download task that has completed
     * @return the download summary string to be shown. Contains summary of success/failed plugins.
     */
    public static String buildDownloadSummary(PluginDownloadTask task) {
        Collection<PluginInformation> downloaded = task.getDownloadedPlugins();
        Collection<PluginInformation> failed = task.getFailedPlugins();
        StringBuilder sb = new StringBuilder();
        if (! downloaded.isEmpty()) {
            sb.append(trn(
                    "The following plugin has been downloaded <strong>successfully</strong>:",
                    "The following {0} plugins have been downloaded <strong>successfully</strong>:",
                    downloaded.size(),
                    downloaded.size()
                    ));
            sb.append("<ul>");
            for(PluginInformation pi: downloaded) {
                sb.append("<li>").append(pi.name).append(" (").append(pi.version).append(")").append("</li>");
            }
            sb.append("</ul>");
        }
        if (! failed.isEmpty()) {
            sb.append(trn(
                    "Downloading the following plugin has <strong>failed</strong>:",
                    "Downloading the following {0} plugins has <strong>failed</strong>:",
                    failed.size(),
                    failed.size()
                    ));
            sb.append("<ul>");
            for(PluginInformation pi: failed) {
                sb.append("<li>").append(pi.name).append("</li>");
            }
            sb.append("</ul>");
        }
        return sb.toString();
    }
    
    /**
     * Notifies user about result of a finished plugin download task.
     * @param parent The parent component
     * @param task The finished plugin download task
     * @since 6797
     */
    public static void notifyDownloadResults(final Component parent, PluginDownloadTask task) {
        final Collection<PluginInformation> downloaded = task.getDownloadedPlugins();
        final Collection<PluginInformation> failed = task.getFailedPlugins();
        final StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append(buildDownloadSummary(task));
        if (!downloaded.isEmpty()) {
            sb.append(tr("Please restart JOSM to activate the downloaded plugins."));
        }
        sb.append("</html>");
        GuiHelper.runInEDTAndWait(new Runnable() {
            @Override
            public void run() {
                HelpAwareOptionPane.showOptionDialog(
                        parent,
                        sb.toString(),
                        tr("Update plugins"),
                        !failed.isEmpty() ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE,
                                HelpUtil.ht("/Preferences/Plugins")
                        );
            }
        });
    }

    private JosmTextField tfFilter;
    private PluginListPanel pnlPluginPreferences;
    private PluginPreferencesModel model;
    private JScrollPane spPluginPreferences;
    private PluginUpdatePolicyPanel pnlPluginUpdatePolicy;

    /**
     * is set to true if this preference pane has been selected
     * by the user
     */
    private boolean pluginPreferencesActivated = false;

    protected JPanel buildSearchFieldPanel() {
        JPanel pnl  = new JPanel(new GridBagLayout());
        pnl.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        GridBagConstraints gc = new GridBagConstraints();

        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        gc.insets = new Insets(0,0,0,3);
        pnl.add(new JLabel(tr("Search:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        tfFilter = new JosmTextField();
        pnl.add(tfFilter, gc);
        tfFilter.setToolTipText(tr("Enter a search expression"));
        SelectAllOnFocusGainedDecorator.decorate(tfFilter);
        tfFilter.getDocument().addDocumentListener(new SearchFieldAdapter());
        return pnl;
    }

    protected JPanel buildActionPanel() {
        JPanel pnl = new JPanel(new GridLayout(1,3));

        pnl.add(new JButton(new DownloadAvailablePluginsAction()));
        pnl.add(new JButton(new UpdateSelectedPluginsAction()));
        pnl.add(new JButton(new ConfigureSitesAction()));
        return pnl;
    }

    protected JPanel buildPluginListPanel() {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.add(buildSearchFieldPanel(), BorderLayout.NORTH);
        model  = new PluginPreferencesModel();
        pnlPluginPreferences = new PluginListPanel(model);
        spPluginPreferences = GuiHelper.embedInVerticalScrollPane(pnlPluginPreferences);
        spPluginPreferences.getVerticalScrollBar().addComponentListener(
                new ComponentAdapter(){
                    @Override
                    public void componentShown(ComponentEvent e) {
                        spPluginPreferences.setBorder(UIManager.getBorder("ScrollPane.border"));
                    }
                    @Override
                    public void componentHidden(ComponentEvent e) {
                        spPluginPreferences.setBorder(null);
                    }
                }
                );

        pnl.add(spPluginPreferences, BorderLayout.CENTER);
        pnl.add(buildActionPanel(), BorderLayout.SOUTH);
        return pnl;
    }

    protected JTabbedPane buildContentPane() {
        JTabbedPane pane = getTabPane();
        pnlPluginUpdatePolicy = new PluginUpdatePolicyPanel();
        pane.addTab(tr("Plugins"), buildPluginListPanel());
        pane.addTab(tr("Plugin update policy"), pnlPluginUpdatePolicy);
        return pane;
    }

    @Override
    public void addGui(final PreferenceTabbedPane gui) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.BOTH;
        PreferencePanel plugins = gui.createPreferenceTab(this);
        plugins.add(buildContentPane(), gc);
        readLocalPluginInformation();
        pluginPreferencesActivated = true;
    }

    private void configureSites() {
        ButtonSpec[] options = new ButtonSpec[] {
                new ButtonSpec(
                        tr("OK"),
                        ImageProvider.get("ok"),
                        tr("Accept the new plugin sites and close the dialog"),
                        null /* no special help topic */
                        ),
                        new ButtonSpec(
                                tr("Cancel"),
                                ImageProvider.get("cancel"),
                                tr("Close the dialog"),
                                null /* no special help topic */
                                )
        };
        PluginConfigurationSitesPanel pnl = new PluginConfigurationSitesPanel();

        int answer = HelpAwareOptionPane.showOptionDialog(
                pnlPluginPreferences,
                pnl,
                tr("Configure Plugin Sites"),
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0],
                null /* no help topic */
                );
        if (answer != 0 /* OK */)
            return;
        List<String> sites = pnl.getUpdateSites();
        Main.pref.setPluginSites(sites);
    }

    /**
     * Replies the list of plugins waiting for update or download
     *
     * @return the list of plugins waiting for update or download
     */
    public List<PluginInformation> getPluginsScheduledForUpdateOrDownload() {
        return model != null ? model.getPluginsScheduledForUpdateOrDownload() : null;
    }

    @Override
    public boolean ok() {
        if (! pluginPreferencesActivated)
            return false;
        pnlPluginUpdatePolicy.rememberInPreferences();
        if (model.isActivePluginsChanged()) {
            LinkedList<String> l = new LinkedList<String>(model.getSelectedPluginNames());
            Collections.sort(l);
            Main.pref.putCollection("plugins", l);
            return true;
        }
        return false;
    }

    /**
     * Reads locally available information about plugins from the local file system.
     * Scans cached plugin lists from plugin download sites and locally available
     * plugin jar files.
     *
     */
    public void readLocalPluginInformation() {
        final ReadLocalPluginInformationTask task = new ReadLocalPluginInformationTask();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (task.isCanceled()) return;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        model.setAvailablePlugins(task.getAvailablePlugins());
                        pnlPluginPreferences.refreshView();
                    }
                });
            }
        };
        Main.worker.submit(task);
        Main.worker.submit(r);
    }

    /**
     * The action for downloading the list of available plugins
     *
     */
    class DownloadAvailablePluginsAction extends AbstractAction {

        public DownloadAvailablePluginsAction() {
            putValue(NAME,tr("Download list"));
            putValue(SHORT_DESCRIPTION, tr("Download the list of available plugins"));
            putValue(SMALL_ICON, ImageProvider.get("download"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final ReadRemotePluginInformationTask task = new ReadRemotePluginInformationTask(Main.pref.getPluginSites());
            Runnable continuation = new Runnable() {
                @Override
                public void run() {
                    if (task.isCanceled()) return;
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            model.updateAvailablePlugins(task.getAvailablePlugins());
                            pnlPluginPreferences.refreshView();
                            Main.pref.putInteger("pluginmanager.version", Version.getInstance().getVersion()); // fix #7030
                        }
                    });
                }
            };
            Main.worker.submit(task);
            Main.worker.submit(continuation);
        }
    }

    /**
     * The action for downloading the list of available plugins
     *
     */
    class UpdateSelectedPluginsAction extends AbstractAction {
        public UpdateSelectedPluginsAction() {
            putValue(NAME,tr("Update plugins"));
            putValue(SHORT_DESCRIPTION, tr("Update the selected plugins"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "refresh"));
        }

        protected void alertNothingToUpdate() {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        HelpAwareOptionPane.showOptionDialog(
                                pnlPluginPreferences,
                                tr("All installed plugins are up to date. JOSM does not have to download newer versions."),
                                tr("Plugins up to date"),
                                JOptionPane.INFORMATION_MESSAGE,
                                null // FIXME: provide help context
                                );
                    }
                });
            } catch (InterruptedException e) {
                Main.error(e);
            } catch (InvocationTargetException e) {
                Main.error(e);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final List<PluginInformation> toUpdate = model.getSelectedPlugins();
            // the async task for downloading plugins
            final PluginDownloadTask pluginDownloadTask = new PluginDownloadTask(
                    pnlPluginPreferences,
                    toUpdate,
                    tr("Update plugins")
                    );
            // the async task for downloading plugin information
            final ReadRemotePluginInformationTask pluginInfoDownloadTask = new ReadRemotePluginInformationTask(Main.pref.getPluginSites());

            // to be run asynchronously after the plugin download
            //
            final Runnable pluginDownloadContinuation = new Runnable() {
                @Override
                public void run() {
                    if (pluginDownloadTask.isCanceled())
                        return;
                    notifyDownloadResults(pnlPluginPreferences, pluginDownloadTask);
                    model.refreshLocalPluginVersion(pluginDownloadTask.getDownloadedPlugins());
                    model.clearPendingPlugins(pluginDownloadTask.getDownloadedPlugins());
                    GuiHelper.runInEDT(new Runnable() {
                        @Override
                        public void run() {
                            pnlPluginPreferences.refreshView();                        }
                    });
                }
            };

            // to be run asynchronously after the plugin list download
            //
            final Runnable pluginInfoDownloadContinuation = new Runnable() {
                @Override
                public void run() {
                    if (pluginInfoDownloadTask.isCanceled())
                        return;
                    model.updateAvailablePlugins(pluginInfoDownloadTask.getAvailablePlugins());
                    // select plugins which actually have to be updated
                    //
                    Iterator<PluginInformation> it = toUpdate.iterator();
                    while(it.hasNext()) {
                        PluginInformation pi = it.next();
                        if (!pi.isUpdateRequired()) {
                            it.remove();
                        }
                    }
                    if (toUpdate.isEmpty()) {
                        alertNothingToUpdate();
                        return;
                    }
                    pluginDownloadTask.setPluginsToDownload(toUpdate);
                    Main.worker.submit(pluginDownloadTask);
                    Main.worker.submit(pluginDownloadContinuation);
                }
            };

            Main.worker.submit(pluginInfoDownloadTask);
            Main.worker.submit(pluginInfoDownloadContinuation);
        }
    }


    /**
     * The action for configuring the plugin download sites
     *
     */
    class ConfigureSitesAction extends AbstractAction {
        public ConfigureSitesAction() {
            putValue(NAME,tr("Configure sites..."));
            putValue(SHORT_DESCRIPTION, tr("Configure the list of sites where plugins are downloaded from"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "settings"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            configureSites();
        }
    }

    /**
     * Applies the current filter condition in the filter text field to the
     * model
     */
    class SearchFieldAdapter implements DocumentListener {
        public void filter() {
            String expr = tfFilter.getText().trim();
            if (expr.isEmpty()) {
                expr = null;
            }
            model.filterDisplayedPlugins(expr);
            pnlPluginPreferences.refreshView();
        }

        @Override
        public void changedUpdate(DocumentEvent arg0) {
            filter();
        }

        @Override
        public void insertUpdate(DocumentEvent arg0) {
            filter();
        }

        @Override
        public void removeUpdate(DocumentEvent arg0) {
            filter();
        }
    }

    private static class PluginConfigurationSitesPanel extends JPanel {

        private DefaultListModel model;

        protected final void build() {
            setLayout(new GridBagLayout());
            add(new JLabel(tr("Add JOSM Plugin description URL.")), GBC.eol());
            model = new DefaultListModel();
            for (String s : Main.pref.getPluginSites()) {
                model.addElement(s);
            }
            final JList list = new JList(model);
            add(new JScrollPane(list), GBC.std().fill());
            JPanel buttons = new JPanel(new GridBagLayout());
            buttons.add(new JButton(new AbstractAction(tr("Add")){
                @Override
                public void actionPerformed(ActionEvent e) {
                    String s = JOptionPane.showInputDialog(
                            JOptionPane.getFrameForComponent(PluginConfigurationSitesPanel.this),
                            tr("Add JOSM Plugin description URL."),
                            tr("Enter URL"),
                            JOptionPane.QUESTION_MESSAGE
                            );
                    if (s != null) {
                        model.addElement(s);
                    }
                }
            }), GBC.eol().fill(GBC.HORIZONTAL));
            buttons.add(new JButton(new AbstractAction(tr("Edit")){
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (list.getSelectedValue() == null) {
                        JOptionPane.showMessageDialog(
                                JOptionPane.getFrameForComponent(PluginConfigurationSitesPanel.this),
                                tr("Please select an entry."),
                                tr("Warning"),
                                JOptionPane.WARNING_MESSAGE
                                );
                        return;
                    }
                    String s = (String)JOptionPane.showInputDialog(
                            Main.parent,
                            tr("Edit JOSM Plugin description URL."),
                            tr("JOSM Plugin description URL"),
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            null,
                            list.getSelectedValue()
                            );
                    if (s != null) {
                        model.setElementAt(s, list.getSelectedIndex());
                    }
                }
            }), GBC.eol().fill(GBC.HORIZONTAL));
            buttons.add(new JButton(new AbstractAction(tr("Delete")){
                @Override
                public void actionPerformed(ActionEvent event) {
                    if (list.getSelectedValue() == null) {
                        JOptionPane.showMessageDialog(
                                JOptionPane.getFrameForComponent(PluginConfigurationSitesPanel.this),
                                tr("Please select an entry."),
                                tr("Warning"),
                                JOptionPane.WARNING_MESSAGE
                                );
                        return;
                    }
                    model.removeElement(list.getSelectedValue());
                }
            }), GBC.eol().fill(GBC.HORIZONTAL));
            add(buttons, GBC.eol());
        }

        public PluginConfigurationSitesPanel() {
            build();
        }

        public List<String> getUpdateSites() {
            if (model.getSize() == 0) return Collections.emptyList();
            List<String> ret = new ArrayList<String>(model.getSize());
            for (int i=0; i< model.getSize();i++){
                ret.add((String)model.get(i));
            }
            return ret;
        }
    }
}
