// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane.ValidationListener;
import org.openstreetmap.josm.gui.tagging.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.TaggingPresetMenu;
import org.openstreetmap.josm.gui.tagging.TaggingPresetSeparator;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionManager;
import org.openstreetmap.josm.tools.GBC;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class TaggingPresetPreference implements PreferenceSetting {

    public static class Factory implements PreferenceSettingFactory {
        public PreferenceSetting createPreferenceSetting() {
            return new TaggingPresetPreference();
        }
    }

    public static Collection<TaggingPreset> taggingPresets;
    private StyleSourceEditor sources;
    private JCheckBox sortMenu;
    private JCheckBox enableDefault;


    private ValidationListener validationListener = new ValidationListener() {
        @Override
        public boolean validatePreferences() {
            if (sources.hasActiveStylesChanged()) {
                List<String> sourcesToRemove = new ArrayList<String>();
                SOURCES:
                    for (String source: sources.getActiveStyles()) {
                        boolean canLoad = false;
                        try {
                            TaggingPreset.readAll(source, false);
                            canLoad = true;
                        } catch (IOException e) {
                            ExtendedDialog ed = new ExtendedDialog(Main.parent, tr("Error"),
                                    new String[] {tr("Yes"), tr("No"), tr("Cancel")});
                            ed.setContent(tr("Could not read tagging preset source: {0}\nDo you want to keep it?", source));
                            switch (ed.showDialog().getValue()) {
                            case 1:
                                continue SOURCES;
                            case 2:
                                sourcesToRemove.add(source);
                                continue SOURCES;
                            default:
                                return false;
                            }
                        } catch (SAXException e) {
                            // We will handle this in step with validation
                        }

                        String errorMessage = null;

                        try {
                            TaggingPreset.readAll(source, true);
                        } catch (IOException e) {
                            // Should not happen, but at least show message
                            JOptionPane.showMessageDialog(Main.parent, tr("Could not read tagging preset source {0}", source));
                            return false;
                        } catch (SAXParseException e) {
                            if (canLoad) {
                                errorMessage = tr("<html>Tagging preset source {0} can be loaded but it contains errors. " +
                                        "Do you really want to use it?<br><br><table width=600>Error is: [{1}:{2}] {3}</table></html>",
                                        source, e.getLineNumber(), e.getColumnNumber(), e.getMessage());
                            } else {
                                errorMessage = tr("<html>Unable to parse tagging preset source: {0}. " +
                                        "Do you really want to use it?<br><br><table width=400>Error is: [{1}:{2}] {3}</table></html>",
                                        source, e.getLineNumber(), e.getColumnNumber(), e.getMessage());
                            }
                        } catch (SAXException e) {
                            if (canLoad) {
                                errorMessage = tr("<html>Tagging preset source {0} can be loaded but it contains errors. " +
                                        "Do you really want to use it?<br><br><table width=600>Error is: {1}</table></html>",
                                        source,  e.getMessage());
                            } else {
                                errorMessage = tr("<html>Unable to parse tagging preset source: {0}. " +
                                        "Do you really want to use it?<br><br><table width=600>Error is: {1}</table></html>",
                                        source, e.getMessage());
                            }

                        }

                        if (errorMessage != null) {
                            int result = JOptionPane.showConfirmDialog(Main.parent, new JLabel(errorMessage), tr("Error"),
                                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE);

                            switch (result) {
                            case JOptionPane.YES_OPTION:
                                continue SOURCES;
                            case JOptionPane.NO_OPTION:
                                sourcesToRemove.add(source);
                                continue SOURCES;
                            default:
                                return false;
                            }
                        }
                    }
                for (String toRemove:sourcesToRemove) {
                    sources.removeSource(toRemove);
                }
                return true;
            }  else
                return true;
        }
    };

    public void addGui(final PreferenceTabbedPane gui) {
        sortMenu = new JCheckBox(tr("Sort presets menu"),
                Main.pref.getBoolean("taggingpreset.sortmenu", false));
        enableDefault = new JCheckBox(tr("Enable built-in defaults"),
                Main.pref.getBoolean("taggingpreset.enable-defaults", true));

        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder( 0, 0, 0, 0 ));
        panel.add(sortMenu, GBC.eol().insets(5,5,5,0));
        panel.add(enableDefault, GBC.eol().insets(5,0,5,0));
        sources = new StyleSourceEditor("taggingpreset.sources", "taggingpreset.icon.sources",
        "http://josm.openstreetmap.de/presets");
        panel.add(sources, GBC.eol().fill(GBC.BOTH));
        gui.mapcontent.addTab(tr("Tagging Presets"), panel);

        // this defers loading of tagging preset sources to the first time the tab
        // with the tagging presets is selected by the user
        //
        gui.mapcontent.addChangeListener(
                new ChangeListener() {
                    public void stateChanged(ChangeEvent e) {
                        if (gui.mapcontent.getSelectedComponent() == panel) {
                            sources.initiallyLoadAvailableStyles();
                        }
                    }
                }
        );
        gui.addValidationListener(validationListener);
    }

    public boolean ok() {
        boolean restart = Main.pref.put("taggingpreset.enable-defaults",
                enableDefault.getSelectedObjects() != null);
        if(Main.pref.put("taggingpreset.sortmenu", sortMenu.getSelectedObjects() != null)) {
            restart = true;
        }
        if(sources.finish()) {
            restart = true;
        }
        return restart;
    }

    /**
     * Initialize the tagging presets (load and may display error)
     */
    public static void initialize() {
        taggingPresets = TaggingPreset.readFromPreferences(false);
        for (TaggingPreset tp: taggingPresets) {
            if (!(tp instanceof TaggingPresetSeparator)) {
                Main.toolbar.register(tp);
            }
        }
        if (taggingPresets.isEmpty()) {
            Main.main.menu.presetsMenu.setVisible(false);
        }
        else
        {
            AutoCompletionManager.cachePresets(taggingPresets);
            HashMap<TaggingPresetMenu,JMenu> submenus = new HashMap<TaggingPresetMenu,JMenu>();
            for (final TaggingPreset p : taggingPresets)
            {
                JMenu m = p.group != null ? submenus.get(p.group) : Main.main.menu.presetsMenu;
                if (p instanceof TaggingPresetSeparator) {
                    m.add(new JSeparator());
                } else if (p instanceof TaggingPresetMenu)
                {
                    JMenu submenu = new JMenu(p);
                    submenu.setText(p.getLocaleName());
                    ((TaggingPresetMenu)p).menu = submenu;
                    submenus.put((TaggingPresetMenu)p, submenu);
                    m.add(submenu);
                }
                else
                {
                    JMenuItem mi = new JMenuItem(p);
                    mi.setText(p.getLocaleName());
                    m.add(mi);
                }
            }
        }
        if(Main.pref.getBoolean("taggingpreset.sortmenu")) {
            TaggingPresetMenu.sortMenu(Main.main.menu.presetsMenu);
        }
    }
}
