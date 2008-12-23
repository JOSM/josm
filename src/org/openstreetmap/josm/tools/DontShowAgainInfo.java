// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Container;
import java.awt.GridBagLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;

public class DontShowAgainInfo {

    public static boolean show(String prefKey, String msg) {
        return show(prefKey, new JLabel(msg), true, JOptionPane.OK_CANCEL_OPTION, JOptionPane.OK_OPTION);
    }

    public static boolean show(String prefKey, String msg, Boolean state) {
        return show(prefKey, new JLabel(msg), state, JOptionPane.OK_CANCEL_OPTION, JOptionPane.OK_OPTION);
    }

    public static boolean show(String prefKey, Container msg) {
        return show(prefKey, msg, true, JOptionPane.OK_CANCEL_OPTION, JOptionPane.OK_OPTION);
    }

    public static boolean show(String prefKey, Container msg, Boolean state, int options, int true_option) {
        if (!Main.pref.getBoolean("message."+prefKey)) {
            JCheckBox dontshowagain = new JCheckBox(tr("Do not show again"));
            dontshowagain.setSelected(Main.pref.getBoolean("message."+prefKey, state));
            JPanel all = new JPanel(new GridBagLayout());
            all.add(msg, GBC.eop());
            all.add(dontshowagain, GBC.eol());
            int answer = JOptionPane.showConfirmDialog(Main.parent, all, tr("Information"), options);
            if (answer != true_option)
                return false;
            Main.pref.put("message."+prefKey, dontshowagain.isSelected());
        }
        return true;
    }
}
