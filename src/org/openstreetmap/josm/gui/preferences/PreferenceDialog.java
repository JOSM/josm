// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.ScrollPane;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.plugins.PluginProxy;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * The preference settings.
 *
 * @author imi
 */
public class PreferenceDialog extends JTabbedPane {

    public final static Collection<PreferenceSetting> settings = new LinkedList<PreferenceSetting>();

    // some common tabs
    public final JPanel display = createPreferenceTab("display", tr("Display Settings"), tr("Various settings that influence the visual representation of the whole program."));
    public final JPanel connection = createPreferenceTab("connection", I18n.tr("Connection Settings"), I18n.tr("Connection Settings for the OSM server."));
    public final JPanel map = createPreferenceTab("map", I18n.tr("Map Settings"), I18n.tr("Settings for the map projection and data interpretation."));
    public final JPanel audio = createPreferenceTab("audio", I18n.tr("Audio Settings"), I18n.tr("Settings for the audio player and audio markers."));

  public final javax.swing.JTabbedPane displaycontent = new javax.swing.JTabbedPane();

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
        return createPreferenceTab(icon, title, desc, true);
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
            if(setting.ok())
                requiresRestart = true;
        }
        if (requiresRestart)
            JOptionPane.showMessageDialog(Main.parent,tr("You have to restart JOSM for some settings to take effect."));
        Main.parent.repaint();
    }

    /**
     * If the dialog is closed with Ok, the preferences will be stored to the preferences-
     * file, otherwise no change of the file happens.
     */
    public PreferenceDialog() {
        super(JTabbedPane.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT);
        display.add(displaycontent, GBC.eol().fill(GBC.BOTH));
        for (Iterator<PreferenceSetting> it = settings.iterator(); it.hasNext();) {
            try {
                it.next().addGui(this);
            } catch (SecurityException e) {
                it.remove();
            }
        }
    }

    static {
        // order is important!
        settings.add(new DrawingPreference());
        settings.add(new ColorPreference());
        settings.add(new LafPreference());
        settings.add(new LanguagePreference());
        settings.add(new MapPaintPreference());
        settings.add(new ServerAccessPreference());
        settings.add(new FilePreferences());
        settings.add(new ProxyPreferences());
        settings.add(new ProjectionPreference());
        settings.add(new TaggingPresetPreference());
        settings.add(new PluginPreference());
        settings.add(Main.toolbar);
        settings.add(new AudioPreference());
        settings.add(new ShortcutPreference());

        for (PluginProxy plugin : Main.plugins) {
            PreferenceSetting p = plugin.getPreferenceSetting();
            if (p != null)
                settings.add(p);
        }

        // always the last: advanced tab
        settings.add(new AdvancedPreference());
    }
}
