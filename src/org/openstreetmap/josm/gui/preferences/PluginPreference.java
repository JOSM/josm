//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

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
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.preferences.plugin.PluginListPanel;
import org.openstreetmap.josm.gui.preferences.plugin.PluginPreferencesModel;
import org.openstreetmap.josm.gui.preferences.plugin.PluginUpdatePolicyPanel;
import org.openstreetmap.josm.gui.widgets.SelectAllOnFocusGainedDecorator;
import org.openstreetmap.josm.plugins.PluginDownloadTask;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.ReadLocalPluginInformationTask;
import org.openstreetmap.josm.plugins.ReadRemotePluginInformationTask;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

public class PluginPreference implements PreferenceSetting {
    @SuppressWarnings("unused")
    private final static Logger logger = Logger.getLogger(PluginPreference.class.getName());

    public static class Factory implements PreferenceSettingFactory {
        public PreferenceSetting createPreferenceSetting() {
            return new PluginPreference();
        }
    }

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
                sb.append("<li>").append(pi.name).append("</li>");
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

    private JTextField tfFilter;
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
        pnl.add(tfFilter = new JTextField(), gc);
        tfFilter.setToolTipText(tr("Enter a search expression"));
        SelectAllOnFocusGainedDecorator.decorate(tfFilter);
        tfFilter.getDocument().addDocumentListener(new SearchFieldAdapter());
        return pnl;
    }

    protected JPanel buildActionPanel() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER));

        pnl.add(new JButton(new DownloadAvailablePluginsAction()));
        pnl.add(new JButton(new UpdateSelectedPluginsAction()));
        pnl.add(new JButton(new ConfigureSitesAction()));
        return pnl;
    }

    protected JPanel buildPluginListPanel() {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.add(buildSearchFieldPanel(), BorderLayout.NORTH);
        model  = new PluginPreferencesModel();
        spPluginPreferences = new JScrollPane(pnlPluginPreferences = new PluginListPanel(model));
        spPluginPreferences.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        spPluginPreferences.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
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

    protected JPanel buildContentPanel() {
        JPanel pnl = new JPanel(new BorderLayout());
        JTabbedPane tpPluginPreferences = new JTabbedPane();
        tpPluginPreferences.add(buildPluginListPanel());
        tpPluginPreferences.add(pnlPluginUpdatePolicy  =new PluginUpdatePolicyPanel());
        tpPluginPreferences.setTitleAt(0, tr("Plugins"));
        tpPluginPreferences.setTitleAt(1, tr("Plugin update policy"));

        pnl.add(tpPluginPreferences, BorderLayout.CENTER);
        return pnl;
    }

    public void addGui(final PreferenceTabbedPane gui) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.BOTH;
        gui.plugins.add(buildContentPanel(), gc);
        pnlPluginPreferences.refreshView();
        gui.addChangeListener(new PluginPreferenceActivationListener(gui.plugins));
    }

    private void configureSites() {
        JPanel p = new JPanel(new GridBagLayout());
        p.add(new JLabel(tr("Add JOSM Plugin description URL.")), GBC.eol());
        final DefaultListModel model = new DefaultListModel();
        for (String s : Main.pref.getPluginSites()) {
            model.addElement(s);
        }
        final JList list = new JList(model);
        p.add(new JScrollPane(list), GBC.std().fill());
        JPanel buttons = new JPanel(new GridBagLayout());
        buttons.add(new JButton(new AbstractAction(tr("Add")){
            public void actionPerformed(ActionEvent e) {
                String s = JOptionPane.showInputDialog(
                        JOptionPane.getFrameForComponent(pnlPluginPreferences),
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
            public void actionPerformed(ActionEvent e) {
                if (list.getSelectedValue() == null) {
                    JOptionPane.showMessageDialog(
                            JOptionPane.getFrameForComponent(pnlPluginPreferences),
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
                model.setElementAt(s, list.getSelectedIndex());
            }
        }), GBC.eol().fill(GBC.HORIZONTAL));
        buttons.add(new JButton(new AbstractAction(tr("Delete")){
            public void actionPerformed(ActionEvent event) {
                if (list.getSelectedValue() == null) {
                    JOptionPane.showMessageDialog(
                            JOptionPane.getFrameForComponent(pnlPluginPreferences),
                            tr("Please select an entry."),
                            tr("Warning"),
                            JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }
                model.removeElement(list.getSelectedValue());
            }
        }), GBC.eol().fill(GBC.HORIZONTAL));
        p.add(buttons, GBC.eol());
        int answer = JOptionPane.showConfirmDialog(
                JOptionPane.getFrameForComponent(pnlPluginPreferences),
                p,
                tr("Configure Plugin Sites"), JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION)
            return;
        Collection<String> sites = new LinkedList<String>();
        for (int i = 0; i < model.getSize(); ++i) {
            sites.add((String)model.getElementAt(i));
        }
        Main.pref.setPluginSites(sites);
    }

    /**
     * Replies the list of plugins waiting for update or download
     * 
     * @return the list of plugins waiting for update or download
     */
    public List<PluginInformation> getPluginsScheduledForUpdateOrDownload() {
        return model.getPluginsScheduledForUpdateOrDownload();
    }

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
            public void run() {
                if (task.isCanceled()) return;
                SwingUtilities.invokeLater(new Runnable() {
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

        public void actionPerformed(ActionEvent e) {
            final ReadRemotePluginInformationTask task = new ReadRemotePluginInformationTask(Main.pref.getPluginSites());
            Runnable continuation = new Runnable() {
                public void run() {
                    if (task.isCanceled()) return;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            model.setAvailablePlugins(task.getAvailabePlugins());
                            pnlPluginPreferences.refreshView();

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

        protected void notifyDownloadResults(PluginDownloadTask task) {
            Collection<PluginInformation> downloaded = task.getDownloadedPlugins();
            Collection<PluginInformation> failed = task.getFailedPlugins();
            StringBuilder sb = new StringBuilder();
            sb.append("<html>");
            sb.append(buildDownloadSummary(task));
            if (!downloaded.isEmpty()) {
                sb.append("Please restart JOSM to activate the downloaded plugins.");
            }
            sb.append("</html>");
            HelpAwareOptionPane.showOptionDialog(
                    pnlPluginPreferences,
                    sb.toString(),
                    tr("Update plugins"),
                    failed.isEmpty() ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE,
                            // FIXME: check help topic
                            HelpUtil.ht("/Preferences/Plugin")
            );
        }

        public void actionPerformed(ActionEvent e) {
            List<PluginInformation> toUpdate = model.getSelectedPlugins();
            final PluginDownloadTask task = new PluginDownloadTask(
                    pnlPluginPreferences,
                    toUpdate,
                    tr("Update plugins")
            );
            Runnable r = new Runnable() {
                public void run() {
                    if (task.isCanceled())
                        return;
                    notifyDownloadResults(task);
                    model.refreshLocalPluginVersion(task.getDownloadedPlugins());
                    pnlPluginPreferences.refreshView();
                }
            };
            Main.worker.submit(task);
            Main.worker.submit(r);
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

        public void actionPerformed(ActionEvent e) {
            configureSites();
        }
    }

    /**
     * Listens to the activation of the plugin preferences tab. On activation it
     * reloads plugin information from the local file system.
     *
     */
    class PluginPreferenceActivationListener implements ChangeListener {
        private Component pane;
        public PluginPreferenceActivationListener(Component preferencesPane) {
            pane = preferencesPane;
        }

        public void stateChanged(ChangeEvent e) {
            JTabbedPane tp = (JTabbedPane)e.getSource();
            if (tp.getSelectedComponent() == pane) {
                readLocalPluginInformation();
                pluginPreferencesActivated = true;
            }
        }
    }

    /**
     * Applies the current filter condition in the filter text field to the
     * model
     */
    class SearchFieldAdapter implements DocumentListener {
        public void filter() {
            String expr = tfFilter.getText().trim();
            if (expr.equals("")) {
                expr = null;
            }
            model.filterDisplayedPlugins(expr);
            pnlPluginPreferences.refreshView();
        }

        public void changedUpdate(DocumentEvent arg0) {
            filter();
        }

        public void insertUpdate(DocumentEvent arg0) {
            filter();
        }

        public void removeUpdate(DocumentEvent arg0) {
            filter();
        }
    }
}
