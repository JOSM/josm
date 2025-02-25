// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.actions.ExpertToggleAction.ExpertModeChangeListener;
import org.openstreetmap.josm.actions.RestartAction;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.HelpAwareOptionPane.ButtonSpec;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.preferences.advanced.AdvancedPreference;
import org.openstreetmap.josm.gui.preferences.audio.AudioPreference;
import org.openstreetmap.josm.gui.preferences.display.ColorPreference;
import org.openstreetmap.josm.gui.preferences.display.DisplayPreference;
import org.openstreetmap.josm.gui.preferences.display.DrawingPreference;
import org.openstreetmap.josm.gui.preferences.display.GPXPreference;
import org.openstreetmap.josm.gui.preferences.display.LafPreference;
import org.openstreetmap.josm.gui.preferences.display.LanguagePreference;
import org.openstreetmap.josm.gui.preferences.imagery.ImageryPreference;
import org.openstreetmap.josm.gui.preferences.map.BackupPreference;
import org.openstreetmap.josm.gui.preferences.map.MapPaintPreference;
import org.openstreetmap.josm.gui.preferences.map.TaggingPresetPreference;
import org.openstreetmap.josm.gui.preferences.plugin.PluginPreference;
import org.openstreetmap.josm.gui.preferences.projection.ProjectionPreference;
import org.openstreetmap.josm.gui.preferences.remotecontrol.RemoteControlPreference;
import org.openstreetmap.josm.gui.preferences.server.ProxyPreference;
import org.openstreetmap.josm.gui.preferences.server.ServerAccessPreference;
import org.openstreetmap.josm.gui.preferences.shortcut.ShortcutPreference;
import org.openstreetmap.josm.gui.preferences.validator.ValidatorPreference;
import org.openstreetmap.josm.gui.preferences.validator.ValidatorTagCheckerRulesPreference;
import org.openstreetmap.josm.gui.preferences.validator.ValidatorTestsPreference;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.plugins.PluginDownloadTask;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.bugreport.BugReportExceptionHandler;

/**
 * The preference settings.
 *
 * @author imi
 */
public final class PreferenceTabbedPane extends JTabbedPane implements ExpertModeChangeListener, ChangeListener {

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
                sb.append(tr("You have to restart JOSM for some settings to take effect."))
                    .append("<br/><br/>")
                    .append(tr("Would you like to restart now?"));
            }
            sb.append("</html>");

            // display the message, if necessary
            //
            if (requiresRestart) {
                final ButtonSpec[] options = RestartAction.getButtonSpecs();
                if (0 == HelpAwareOptionPane.showOptionDialog(
                        MainApplication.getMainFrame(),
                        sb.toString(),
                        tr("Restart"),
                        JOptionPane.INFORMATION_MESSAGE,
                        null, /* no special icon */
                        options,
                        options[0],
                        null /* no special help */
                        )) {
                    MainApplication.getMenu().restart.actionPerformed(null);
                }
            } else if (task != null && !task.isCanceled()) {
                Collection<PluginInformation> failed = task.getFailedPlugins();
                JOptionPane.showMessageDialog(
                        MainApplication.getMainFrame(),
                        sb.toString(),
                        !failed.isEmpty() ? tr("Warning") : tr("Information"),
                        !failed.isEmpty() ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE
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
                // check if plugin dependencies can also be loaded
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

            if (MainApplication.getMainFrame() != null) {
                MainApplication.getMainFrame().repaint();
            }
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
         * Determines if preferences can be saved.
         * @return True if preferences can be saved
         */
        boolean validatePreferences();
    }

    private interface PreferenceTab {
        TabPreferenceSetting getTabPreferenceSetting();

        Component getComponent();
    }

    /**
     * Panel used for preference settings.
     * @since 4968
     */
    public static final class PreferencePanel extends JPanel implements PreferenceTab {
        private final transient TabPreferenceSetting preferenceSetting;

        private PreferencePanel(TabPreferenceSetting preferenceSetting) {
            super(new GridBagLayout());
            this.preferenceSetting = Objects.requireNonNull(preferenceSetting, "preferenceSetting");
            buildPanel();
        }

        private void buildPanel() {
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            JPanel headerPanel = new JPanel(new BorderLayout());
            add(headerPanel, GBC.eop().fill(GridBagConstraints.HORIZONTAL));

            JLabel label = new JLabel("<html>" +
                    "<b>" + preferenceSetting.getTitle() + "</b><br>" +
                    "<i>" + preferenceSetting.getDescription() + "</i></html>");
            label.setFont(label.getFont().deriveFont(Font.PLAIN));
            headerPanel.add(label, BorderLayout.CENTER);

            ImageIcon icon = preferenceSetting.getIcon(ImageProvider.ImageSizes.SETTINGS_TAB);
            headerPanel.add(new JLabel(icon), BorderLayout.EAST);
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

    /**
     * Scroll pane used for large {@link PreferencePanel}s.
     * @since 4968
     */
    public static final class PreferenceScrollPane extends JScrollPane implements PreferenceTab {
        private final transient TabPreferenceSetting preferenceSetting;

        private PreferenceScrollPane(PreferencePanel preferencePanel) {
            super(preferencePanel.getComponent());
            this.preferenceSetting = preferencePanel.getTabPreferenceSetting();
            GuiHelper.setDefaultIncrement(this);
            setBorder(BorderFactory.createEmptyBorder());
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
        PreferencePanel p = new PreferencePanel(Objects.requireNonNull(caller, "caller"));
        tabs.add(inScrollPane ? new PreferenceScrollPane(p) : p);
        return p;
    }

    private OptionalInt indexOfTab(Predicate<TabPreferenceSetting> predicate) {
        return IntStream.range(0, getTabCount())
                .filter(i -> getComponentAt(i) instanceof PreferenceTab
                        && predicate.test(((PreferenceTab) getComponentAt(i)).getTabPreferenceSetting()))
                .findFirst();
    }

    private void selectTabBy(Predicate<TabPreferenceSetting> predicate) {
        setSelectedIndex(indexOfTab(predicate).orElse(0));
    }

    /**
     * Selects a {@link TabPreferenceSetting} by its icon name
     * @param name the icon name
     */
    public void selectTabByName(String name) {
        Objects.requireNonNull(name);
        selectTabBy(tps -> Objects.equals(name, tps.getIconName()));
    }

    /**
     * Selects a {@link TabPreferenceSetting} by class
     * @param clazz preferences tab class
     */
    public void selectTabByPref(Class<? extends TabPreferenceSetting> clazz) {
        selectTabBy(clazz::isInstance);
    }

    /**
     * Selects a {@link SubPreferenceSetting} by class
     * @param clazz sub preferences tab class
     * @return true if the specified preference settings have been selected, false otherwise.
     */
    public boolean selectSubTabByPref(Class<? extends SubPreferenceSetting> clazz) {
        try {
            final SubPreferenceSetting sub = getSetting(clazz);
            final TabPreferenceSetting tab = sub.getTabPreferenceSetting(this);
            selectTabBy(tps -> tps.equals(tab));
            return tab.selectSubTab(sub);
        } catch (NoSuchElementException e) {
            Logging.trace(e);
            return false;
        }
    }

    /**
     * Returns the currently selected preference and sub preference setting
     * @return the currently selected preference and sub preference setting
     */
    public Pair<Class<? extends TabPreferenceSetting>, Class<? extends SubPreferenceSetting>> getSelectedTab() {
        final Component selected = getSelectedComponent();
        if (selected instanceof PreferenceTab) {
            final TabPreferenceSetting setting = ((PreferenceTab) selected).getTabPreferenceSetting();
            return Pair.create(setting.getClass(), setting.getSelectedSubTab());
        } else {
            return null;
        }
    }

    /**
     * Returns the {@code DisplayPreference} object.
     * @return the {@code DisplayPreference} object.
     */
    public DisplayPreference getDisplayPreference() {
        return getSetting(DisplayPreference.class);
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
            if (!Utils.isEmpty(toDownload)) {
                task = new PluginDownloadTask(this, toDownload, tr("Download plugins"));
            } else {
                task = null;
            }

            // this is the task which will run *after* the plugins are downloaded
            final Runnable continuation = new PluginDownloadAfterTask(preference, task, toDownload);

            if (task != null) {
                // if we have to launch a plugin download task we do it asynchronously, followed
                // by the remaining "save preferences" activities run on the Swing EDT.
                MainApplication.worker.submit(task);
                MainApplication.worker.submit(() -> GuiHelper.runInEDT(continuation));
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
        super(SwingConstants.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT);
        super.addMouseWheelListener(new WheelListener(this));
        ExpertToggleAction.addExpertModeChangeListener(this);
    }

    /**
     * Constructs GUI.
     */
    public void buildGui() {
        Collection<PreferenceSettingFactory> factories = new ArrayList<>(SETTINGS_FACTORIES);
        factories.addAll(PluginHandler.getPreferenceSetting());
        factories.add(ADVANCED_PREFERENCE_FACTORY);

        for (PreferenceSettingFactory factory : factories) {
            if (factory != null) {
                PreferenceSetting setting = factory.createPreferenceSetting();
                if (setting instanceof TabPreferenceSetting && ((TabPreferenceSetting) setting).getIconName() == null) {
                    Logging.error("Invalid setting (Icon missing): " + setting.getClass().getName());
                    setting = null;
                }
                if (setting != null) {
                    settings.add(setting);
                }
            }
        }
        addGUITabs(false);
        super.getModel().addChangeListener(this);
    }

    private void addGUITabsForSetting(Icon icon, TabPreferenceSetting tps, int maxWidth) {
        for (PreferenceTab tab : tabs) {
            if (tab.getTabPreferenceSetting().equals(tps)) {
                insertGUITabsForSetting(icon, tps, tab.getComponent(), getTabCount(), maxWidth);
            }
        }
    }

    private int insertGUITabsForSetting(Icon icon, TabPreferenceSetting tps, int index, int maxWidth) {
        int position = index;
        for (PreferenceTab tab : tabs) {
            if (tab.getTabPreferenceSetting().equals(tps)) {
                insertGUITabsForSetting(icon, tps, tab.getComponent(), position, maxWidth);
                position++;
            }
        }
        return position - 1;
    }

    private void insertGUITabsForSetting(Icon icon, TabPreferenceSetting tps, final Component component, int position, int maxWidth) {
        // macOS / AquaLookAndFeel does not support horizontal tabs, see https://josm.openstreetmap.de/ticket/7548#comment:80
        String title = "Aqua".equals(UIManager.getLookAndFeel().getID()) ? null : htmlTabTitle(tps.getTitle(), maxWidth);
        insertTab(title, icon, component, tps.getTooltip(), position);
    }

    private static String htmlTabTitle(String title, int maxWidth) {
        // Width is set to force left alignment, see https://stackoverflow.com/a/33781096/2257172
        return "<html><div style='padding-left:5px; width:" + maxWidth + "px'>" + title + "</div></html>";
    }

    private void addGUITabs(boolean clear) {
        boolean expert = ExpertToggleAction.isExpert();
        if (clear) {
            removeAll();
        }
        // Compute max tab length in pixels
        int maxWidth = computeMaxTabWidth();
        // Inspect each tab setting
        for (PreferenceSetting setting : settings) {
            if (setting instanceof TabPreferenceSetting) {
                TabPreferenceSetting tps = (TabPreferenceSetting) setting;
                if (expert || !tps.isExpert()) {
                    ImageIcon icon = tps.getIcon(ImageProvider.ImageSizes.LARGEICON);
                    if (settingsInitialized.contains(tps)) {
                        // If it has been initialized, add corresponding tab(s)
                        addGUITabsForSetting(icon, tps, maxWidth);
                    } else {
                        // If it has not been initialized, create an empty tab with only icon and tooltip
                        insertGUITabsForSetting(icon, tps, new PreferencePanel(tps), getTabCount(), maxWidth);
                    }
                }
            } else if (!(setting instanceof SubPreferenceSetting)) {
                Logging.warn("Ignoring preferences "+setting);
            }
        }
        // Hide empty TabPreferenceSetting (only present for plugins)
        for (DefaultTabPreferenceSetting tps : Utils.filteredCollection(settings, DefaultTabPreferenceSetting.class)) {
            if (!tps.canBeHidden() || Utils.filteredCollection(settings, SubPreferenceSetting.class).stream()
                    .anyMatch(s -> s.getTabPreferenceSetting(this) == tps)) {
                continue;
            }
            indexOfTab(tps::equals).ifPresent(index -> {
                remove(index);
                Logging.debug("{0}: hiding empty {1}", getClass().getSimpleName(), tps);
            });
        }
        setSelectedIndex(-1);
    }

    private int computeMaxTabWidth() {
        FontMetrics fm = getFontMetrics(getFont());
        return settings.stream().filter(TabPreferenceSetting.class::isInstance)
                .map(TabPreferenceSetting.class::cast).map(TabPreferenceSetting::getTitle)
                .filter(Objects::nonNull).mapToInt(fm::stringWidth).max().orElse(120);
    }

    @Override
    public void expertChanged(boolean isExpert) {
        Component sel = getSelectedComponent();
        addGUITabs(true);
        int index = -1;
        if (sel != null) {
            index = indexOfComponent(sel);
        }
        setSelectedIndex(Math.max(index, 0));
    }

    /**
     * Returns a list of all preferences settings
     * @return a list of all preferences settings
     */
    public List<PreferenceSetting> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    /**
     * Returns the preferences setting for the given class
     * @param clazz the preference setting class
     * @param <T> the preference setting type
     * @return the preferences setting for the given class
     * @throws NoSuchElementException if there is no such value
     */
    public <T extends PreferenceSetting> T getSetting(Class<? extends T> clazz) {
        return Utils.filteredCollection(settings, clazz).iterator().next();
    }

    static {
        // order is important!
        SETTINGS_FACTORIES.add(new DisplayPreference.Factory());
        SETTINGS_FACTORIES.add(new DrawingPreference.Factory());
        SETTINGS_FACTORIES.add(new GPXPreference.Factory());
        SETTINGS_FACTORIES.add(new ColorPreference.Factory());
        SETTINGS_FACTORIES.add(new LafPreference.Factory());
        SETTINGS_FACTORIES.add(new LanguagePreference.Factory());

        SETTINGS_FACTORIES.add(new ServerAccessPreference.Factory());
        SETTINGS_FACTORIES.add(new ProxyPreference.Factory());
        SETTINGS_FACTORIES.add(new ProjectionPreference.Factory());
        SETTINGS_FACTORIES.add(new MapPaintPreference.Factory());
        SETTINGS_FACTORIES.add(new TaggingPresetPreference.Factory());
        SETTINGS_FACTORIES.add(new BackupPreference.Factory());
        SETTINGS_FACTORIES.add(new PluginPreference.Factory());
        SETTINGS_FACTORIES.add(MainApplication.getToolbar());
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
    static final class WheelListener implements MouseWheelListener {

        final JTabbedPane tabbedPane;

        WheelListener(JTabbedPane tabbedPane) {
            this.tabbedPane = tabbedPane;
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent wev) {
            // Ensure the cursor is over the tab strip
            if (tabbedPane.indexAtLocation(wev.getPoint().x, wev.getPoint().y) < 0)
                return;

            // Get currently selected tab && ensure the new tab index is sound
            int newTab = Utils.clamp(tabbedPane.getSelectedIndex() + wev.getWheelRotation(),
                    0, tabbedPane.getTabCount() - 1);

            tabbedPane.setSelectedIndex(newTab);
        }
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
                    if (index <= insertGUITabsForSetting(icon, preferenceSettings, index, computeMaxTabWidth())) {
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
            Container ancestor = getTopLevelAncestor();
            if (ancestor instanceof PreferenceDialog) {
                ((PreferenceDialog) ancestor).setHelpContext(preferenceSettings.getHelpContext());
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
