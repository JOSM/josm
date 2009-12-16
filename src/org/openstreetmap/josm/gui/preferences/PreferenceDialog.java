// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.ScrollPane;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.tools.BugReportExceptionHandler;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * The preference settings.
 *
 * @author imi
 */
public class PreferenceDialog extends JTabbedPane implements MouseWheelListener {
    static private final Logger logger = Logger.getLogger(PreferenceDialog.class.getName());

    private final static Collection<PreferenceSettingFactory> settingsFactory = new LinkedList<PreferenceSettingFactory>();
    private final List<PreferenceSetting> settings = new ArrayList<PreferenceSetting>();

    // some common tabs
    public final JPanel display = createPreferenceTab("display", tr("Display Settings"), tr("Various settings that influence the visual representation of the whole program."));
    public final JPanel connection = createPreferenceTab("connection", I18n.tr("Connection Settings"), I18n.tr("Connection Settings for the OSM server."),false);
    public final JPanel map = createPreferenceTab("map", I18n.tr("Map Settings"), I18n.tr("Settings for the map projection and data interpretation."));
    public final JPanel audio = createPreferenceTab("audio", I18n.tr("Audio Settings"), I18n.tr("Settings for the audio player and audio markers."));

    public final javax.swing.JTabbedPane displaycontent = new javax.swing.JTabbedPane();
    public final javax.swing.JTabbedPane mapcontent = new javax.swing.JTabbedPane();

    /**
     * Construct a JPanel for the preference settings. Layout is GridBagLayout
     * and a centered title label and the description are added. The panel
     * will be shown inside a {@link ScrollPane}
     * @param icon The name of the icon.
     * @param title The title of this preference tab.
     * @param desc A description in one sentence for this tab. Will be displayed
     *      italic under the title.
     * @return The created panel ready to add other controls.
     */
    public JPanel createPreferenceTab(String icon, String title, String desc) {
        return createPreferenceTab(icon, title, desc, false);
    }

    /**
     * Construct a JPanel for the preference settings. Layout is GridBagLayout
     * and a centered title label and the description are added.
     * @param icon The name of the icon.
     * @param title The title of this preference tab.
     * @param desc A description in one sentence for this tab. Will be displayed
     *      italic under the title.
     * @param inScrollPane if <code>true</code> the added tab will show scroll bars
     *        if the panel content is larger than the available space
     * @return The created panel ready to add other controls.
     */
    public JPanel createPreferenceTab(String icon, String title, String desc, boolean inScrollPane) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        p.add(new JLabel(title), GBC.eol().anchor(GBC.CENTER).insets(0,5,0,10));

        JLabel descLabel = new JLabel("<html>"+desc+"</html>");
        descLabel.setFont(descLabel.getFont().deriveFont(Font.ITALIC));
        p.add(descLabel, GBC.eol().insets(5,0,5,20).fill(GBC.HORIZONTAL));

        JComponent tab = p;
        if (inScrollPane) {
            JScrollPane sp = new JScrollPane(p);
            tab = sp;
        }
        addTab(null, ImageProvider.get("preferences", icon), tab);
        setToolTipTextAt(getTabCount()-1, "<html>"+desc+"</html>");
        return p;
    }

    public void ok() {
        boolean requiresRestart = false;
        for (PreferenceSetting setting : settings)
        {
            if(setting.ok()) {
                requiresRestart = true;
            }
        }
        if (requiresRestart) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("You have to restart JOSM for some settings to take effect."),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE
            );
        }
        Main.parent.repaint();
    }

    /**
     * If the dialog is closed with Ok, the preferences will be stored to the preferences-
     * file, otherwise no change of the file happens.
     */
    public PreferenceDialog() {
        super(JTabbedPane.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT);

        super.addMouseWheelListener(this);

        for (PreferenceSettingFactory factory:settingsFactory) {
            // logger.info("creating settings: " + factory);
            PreferenceSetting setting = factory.createPreferenceSetting();
            if (setting != null) {
                settings.add(factory.createPreferenceSetting());
            }
        }

        display.add(displaycontent, GBC.eol().fill(GBC.BOTH));
        map.add(mapcontent, GBC.eol().fill(GBC.BOTH));
        for (Iterator<PreferenceSetting> it = settings.iterator(); it.hasNext();) {
            try {
                PreferenceSetting settings = it.next();
                //logger.info("adding gui: " + settings);
                settings.addGui(this);
            } catch (SecurityException e) {
                it.remove();
            } catch (Throwable e) {
                /* allow to change most settings even if e.g. a plugin fails */
                BugReportExceptionHandler.handleException(e);
            }
        }
    }

    public List<PreferenceSetting> getSettings() {
        return settings;
    }

    @SuppressWarnings("unchecked")
    public <T>  T getSetting(Class<? extends T> clazz) {
        for (PreferenceSetting setting:settings) {
            if (clazz.isAssignableFrom(setting.getClass()))
                return (T)setting;
        }
        return null;
    }

    static {
        // order is important!
        settingsFactory.add(new DrawingPreference.Factory());
        settingsFactory.add(new ColorPreference.Factory());
        settingsFactory.add(new LafPreference.Factory());
        settingsFactory.add(new LanguagePreference.Factory());
        settingsFactory.add(new ServerAccessPreference.Factory());
        settingsFactory.add(new FilePreferences.Factory());
        settingsFactory.add(new ProxyPreferences.Factory());
        settingsFactory.add(new ProjectionPreference.Factory());
        settingsFactory.add(new MapPaintPreference.Factory());
        settingsFactory.add(new TaggingPresetPreference.Factory());
        settingsFactory.add(new PluginPreference.Factory());
        settingsFactory.add(Main.toolbar);
        settingsFactory.add(new AudioPreference.Factory());
        settingsFactory.add(new ShortcutPreference.Factory());

        PluginHandler.getPreferenceSetting(settingsFactory);

        // always the last: advanced tab
        settingsFactory.add(new AdvancedPreference.Factory());
    }

    /**
     * This mouse wheel listener reacts when a scroll is carried out over the
     * tab strip and scrolls one tab/down or up, selecting it immediately.
     */
    public void mouseWheelMoved(MouseWheelEvent wev) {
        // Ensure the cursor is over the tab strip
        if(super.indexAtLocation(wev.getPoint().x, wev.getPoint().y) < 0)
            return;

        // Get currently selected tab
        int newTab = super.getSelectedIndex() + wev.getWheelRotation();

        // Ensure the new tab index is sound
        newTab = newTab < 0 ? 0 : newTab;
        newTab = newTab >= super.getTabCount() ? super.getTabCount() - 1 : newTab;

        // select new tab
        super.setSelectedIndex(newTab);
    }
}
