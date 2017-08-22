// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.actions.ExpertToggleAction.ExpertModeChangeListener;
import org.openstreetmap.josm.actions.RestartAction;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.preferences.advanced.AdvancedPreference;
import org.openstreetmap.josm.gui.preferences.audio.AudioPreference;
import org.openstreetmap.josm.gui.preferences.display.ColorPreference;
import org.openstreetmap.josm.gui.preferences.display.DisplayPreference;
import org.openstreetmap.josm.gui.preferences.display.DrawingPreference;
import org.openstreetmap.josm.gui.preferences.display.LafPreference;
import org.openstreetmap.josm.gui.preferences.display.LanguagePreference;
import org.openstreetmap.josm.gui.preferences.imagery.ImageryPreference;
import org.openstreetmap.josm.gui.preferences.map.BackupPreference;
import org.openstreetmap.josm.gui.preferences.map.MapPaintPreference;
import org.openstreetmap.josm.gui.preferences.map.MapPreference;
import org.openstreetmap.josm.gui.preferences.map.TaggingPresetPreference;
import org.openstreetmap.josm.gui.preferences.plugin.PluginPreference;
import org.openstreetmap.josm.gui.preferences.projection.ProjectionPreference;
import org.openstreetmap.josm.gui.preferences.remotecontrol.RemoteControlPreference;
import org.openstreetmap.josm.gui.preferences.server.AuthenticationPreference;
import org.openstreetmap.josm.gui.preferences.server.OverpassServerPreference;
import org.openstreetmap.josm.gui.preferences.server.ProxyPreference;
import org.openstreetmap.josm.gui.preferences.server.ServerAccessPreference;
import org.openstreetmap.josm.gui.preferences.shortcut.ShortcutPreference;
import org.openstreetmap.josm.gui.preferences.validator.ValidatorPreference;
import org.openstreetmap.josm.gui.preferences.validator.ValidatorTagCheckerRulesPreference;
import org.openstreetmap.josm.gui.preferences.validator.ValidatorTestsPreference;
import org.openstreetmap.josm.plugins.PluginDownloadTask;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.bugreport.BugReportExceptionHandler;

/**
 * The preference settings.
 *
 * @author imi
 */
public final class PreferenceTabbedPane extends JTabbedPane implements MouseWheelListener, ExpertModeChangeListener, ChangeListener {

    private final class PluginDownloadAfterTask implements Runnable {
        private final PluginPreference preference;
        private final PluginDownloadTask task;
        private final Set<PluginInformation> toDownload;

        private PluginDownloadAfterTask(PluginPreference preference, PluginDownloadTask task,
                Set<PluginInformation> toDownload) {
            this.preference = preference;
            this.task = task;
            this.toDownload = toDownload;
        }

        @Override
        public void run() {
            boolean requiresRestart = false;

            for (PreferenceSetting setting : settingsInitialized) {
                if (setting.ok()) {
                    requiresRestart = true;
                }
            }

            // build the messages. We only display one message, including the status information from the plugin download task
            // and - if necessary - a hint to restart JOSM
            //
            StringBuilder sb = new StringBuilder();
            sb.append("<html>");
            if (task != null && !task.isCanceled()) {
                PluginHandler.refreshLocalUpdatedPluginInfo(task.getDownloadedPlugins());
                sb.append(PluginPreference.buildDownloadSummary(task));
            }
            if (requiresRestart) {
                sb.append(tr("You have to restart JOSM for some settings to take effect."));
                sb.append("<br/><br/>");
                sb.append(tr("Would you like to restart now?"));
            }
            sb.append("</html>");

            // display the message, if necessary
            //
            if (requiresRestart) {
                final ButtonSpec[] options = RestartAction.getButtonSpecs();
                if (0 == HelpAwareOptionPane.showOptionDialog(
                        Main.parent,
                        sb.toString(),
                        tr("Restart"),
                        JOptionPane.INFORMATION_MESSAGE,
                        null, /* no special icon */
                        options,
                        options[0],
                        null /* no special help */
                        )) {
                    Main.main.menu.restart.actionPerformed(null);
                }
            } else if (task != null && !task.isCanceled()) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        sb.toString(),
                        tr("Warning"),
                        JOptionPane.WARNING_MESSAGE
                        );
            }

            // load the plugins that can be loaded at runtime
            List<PluginInformation> newPlugins = preference.getNewlyActivatedPlugins();
            if (newPlugins != null) {
                Collection<PluginInformation> downloadedPlugins = null;
                if (task != null && !task.isCanceled()) {
                    downloadedPlugins = task.getDownloadedPlugins();
                }
                List<PluginInformation> toLoad = new ArrayList<>();
                for (PluginInformation pi : newPlugins) {
                    if (toDownload.contains(pi) && downloadedPlugins != null && !downloadedPlugins.contains(pi)) {
                        continue; // failed download
                    }
                    if (pi.canloadatruntime) {
                        toLoad.add(pi);
                    }
                }
                // check if plugin dependences can also be loaded
                Collection<PluginInformation> allPlugins = new HashSet<>(toLoad);
                allPlugins.addAll(PluginHandler.getPlugins());
                boolean removed;
                do {
                    removed = false;
                    Iterator<PluginInformation> it = toLoad.iterator();
                    while (it.hasNext()) {
                        if (!PluginHandler.checkRequiredPluginsPreconditions(null, allPlugins, it.next(), requiresRestart)) {
                            it.remove();
                            removed = true;
                        }
                    }
                } while (removed);

                if (!toLoad.isEmpty()) {
                    PluginHandler.loadPlugins(PreferenceTabbedPane.this, toLoad, null);
                }
            }

            Main.parent.repaint();
        }
    }

    /**
     * Allows PreferenceSettings to do validation of entered values when ok was pressed.
     * If data is invalid then event can return false to cancel closing of preferences dialog.
     * @since 10600 (functional interface)
     */
    @FunctionalInterface
    public interface ValidationListener {
        /**
         *
         * @return True if preferences can be saved
         */
        boolean validatePreferences();
    }

    private interface PreferenceTab {
        TabPreferenceSetting getTabPreferenceSetting();

        Component getComponent();
    }

    public static final class PreferencePanel extends JPanel implements PreferenceTab {
        private final transient TabPreferenceSetting preferenceSetting;

        private PreferencePanel(TabPreferenceSetting preferenceSetting) {
            super(new GridBagLayout());
            CheckParameterUtil.ensureParameterNotNull(preferenceSetting);
            this.preferenceSetting = preferenceSetting;
            buildPanel();
        }

        private void buildPanel() {
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            add(new JLabel(preferenceSetting.getTitle()), GBC.eol().insets(0, 5, 0, 10).anchor(GBC.NORTHWEST));

            JLabel descLabel = new JLabel("<html>"+preferenceSetting.getDescription()+"</html>");
            descLabel.setFont(descLabel.getFont().deriveFont(Font.ITALIC));
            add(descLabel, GBC.eol().insets(5, 0, 5, 20).fill(GBC.HORIZONTAL));
        }

        @Override
        public TabPreferenceSetting getTabPreferenceSetting() {
            return preferenceSetting;
        }

        @Override
        public Component getComponent() {
            return this;
        }
    }

    public static final class PreferenceScrollPane extends JScrollPane implements PreferenceTab {
        private final transient TabPreferenceSetting preferenceSetting;

        private PreferenceScrollPane(Component view, TabPreferenceSetting preferenceSetting) {
            super(view);
            this.preferenceSetting = preferenceSetting;
        }

        private PreferenceScrollPane(PreferencePanel preferencePanel) {
            this(preferencePanel.getComponent(), preferencePanel.getTabPreferenceSetting());
        }

        @Override
        public TabPreferenceSetting getTabPreferenceSetting() {
            return preferenceSetting;
        }

        @Override
        public Component getComponent() {
            return this;
        }
    }

    // all created tabs
    private final transient List<PreferenceTab> tabs = new ArrayList<>();
    private static final Collection<PreferenceSettingFactory> SETTINGS_FACTORIES = new LinkedList<>();
    private static final PreferenceSettingFactory ADVANCED_PREFERENCE_FACTORY = new AdvancedPreference.Factory();
    private final transient List<PreferenceSetting> settings = new ArrayList<>();

    // distinct list of tabs that have been initialized (we do not initialize tabs until they are displayed to speed up dialog startup)
    private final transient List<PreferenceSetting> settingsInitialized = new ArrayList<>();

    final transient List<ValidationListener> validationListeners = new ArrayList<>();

    /**
     * Add validation listener to currently open preferences dialog. Calling to removeValidationListener is not necessary, all listeners will
     * be automatically removed when dialog is closed
     * @param validationListener validation listener to add
     */
    public void addValidationListener(ValidationListener validationListener) {
        validationListeners.add(validationListener);
    }

    /**
     * Construct a PreferencePanel for the preference settings. Layout is GridBagLayout
     * and a centered title label and the description are added.
     * @param caller Preference settings, that display a top level tab
     * @return The created panel ready to add other controls.
     */
    public PreferencePanel createPreferenceTab(TabPreferenceSetting caller) {
        return createPreferenceTab(caller, false);
    }

    /**
     * Construct a PreferencePanel for the preference settings. Layout is GridBagLayout
     * and a centered title label and the description are added.
     * @param caller Preference settings, that display a top level tab
     * @param inScrollPane if <code>true</code> the added tab will show scroll bars
     *        if the panel content is larger than the available space
     * @return The created panel ready to add other controls.
     */
    public PreferencePanel createPreferenceTab(TabPreferenceSetting caller, boolean inScrollPane) {
        CheckParameterUtil.ensureParameterNotNull(caller, "caller");
        PreferencePanel p = new PreferencePanel(caller);

        PreferenceTab tab = p;
        if (inScrollPane) {
            PreferenceScrollPane sp = new PreferenceScrollPane(p);
            tab = sp;
        }
        tabs.add(tab);
        return p;
    }

    @FunctionalInterface
    private interface TabIdentifier {
        boolean identify(TabPreferenceSetting tps, Object param);
    }

    private void selectTabBy(TabIdentifier method, Object param) {
        for (int i = 0; i < getTabCount(); i++) {
            Component c = getComponentAt(i);
            if (c instanceof PreferenceTab) {
                PreferenceTab tab = (PreferenceTab) c;
                if (method.identify(tab.getTabPreferenceSetting(), param)) {
                    setSelectedIndex(i);
                    return;
                }
            }
        }
    }

    public void selectTabByName(String name) {
        selectTabBy((tps, name1) -> name1 != null && tps != null && tps.getIconName() != null && name1.equals(tps.getIconName()), name);
    }

    public void selectTabByPref(Class<? extends TabPreferenceSetting> clazz) {
        selectTabBy((tps, clazz1) -> tps.getClass().isAssignableFrom((Class<?>) clazz1), clazz);
    }

    public boolean selectSubTabByPref(Class<? extends SubPreferenceSetting> clazz) {
        for (PreferenceSetting setting : settings) {
            if (clazz.isInstance(setting)) {
                final SubPreferenceSetting sub = (SubPreferenceSetting) setting;
                final TabPreferenceSetting tab = sub.getTabPreferenceSetting(this);
                selectTabBy((tps, unused) -> tps.equals(tab), null);
                return tab.selectSubTab(sub);
            }
        }
        return false;
    }

    /**
     * Returns the {@code DisplayPreference} object.
     * @return the {@code DisplayPreference} object.
     */
    public DisplayPreference getDisplayPreference() {
        return getSetting(DisplayPreference.class);
    }

    /**
     * Returns the {@code MapPreference} object.
     * @return the {@code MapPreference} object.
     */
    public MapPreference getMapPreference() {
        return getSetting(MapPreference.class);
    }

    /**
     * Returns the {@code PluginPreference} object.
     * @return the {@code PluginPreference} object.
     */
    public PluginPreference getPluginPreference() {
        return getSetting(PluginPreference.class);
    }

    /**
     * Returns the {@code ImageryPreference} object.
     * @return the {@code ImageryPreference} object.
     */
    public ImageryPreference getImageryPreference() {
        return getSetting(ImageryPreference.class);
    }

    /**
     * Returns the {@code ShortcutPreference} object.
     * @return the {@code ShortcutPreference} object.
     */
    public ShortcutPreference getShortcutPreference() {
        return getSetting(ShortcutPreference.class);
    }

    /**
     * Returns the {@code ServerAccessPreference} object.
     * @return the {@code ServerAccessPreference} object.
     * @since 6523
     */
    public ServerAccessPreference getServerPreference() {
        return getSetting(ServerAccessPreference.class);
    }

    /**
     * Returns the {@code ValidatorPreference} object.
     * @return the {@code ValidatorPreference} object.
     * @since 6665
     */
    public ValidatorPreference getValidatorPreference() {
        return getSetting(ValidatorPreference.class);
    }

    /**
     * Saves preferences.
     */
    public void savePreferences() {
        // create a task for downloading plugins if the user has activated, yet not downloaded, new plugins
        final PluginPreference preference = getPluginPreference();
        if (preference != null) {
            final Set<PluginInformation> toDownload = preference.getPluginsScheduledForUpdateOrDownload();
            final PluginDownloadTask task;
            if (toDownload != null && !toDownload.isEmpty()) {
                task = new PluginDownloadTask(this, toDownload, tr("Download plugins"));
            } else {
                task = null;
            }

            // this is the task which will run *after* the plugins are downloaded
            final Runnable continuation = new PluginDownloadAfterTask(preference, task, toDownload);

            if (task != null) {
                // if we have to launch a plugin download task we do it asynchronously, followed
                // by the remaining "save preferences" activites run on the Swing EDT.
                Main.worker.submit(task);
                Main.worker.submit(() -> SwingUtilities.invokeLater(continuation));
            } else {
                // no need for asynchronous activities. Simply run the remaining "save preference"
                // activities on this thread (we are already on the Swing EDT
                continuation.run();
            }
        }
    }

    /**
     * If the dialog is closed with Ok, the preferences will be stored to the preferences-
     * file, otherwise no change of the file happens.
     */
    public PreferenceTabbedPane() {
        super(JTabbedPane.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT);
        super.addMouseWheelListener(this);
        super.getModel().addChangeListener(this);
        ExpertToggleAction.addExpertModeChangeListener(this);
    }

    public void buildGui() {
        Collection<PreferenceSettingFactory> factories = new ArrayList<>(SETTINGS_FACTORIES);
        factories.addAll(PluginHandler.getPreferenceSetting());
        factories.add(ADVANCED_PREFERENCE_FACTORY);

        for (PreferenceSettingFactory factory : factories) {
            if (factory != null) {
                PreferenceSetting setting = factory.createPreferenceSetting();
                if (setting != null) {
                    settings.add(setting);
                }
            }
        }
        addGUITabs(false);
    }

    private void addGUITabsForSetting(Icon icon, TabPreferenceSetting tps) {
        for (PreferenceTab tab : tabs) {
            if (tab.getTabPreferenceSetting().equals(tps)) {
                insertGUITabsForSetting(icon, tps, getTabCount());
            }
        }
    }

    private int insertGUITabsForSetting(Icon icon, TabPreferenceSetting tps, int index) {
        int position = index;
        for (PreferenceTab tab : tabs) {
            if (tab.getTabPreferenceSetting().equals(tps)) {
                insertTab(null, icon, tab.getComponent(), tps.getTooltip(), position++);
            }
        }
        return position - 1;
    }

    private void addGUITabs(boolean clear) {
        boolean expert = ExpertToggleAction.isExpert();
        Component sel = getSelectedComponent();
        if (clear) {
            removeAll();
        }
        // Inspect each tab setting
        for (PreferenceSetting setting : settings) {
            if (setting instanceof TabPreferenceSetting) {
                TabPreferenceSetting tps = (TabPreferenceSetting) setting;
                if (expert || !tps.isExpert()) {
                    // Get icon
                    String iconName = tps.getIconName();
                    ImageIcon icon = null;

                    if (iconName != null && !iconName.isEmpty()) {
                        icon = ImageProvider.get("preferences", iconName, ImageProvider.ImageSizes.SETTINGS_TAB);
                    }
                    if (settingsInitialized.contains(tps)) {
                        // If it has been initialized, add corresponding tab(s)
                        addGUITabsForSetting(icon, tps);
                    } else {
                        // If it has not been initialized, create an empty tab with only icon and tooltip
                        addTab(null, icon, new PreferencePanel(tps), tps.getTooltip());
                    }
                }
            } else if (!(setting instanceof SubPreferenceSetting)) {
                Logging.warn("Ignoring preferences "+setting);
            }
        }
        try {
            if (sel != null) {
                setSelectedComponent(sel);
            }
        } catch (IllegalArgumentException e) {
            Logging.warn(e);
        }
    }

    @Override
    public void expertChanged(boolean isExpert) {
        addGUITabs(true);
    }

    public List<PreferenceSetting> getSettings() {
        return settings;
    }

    @SuppressWarnings("unchecked")
    public <T> T getSetting(Class<? extends T> clazz) {
        for (PreferenceSetting setting:settings) {
            if (clazz.isAssignableFrom(setting.getClass()))
                return (T) setting;
        }
        return null;
    }

    static {
        // order is important!
        SETTINGS_FACTORIES.add(new DisplayPreference.Factory());
        SETTINGS_FACTORIES.add(new DrawingPreference.Factory());
        SETTINGS_FACTORIES.add(new ColorPreference.Factory());
        SETTINGS_FACTORIES.add(new LafPreference.Factory());
        SETTINGS_FACTORIES.add(new LanguagePreference.Factory());
        SETTINGS_FACTORIES.add(new ServerAccessPreference.Factory());
        SETTINGS_FACTORIES.add(new AuthenticationPreference.Factory());
        SETTINGS_FACTORIES.add(new ProxyPreference.Factory());
        SETTINGS_FACTORIES.add(new OverpassServerPreference.Factory());
        SETTINGS_FACTORIES.add(new MapPreference.Factory());
        SETTINGS_FACTORIES.add(new ProjectionPreference.Factory());
        SETTINGS_FACTORIES.add(new MapPaintPreference.Factory());
        SETTINGS_FACTORIES.add(new TaggingPresetPreference.Factory());
        SETTINGS_FACTORIES.add(new BackupPreference.Factory());
        SETTINGS_FACTORIES.add(new PluginPreference.Factory());
        SETTINGS_FACTORIES.add(Main.toolbar);
        SETTINGS_FACTORIES.add(new AudioPreference.Factory());
        SETTINGS_FACTORIES.add(new ShortcutPreference.Factory());
        SETTINGS_FACTORIES.add(new ValidatorPreference.Factory());
        SETTINGS_FACTORIES.add(new ValidatorTestsPreference.Factory());
        SETTINGS_FACTORIES.add(new ValidatorTagCheckerRulesPreference.Factory());
        SETTINGS_FACTORIES.add(new RemoteControlPreference.Factory());
        SETTINGS_FACTORIES.add(new ImageryPreference.Factory());
    }

    /**
     * This mouse wheel listener reacts when a scroll is carried out over the
     * tab strip and scrolls one tab/down or up, selecting it immediately.
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent wev) {
        // Ensure the cursor is over the tab strip
        if (super.indexAtLocation(wev.getPoint().x, wev.getPoint().y) < 0)
            return;

        // Get currently selected tab
        int newTab = super.getSelectedIndex() + wev.getWheelRotation();

        // Ensure the new tab index is sound
        newTab = newTab < 0 ? 0 : newTab;
        newTab = newTab >= super.getTabCount() ? super.getTabCount() - 1 : newTab;

        // select new tab
        super.setSelectedIndex(newTab);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        int index = getSelectedIndex();
        Component sel = getSelectedComponent();
        if (index > -1 && sel instanceof PreferenceTab) {
            PreferenceTab tab = (PreferenceTab) sel;
            TabPreferenceSetting preferenceSettings = tab.getTabPreferenceSetting();
            if (!settingsInitialized.contains(preferenceSettings)) {
                try {
                    getModel().removeChangeListener(this);
                    preferenceSettings.addGui(this);
                    // Add GUI for sub preferences
                    for (PreferenceSetting setting : settings) {
                        if (setting instanceof SubPreferenceSetting) {
                            addSubPreferenceSetting(preferenceSettings, (SubPreferenceSetting) setting);
                        }
                    }
                    Icon icon = getIconAt(index);
                    remove(index);
                    if (index <= insertGUITabsForSetting(icon, preferenceSettings, index)) {
                        setSelectedIndex(index);
                    }
                } catch (SecurityException ex) {
                    Logging.error(ex);
                } catch (RuntimeException ex) { // NOPMD
                    // allow to change most settings even if e.g. a plugin fails
                    BugReportExceptionHandler.handleException(ex);
                } finally {
                    settingsInitialized.add(preferenceSettings);
                    getModel().addChangeListener(this);
                }
            }
        }
    }

    private void addSubPreferenceSetting(TabPreferenceSetting preferenceSettings, SubPreferenceSetting sps) {
        if (sps.getTabPreferenceSetting(this) == preferenceSettings) {
            try {
                sps.addGui(this);
            } catch (SecurityException ex) {
                Logging.error(ex);
            } catch (RuntimeException ex) { // NOPMD
                BugReportExceptionHandler.handleException(ex);
            } finally {
                settingsInitialized.add(sps);
            }
        }
    }
}
