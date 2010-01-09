// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;

public class BackupPreferencesPanel extends VerticallyScrollablePanel {

    private JCheckBox keepBackup;

    protected void build() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;
        keepBackup = new JCheckBox(tr("Keep backup files"));
        keepBackup.setSelected(Main.pref.getBoolean("save.keepbackup"));
        keepBackup.setToolTipText(tr("When saving, keep backup files ending with a ~"));
        add(keepBackup, gc);

        // filler - grab remaining space
        gc.gridy = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        add(new JPanel(), gc);
    }

    public void saveToPreferences() {
        Main.pref.put("save.keepbackup", keepBackup.isSelected());
    }

    public void initFromPreferences() {
        keepBackup.setSelected(Main.pref.getBoolean("save.keepbackup", true));
    }

    public BackupPreferencesPanel() {
        build();
        HelpUtil.setHelpContext(this, HelpUtil.ht("/Preferences/Connection#BackupSettings"));
    }
}
