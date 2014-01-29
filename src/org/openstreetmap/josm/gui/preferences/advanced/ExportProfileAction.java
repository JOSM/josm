// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.preferences.advanced;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.DiskAccessAction;
import org.openstreetmap.josm.data.CustomConfigurator;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.Preferences.Setting;

/**
 * Action that exports some fragment of settings to custom configuration file
 */
public class ExportProfileAction extends AbstractAction {
    private final String prefPattern;
    private final String schemaKey;
    private final Preferences prefs;

    /**
     * Constructs a new {@code ExportProfileAction}.
     */
    public ExportProfileAction(Preferences prefs, String schemaKey, String prefPattern) {
        super(tr("Save {0} profile", tr(schemaKey)));
        this.prefs = prefs;
        this.prefPattern = prefPattern;
        this.schemaKey = schemaKey;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        List<String> keys = new ArrayList<String>();
        Map<String, Setting> all = prefs.getAllSettings();
        for (String key: all.keySet()) {
            if (key.matches(prefPattern)) {
                keys.add(key);
            }
        }
        if (keys.isEmpty()) {
            JOptionPane.showMessageDialog(Main.parent,
                    tr("All the preferences of this group are default, nothing to save"), tr("Warning"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        File f = askUserForCustomSettingsFile();
        if (f!=null)
           CustomConfigurator.exportPreferencesKeysToFile(f.getAbsolutePath(), false, keys);
    }

    private File askUserForCustomSettingsFile() {
        String title = tr("Choose profile file");

        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".xml") && f.getName().toLowerCase().startsWith(schemaKey);
            }
            @Override
            public String getDescription() {
                return tr("JOSM custom settings files (*.xml)");
            }
        };
        JFileChooser fc = DiskAccessAction.createAndOpenFileChooser(false, false, title, filter, JFileChooser.FILES_ONLY, "customsettings.lastDirectory");
        if (fc != null) {
            File sel = fc.getSelectedFile();
            if (!sel.getName().endsWith(".xml")) sel=new File(sel.getAbsolutePath()+".xml");
            if (!sel.getName().startsWith(schemaKey)) {
                sel = new File(sel.getParentFile().getAbsolutePath()+"/"+schemaKey+"_"+sel.getName());
            }
            return sel;
        }
        return null;
    }
}
