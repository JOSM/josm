// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JCheckBox;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;

/**
 * Out of pure laziness, I add the file stuff to connection tab.
 * Feel free to fix this.
 *
 * @author imi
 */
public class FilePreferences implements PreferenceSetting {

    private JCheckBox keepBackup = new JCheckBox(tr("Keep backup files"));

    public void addGui(PreferenceDialog gui) {
        gui.connection.add(new JSeparator(SwingConstants.HORIZONTAL), GBC.eol().fill(GBC.HORIZONTAL));
        keepBackup.setSelected(Main.pref.getBoolean("save.keepbackup"));
        keepBackup.setToolTipText(tr("When saving, keep backup files ending with a ~"));
        gui.connection.add(keepBackup, GBC.eol().insets(20,0,0,0));
    }

    public boolean ok() {
        Main.pref.put("save.keepbackup", keepBackup.isSelected());
        return false;
    }
}
