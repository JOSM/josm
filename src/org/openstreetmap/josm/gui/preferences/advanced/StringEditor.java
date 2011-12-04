// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.preferences.advanced;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openstreetmap.josm.data.Preferences.StringSetting;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.advanced.AdvancedPreference.PrefEntry;
import org.openstreetmap.josm.tools.GBC;

public class StringEditor extends ExtendedDialog {

    PrefEntry entry;
    JTextField tvalue;

    public StringEditor(final PreferenceTabbedPane gui, PrefEntry entry, StringSetting setting) {
        super(gui, tr("Change string setting"), new String[] {tr("OK"), tr("Cancel")});
        this.entry = entry;
        setButtonIcons(new String[] {"ok.png", "cancel.png"});
        setContent(build(setting.getValue() == null ? "" : setting.getValue()));
    }

    public String getData() {
        return tvalue.getText();
    }

    protected JPanel build(String orig) {
        JPanel p = new JPanel(new GridBagLayout());
        p.add(new JLabel(tr("Key: {0}", entry.getKey())), GBC.eol().insets(0,0,5,0));

        p.add(new JLabel(tr("Value: ")), GBC.std());
        tvalue = new JTextField(orig, 50);
        p.add(tvalue, GBC.eop().insets(5,0,0,0).fill(GBC.HORIZONTAL));

        return p;
    }
}
