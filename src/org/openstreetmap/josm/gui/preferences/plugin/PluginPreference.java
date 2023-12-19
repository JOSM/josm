// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.plugin;

import static java.awt.GridBagConstraints.HORIZONTAL;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.preferences.ExtensibleTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.FilterField;
import org.openstreetmap.josm.plugins.PluginDownloadTask;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.ReadLocalPluginInformationTask;
import org.openstreetmap.josm.plugins.ReadRemotePluginInformationTask;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Preference settings for plugins.
 * @since 168
 */
public final class PluginPreference extends ExtensibleTabPreferenceSetting {

    /**
     * Factory used to create a new {@code PluginPreference}.
     */
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new PluginPreference();
        }
    }

    private PluginListPanel pnlPluginPreferences;
    private PluginPreferencesModel model;
    private JScrollPane spPluginPreferences;
    private PluginUpdatePolicyPanel pnlPluginUpdatePolicy;

    /**
     * is set to true if this preference pane has been selected by the user
     */
    private boolean pluginPreferencesActivated;

    private PluginPreference() {
        super(/* ICON(preferences/) */ "plugin", tr("Plugins"), tr("Configure available plugins."), false);
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
        GuiHelper.runInEDTAndWait(() -> HelpAwareOptionPane.showOptionDialog(
                parent,
                sb.toString(),
                tr("Update plugins"),
                !failed.isEmpty() ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE,
                        HelpUtil.ht("/Preferences/Plugins")
                ));
    }

    private JPanel buildSearchFieldPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.add(GBC.glue(0, 0));

        ButtonGroup bg = new ButtonGroup();
        JPanel radios = new JPanel();
        addRadioButton(bg, radios, new JRadioButton(trc("plugins", "All"), true), PluginInstallation.ALL);
        addRadioButton(bg, radios, new JRadioButton(trc("plugins", "Installed")), PluginInstallation.INSTALLED);
        addRadioButton(bg, radios, new JRadioButton(trc("plugins", "Available")), PluginInstallation.AVAILABLE);
        pnl.add(radios, GBC.eol().fill(HORIZONTAL));

        pnl.add(new FilterField().filter(expr -> {
            model.filterDisplayedPlugins(expr);
            pnlPluginPreferences.refreshView();
        }), GBC.eol().insets(0, 0, 0, 5).fill(HORIZONTAL));
        return pnl;
    }

    private void addRadioButton(ButtonGroup bg, JPanel pnl, JRadioButton rb, PluginInstallation value) {
        bg.add(rb);
        pnl.add(rb, GBC.std());
        rb.addActionListener(e -> {
            model.filterDisplayedPlugins(value);
            pnlPluginPreferences.refreshView();
        });
    }

    private static Component addButton(JPanel pnl, JButton button, String buttonName) {
        button.setName(buttonName);
        return pnl.add(button);
    }

    private JPanel buildActionPanel() {
        JPanel pnl = new JPanel(new GridLayout(1, 4));

        // assign some component names to these as we go to aid testing
        addButton(pnl, new JButton(new DownloadAvailablePluginsAction()), "downloadListButton");
        addButton(pnl, new JButton(new UpdateSelectedPluginsAction()), "updatePluginsButton");
        ExpertToggleAction.addVisibilitySwitcher(addButton(pnl, new JButton(new SelectByListAction()), "loadFromListButton"));
        ExpertToggleAction.addVisibilitySwitcher(addButton(pnl, new JButton(new ConfigureSitesAction()), "configureSitesButton"));
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

    @Override
    public void addGui(final PreferenceTabbedPane gui) {
        JTabbedPane pane = getTabPane();
        pnlPluginUpdatePolicy = new PluginUpdatePolicyPanel();
        pane.addTab(tr("Plugins"), buildPluginListPanel());
        pane.addTab(tr("Plugin update policy"), pnlPluginUpdatePolicy);
        super.addGui(gui);
        readLocalPluginInformation();
        pluginPreferencesActivated = true;
    }

    private void configureSites() {
        ButtonSpec[] options = {
                new ButtonSpec(
                        tr("OK"),
                        new ImageProvider("ok"),
                        tr("Accept the new plugin sites and close the dialog"),
                        null /* no special help topic */
                        ),
                        new ButtonSpec(
                                tr("Cancel"),
                                new ImageProvider("cancel"),
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
        Preferences.main().setPluginSites(pnl.getUpdateSites());
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
            Config.getPref().putList("plugins", l);
            List<PluginInformation> deactivatedPlugins = model.getNewlyDeactivatedPlugins();
            if (!deactivatedPlugins.isEmpty()) {
                boolean requiresRestart = PluginHandler.removePlugins(deactivatedPlugins);
                if (requiresRestart)
                    return true;
            }
            return model.getNewlyActivatedPlugins().stream().anyMatch(pi -> !pi.canloadatruntime);
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
                    pnlPluginPreferences.resetDisplayedComponents();
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
            Collection<String> pluginSites = Preferences.main().getOnlinePluginSites();
            if (pluginSites.isEmpty()) {
                return;
            }
            final ReadRemotePluginInformationTask task = new ReadRemotePluginInformationTask(pluginSites);
            Runnable continuation = () -> {
                if (!task.isCanceled()) {
                    SwingUtilities.invokeLater(() -> {
                        model.updateAvailablePlugins(task.getAvailablePlugins());
                        pnlPluginPreferences.resetDisplayedComponents();
                        pnlPluginPreferences.refreshView();
                        Config.getPref().putInt("pluginmanager.version", Version.getInstance().getVersion()); // fix #7030
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
                    Preferences.main().getOnlinePluginSites());

            // to be run asynchronously after the plugin download
            //
            final Runnable pluginDownloadContinuation = () -> {
                if (pluginDownloadTask.isCanceled())
                    return;
                boolean restartRequired = pluginDownloadTask.getDownloadedPlugins().stream()
                        .anyMatch(pi -> !(model.getNewlyActivatedPlugins().contains(pi) && pi.canloadatruntime));
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
                int toUpdateSize;
                boolean refreshRequired = false;
                do {
                    toUpdateSize = toUpdate.size();
                    Set<PluginInformation> enabledPlugins = new HashSet<>(PluginHandler.getPlugins());
                    enabledPlugins.addAll(toUpdate);
                    Set<PluginInformation> toAdd = new HashSet<>();
                    for (PluginInformation pi : toUpdate) {
                        if (!PluginHandler.checkRequiredPluginsPreconditions(null, enabledPlugins, pi, false)) {
                            // Time to find the missing plugins...
                            toAdd.addAll(pi.getRequiredPlugins().stream().filter(plugin -> PluginHandler.getPlugin(plugin) == null)
                                    .map(plugin -> model.getPluginInformation(plugin))
                                    .collect(Collectors.toSet()));
                        }
                    }
                    toAdd.forEach(plugin -> model.setPluginSelected(plugin.name, true));
                    refreshRequired |= !toAdd.isEmpty(); // We need to force refresh the checkboxes if we are adding new plugins
                    toAdd.removeIf(plugin -> !plugin.isUpdateRequired()); // Avoid downloading plugins that already exist
                    toUpdate.addAll(toAdd);
                } while (toUpdateSize != toUpdate.size());

                pluginDownloadTask.setPluginsToDownload(toUpdate);
                MainApplication.worker.submit(pluginDownloadTask);
                MainApplication.worker.submit(pluginDownloadContinuation);
                if (refreshRequired) {
                    // Needed since we need to recreate the checkboxes to show the enabled dependent plugins that were not previously enabled
                    pnlPluginPreferences.resetDisplayedComponents();
                }
                GuiHelper.runInEDT(pnlPluginPreferences::refreshView);
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
            new ImageProvider("preference").getResource().attachImageIcon(this);
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
            new ImageProvider("misc/statusreport").getResource().attachImageIcon(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JTextArea textField = new JTextArea(10, 0);
            JCheckBox deleteNotInList = new JCheckBox(tr("Disable all other plugins"));

            JLabel helpLabel = new JLabel("<html>" + String.join("<br/>",
                    tr("Enter a list of plugins you want to download."),
                    tr("You should add one plugin id per line, version information is ignored."),
                    tr("You can copy+paste the list of a status report here.")) + "</html>");

            if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(GuiHelper.getFrameForComponent(getTabPane()),
                    new Object[] {helpLabel, new JScrollPane(textField), deleteNotInList},
                    tr("Load plugins from list"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)) {
                activatePlugins(textField, deleteNotInList.isSelected());
            }
        }

        private void activatePlugins(JTextArea textField, boolean deleteNotInList) {
            String[] lines = textField.getText().split("\n", -1);
            List<String> toActivate = new ArrayList<>();
            List<String> notFound = new ArrayList<>();
            // This pattern matches the default list format JOSM uses for bug reports.
            // It removes a list item mark at the beginning of the line: +, -, *
            // It removes the version number after the plugin, like: 123, (123), (v5.7alpha3), (1b3), (v1-SNAPSHOT-1)...
            Pattern regex = Pattern.compile("^[-+*\\s]*|\\s[\\d\\s]*(\\([^()\\[\\]]*\\))?[\\d\\s]*$");
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
            String list = "<ul><li>" + String.join("</li><li>", notFound) + "</li></ul>";
            String message = "<html>" + tr("The following plugins were not found. Continue anyway?") + list + "</html>";
            return JOptionPane.showConfirmDialog(GuiHelper.getFrameForComponent(getTabPane()),
                    message) == JOptionPane.OK_OPTION;
        }
    }

    private static class PluginConfigurationSitesPanel extends JPanel {

        private final DefaultListModel<String> model = new DefaultListModel<>();

        PluginConfigurationSitesPanel() {
            super(new GridBagLayout());
            add(new JLabel(tr("Add JOSM Plugin description URL.")), GBC.eol());
            for (String s : Preferences.main().getPluginSites()) {
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
                    if (!Utils.isEmpty(s)) {
                        model.addElement(s);
                    }
                }
            }), GBC.eol().fill(HORIZONTAL));
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
                            MainApplication.getMainFrame(),
                            tr("Edit JOSM Plugin description URL."),
                            tr("JOSM Plugin description URL"),
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            null,
                            list.getSelectedValue()
                            );
                    if (!Utils.isEmpty(s)) {
                        model.setElementAt(s, list.getSelectedIndex());
                    }
                }
            }), GBC.eol().fill(HORIZONTAL));
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
            }), GBC.eol().fill(HORIZONTAL));
            add(buttons, GBC.eol());
        }

        protected List<String> getUpdateSites() {
            if (model.getSize() == 0)
                return Collections.emptyList();
            return IntStream.range(0, model.getSize())
                    .mapToObj(model::get)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public String getHelpContext() {
        return HelpUtil.ht("/Preferences/Plugins");
    }
}
