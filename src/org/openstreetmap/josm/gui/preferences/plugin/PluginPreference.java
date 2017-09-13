// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.plugin;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.MainApplication;
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
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

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

    private JosmTextField tfFilter;
    private PluginListPanel pnlPluginPreferences;
    private PluginPreferencesModel model;
    private JScrollPane spPluginPreferences;
    private PluginUpdatePolicyPanel pnlPluginUpdatePolicy;

    /**
     * is set to true if this preference pane has been selected by the user
     */
    private boolean pluginPreferencesActivated;

    private PluginPreference() {
        super(/* ICON(preferences/) */ "plugin", tr("Plugins"), tr("Configure available plugins."), false, new JTabbedPane());
    }

    /**
     * Returns the download summary string to be shown.
     * @param task The plugin download task that has completed
     * @return the download summary string to be shown. Contains summary of success/failed plugins.
     */
    public static String buildDownloadSummary(PluginDownloadTask task) {
        Collection<PluginInformation> downloaded = task.getDownloadedPlugins();
        Collection<PluginInformation> failed = task.getFailedPlugins();
        Exception exception = task.getLastException();
        StringBuilder sb = new StringBuilder();
        if (!downloaded.isEmpty()) {
            sb.append(trn(
                    "The following plugin has been downloaded <strong>successfully</strong>:",
                    "The following {0} plugins have been downloaded <strong>successfully</strong>:",
                    downloaded.size(),
                    downloaded.size()
                    ));
            sb.append("<ul>");
            for (PluginInformation pi: downloaded) {
                sb.append("<li>").append(pi.name).append(" (").append(pi.version).append(")</li>");
            }
            sb.append("</ul>");
        }
        if (!failed.isEmpty()) {
            sb.append(trn(
                    "Downloading the following plugin has <strong>failed</strong>:",
                    "Downloading the following {0} plugins has <strong>failed</strong>:",
                    failed.size(),
                    failed.size()
                    ));
            sb.append("<ul>");
            for (PluginInformation pi: failed) {
                sb.append("<li>").append(pi.name).append("</li>");
            }
            sb.append("</ul>");
        }
        if (exception != null) {
            // Same i18n string in ExceptionUtil.explainBadRequest()
            sb.append(tr("<br>Error message(untranslated): {0}", exception.getMessage()));
        }
        return sb.toString();
    }

    /**
     * Notifies user about result of a finished plugin download task.
     * @param parent The parent component
     * @param task The finished plugin download task
     * @param restartRequired true if a restart is required
     * @since 6797
     */
    public static void notifyDownloadResults(final Component parent, PluginDownloadTask task, boolean restartRequired) {
        final Collection<PluginInformation> failed = task.getFailedPlugins();
        final StringBuilder sb = new StringBuilder();
        sb.append("<html>")
          .append(buildDownloadSummary(task));
        if (restartRequired) {
            sb.append(tr("Please restart JOSM to activate the downloaded plugins."));
        }
        sb.append("</html>");
        if (!GraphicsEnvironment.isHeadless()) {
            GuiHelper.runInEDTAndWait(() -> HelpAwareOptionPane.showOptionDialog(
                    parent,
                    sb.toString(),
                    tr("Update plugins"),
                    !failed.isEmpty() ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE,
                            HelpUtil.ht("/Preferences/Plugins")
                    ));
        }
    }

    private JPanel buildSearchFieldPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        GridBagConstraints gc = new GridBagConstraints();

        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        gc.insets = new Insets(0, 0, 0, 3);
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

    private JPanel buildActionPanel() {
        JPanel pnl = new JPanel(new GridLayout(1, 4));

        pnl.add(new JButton(new DownloadAvailablePluginsAction()));
        pnl.add(new JButton(new UpdateSelectedPluginsAction()));
        ExpertToggleAction.addVisibilitySwitcher(pnl.add(new JButton(new SelectByListAction())));
        ExpertToggleAction.addVisibilitySwitcher(pnl.add(new JButton(new ConfigureSitesAction())));
        return pnl;
    }

    private JPanel buildPluginListPanel() {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.add(buildSearchFieldPanel(), BorderLayout.NORTH);
        model = new PluginPreferencesModel();
        pnlPluginPreferences = new PluginListPanel(model);
        spPluginPreferences = GuiHelper.embedInVerticalScrollPane(pnlPluginPreferences);
        spPluginPreferences.getVerticalScrollBar().addComponentListener(
                new ComponentAdapter() {
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

    private JTabbedPane buildContentPane() {
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
        Main.pref.setPluginSites(pnl.getUpdateSites());
    }

    /**
     * Replies the set of plugins waiting for update or download
     *
     * @return the set of plugins waiting for update or download
     */
    public Set<PluginInformation> getPluginsScheduledForUpdateOrDownload() {
        return model != null ? model.getPluginsScheduledForUpdateOrDownload() : null;
    }

    /**
     * Replies the list of plugins which have been added by the user to the set of activated plugins
     *
     * @return the list of newly activated plugins
     */
    public List<PluginInformation> getNewlyActivatedPlugins() {
        return model != null ? model.getNewlyActivatedPlugins() : null;
    }

    @Override
    public boolean ok() {
        if (!pluginPreferencesActivated)
            return false;
        pnlPluginUpdatePolicy.rememberInPreferences();
        if (model.isActivePluginsChanged()) {
            List<String> l = new LinkedList<>(model.getSelectedPluginNames());
            Collections.sort(l);
            Main.pref.putList("plugins", l);
            if (!model.getNewlyDeactivatedPlugins().isEmpty())
                return true;
            for (PluginInformation pi : model.getNewlyActivatedPlugins()) {
                if (!pi.canloadatruntime)
                    return true;
            }
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
        Runnable r = () -> {
            if (!task.isCanceled()) {
                SwingUtilities.invokeLater(() -> {
                    model.setAvailablePlugins(task.getAvailablePlugins());
                    pnlPluginPreferences.refreshView();
                });
            }
        };
        MainApplication.worker.submit(task);
        MainApplication.worker.submit(r);
    }

    /**
     * The action for downloading the list of available plugins
     */
    class DownloadAvailablePluginsAction extends AbstractAction {

        /**
         * Constructs a new {@code DownloadAvailablePluginsAction}.
         */
        DownloadAvailablePluginsAction() {
            putValue(NAME, tr("Download list"));
            putValue(SHORT_DESCRIPTION, tr("Download the list of available plugins"));
            new ImageProvider("download").getResource().attachImageIcon(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Collection<String> pluginSites = Main.pref.getOnlinePluginSites();
            if (pluginSites.isEmpty()) {
                return;
            }
            final ReadRemotePluginInformationTask task = new ReadRemotePluginInformationTask(pluginSites);
            Runnable continuation = () -> {
                if (!task.isCanceled()) {
                    SwingUtilities.invokeLater(() -> {
                        model.updateAvailablePlugins(task.getAvailablePlugins());
                        pnlPluginPreferences.refreshView();
                        Main.pref.putInt("pluginmanager.version", Version.getInstance().getVersion()); // fix #7030
                    });
                }
            };
            MainApplication.worker.submit(task);
            MainApplication.worker.submit(continuation);
        }
    }

    /**
     * The action for updating the list of selected plugins
     */
    class UpdateSelectedPluginsAction extends AbstractAction {
        UpdateSelectedPluginsAction() {
            putValue(NAME, tr("Update plugins"));
            putValue(SHORT_DESCRIPTION, tr("Update the selected plugins"));
            new ImageProvider("dialogs", "refresh").getResource().attachImageIcon(this);
        }

        protected void alertNothingToUpdate() {
            try {
                SwingUtilities.invokeAndWait(() -> HelpAwareOptionPane.showOptionDialog(
                        pnlPluginPreferences,
                        tr("All installed plugins are up to date. JOSM does not have to download newer versions."),
                        tr("Plugins up to date"),
                        JOptionPane.INFORMATION_MESSAGE,
                        null // FIXME: provide help context
                        ));
            } catch (InterruptedException | InvocationTargetException e) {
                Logging.error(e);
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
            final ReadRemotePluginInformationTask pluginInfoDownloadTask = new ReadRemotePluginInformationTask(
                    Main.pref.getOnlinePluginSites());

            // to be run asynchronously after the plugin download
            //
            final Runnable pluginDownloadContinuation = () -> {
                if (pluginDownloadTask.isCanceled())
                    return;
                boolean restartRequired = false;
                for (PluginInformation pi : pluginDownloadTask.getDownloadedPlugins()) {
                    if (!model.getNewlyActivatedPlugins().contains(pi) || !pi.canloadatruntime) {
                        restartRequired = true;
                        break;
                    }
                }
                notifyDownloadResults(pnlPluginPreferences, pluginDownloadTask, restartRequired);
                model.refreshLocalPluginVersion(pluginDownloadTask.getDownloadedPlugins());
                model.clearPendingPlugins(pluginDownloadTask.getDownloadedPlugins());
                GuiHelper.runInEDT(pnlPluginPreferences::refreshView);
            };

            // to be run asynchronously after the plugin list download
            //
            final Runnable pluginInfoDownloadContinuation = () -> {
                if (pluginInfoDownloadTask.isCanceled())
                    return;
                model.updateAvailablePlugins(pluginInfoDownloadTask.getAvailablePlugins());
                // select plugins which actually have to be updated
                //
                toUpdate.removeIf(pi -> !pi.isUpdateRequired());
                if (toUpdate.isEmpty()) {
                    alertNothingToUpdate();
                    return;
                }
                pluginDownloadTask.setPluginsToDownload(toUpdate);
                MainApplication.worker.submit(pluginDownloadTask);
                MainApplication.worker.submit(pluginDownloadContinuation);
            };

            MainApplication.worker.submit(pluginInfoDownloadTask);
            MainApplication.worker.submit(pluginInfoDownloadContinuation);
        }
    }

    /**
     * The action for configuring the plugin download sites
     *
     */
    class ConfigureSitesAction extends AbstractAction {
        ConfigureSitesAction() {
            putValue(NAME, tr("Configure sites..."));
            putValue(SHORT_DESCRIPTION, tr("Configure the list of sites where plugins are downloaded from"));
            new ImageProvider("dialogs", "settings").getResource().attachImageIcon(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            configureSites();
        }
    }

    /**
     * The action for selecting the plugins given by a text file compatible to JOSM bug report.
     * @author Michael Zangl
     */
    class SelectByListAction extends AbstractAction {
        SelectByListAction() {
            putValue(NAME, tr("Load from list..."));
            putValue(SHORT_DESCRIPTION, tr("Load plugins from a list of plugins"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JTextArea textField = new JTextArea(10, 0);
            JCheckBox deleteNotInList = new JCheckBox(tr("Disable all other plugins"));

            JLabel helpLabel = new JLabel("<html>" + Utils.join("<br/>", Arrays.asList(
                    tr("Enter a list of plugins you want to download."),
                    tr("You should add one plugin id per line, version information is ignored."),
                    tr("You can copy+paste the list of a status report here."))) + "</html>");

            if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(GuiHelper.getFrameForComponent(getTabPane()),
                    new Object[] {helpLabel, new JScrollPane(textField), deleteNotInList},
                    tr("Load plugins from list"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)) {
                activatePlugins(textField, deleteNotInList.isSelected());
            }
        }

        private void activatePlugins(JTextArea textField, boolean deleteNotInList) {
            String[] lines = textField.getText().split("\n");
            List<String> toActivate = new ArrayList<>();
            List<String> notFound = new ArrayList<>();
            // This pattern matches the default list format JOSM uses for bug reports.
            // It removes a list item mark at the beginning of the line: +, -, *
            // It removes the version number after the plugin, like: 123, (123), (v5.7alpha3), (1b3), (v1-SNAPSHOT-1)...
            Pattern regex = Pattern.compile("^[-+\\*\\s]*|\\s[\\d\\s]*(\\([^\\(\\)\\[\\]]*\\))?[\\d\\s]*$");
            for (String line : lines) {
                String name = regex.matcher(line).replaceAll("");
                if (name.isEmpty()) {
                    continue;
                }
                PluginInformation plugin = model.getPluginInformation(name);
                if (plugin == null) {
                    notFound.add(name);
                } else {
                    toActivate.add(name);
                }
            }

            if (notFound.isEmpty() || confirmIgnoreNotFound(notFound)) {
                activatePlugins(toActivate, deleteNotInList);
            }
        }

        private void activatePlugins(List<String> toActivate, boolean deleteNotInList) {
            if (deleteNotInList) {
                for (String name : model.getSelectedPluginNames()) {
                    if (!toActivate.contains(name)) {
                        model.setPluginSelected(name, false);
                    }
                }
            }
            for (String name : toActivate) {
                model.setPluginSelected(name, true);
            }
            pnlPluginPreferences.refreshView();
        }

        private boolean confirmIgnoreNotFound(List<String> notFound) {
            String list = "<ul><li>" + Utils.join("</li><li>", notFound) + "</li></ul>";
            String message = "<html>" + tr("The following plugins were not found. Continue anyway?") + list + "</html>";
            return JOptionPane.showConfirmDialog(GuiHelper.getFrameForComponent(getTabPane()),
                    message) == JOptionPane.OK_OPTION;
        }
    }

    /**
     * Applies the current filter condition in the filter text field to the model.
     */
    class SearchFieldAdapter implements DocumentListener {
        private void filter() {
            String expr = tfFilter.getText().trim();
            if (expr.isEmpty()) {
                expr = null;
            }
            model.filterDisplayedPlugins(expr);
            pnlPluginPreferences.refreshView();
        }

        @Override
        public void changedUpdate(DocumentEvent evt) {
            filter();
        }

        @Override
        public void insertUpdate(DocumentEvent evt) {
            filter();
        }

        @Override
        public void removeUpdate(DocumentEvent evt) {
            filter();
        }
    }

    private static class PluginConfigurationSitesPanel extends JPanel {

        private final DefaultListModel<String> model = new DefaultListModel<>();

        PluginConfigurationSitesPanel() {
            super(new GridBagLayout());
            add(new JLabel(tr("Add JOSM Plugin description URL.")), GBC.eol());
            for (String s : Main.pref.getPluginSites()) {
                model.addElement(s);
            }
            final JList<String> list = new JList<>(model);
            add(new JScrollPane(list), GBC.std().fill());
            JPanel buttons = new JPanel(new GridBagLayout());
            buttons.add(new JButton(new AbstractAction(tr("Add")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String s = JOptionPane.showInputDialog(
                            GuiHelper.getFrameForComponent(PluginConfigurationSitesPanel.this),
                            tr("Add JOSM Plugin description URL."),
                            tr("Enter URL"),
                            JOptionPane.QUESTION_MESSAGE
                            );
                    if (s != null && !s.isEmpty()) {
                        model.addElement(s);
                    }
                }
            }), GBC.eol().fill(GBC.HORIZONTAL));
            buttons.add(new JButton(new AbstractAction(tr("Edit")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (list.getSelectedValue() == null) {
                        JOptionPane.showMessageDialog(
                                GuiHelper.getFrameForComponent(PluginConfigurationSitesPanel.this),
                                tr("Please select an entry."),
                                tr("Warning"),
                                JOptionPane.WARNING_MESSAGE
                                );
                        return;
                    }
                    String s = (String) JOptionPane.showInputDialog(
                            Main.parent,
                            tr("Edit JOSM Plugin description URL."),
                            tr("JOSM Plugin description URL"),
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            null,
                            list.getSelectedValue()
                            );
                    if (s != null && !s.isEmpty()) {
                        model.setElementAt(s, list.getSelectedIndex());
                    }
                }
            }), GBC.eol().fill(GBC.HORIZONTAL));
            buttons.add(new JButton(new AbstractAction(tr("Delete")) {
                @Override
                public void actionPerformed(ActionEvent event) {
                    if (list.getSelectedValue() == null) {
                        JOptionPane.showMessageDialog(
                                GuiHelper.getFrameForComponent(PluginConfigurationSitesPanel.this),
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

        protected List<String> getUpdateSites() {
            if (model.getSize() == 0)
                return Collections.emptyList();
            List<String> ret = new ArrayList<>(model.getSize());
            for (int i = 0; i < model.getSize(); i++) {
                ret.add(model.get(i));
            }
            return ret;
        }
    }
}
