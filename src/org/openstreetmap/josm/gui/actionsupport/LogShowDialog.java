package org.openstreetmap.josm.gui.actionsupport;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import javax.swing.*;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.advanced.AdvancedPreference;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.tools.GBC;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * Generic dialog with message and scrolling area
 * @author Alexei
 */
public class LogShowDialog extends ExtendedDialog {


    public LogShowDialog (String title, String msg, String log) {
        super(Main.parent, title, new String[] {tr("OK")});
        setButtonIcons(new String[] {"ok.png"});
        setContent(build(msg, log));
    }

    protected JPanel build(String msg, String log) {
        JPanel p = new JPanel(new GridBagLayout());
        JLabel lbl = new JLabel(msg);
        
        lbl.setFont(lbl.getFont().deriveFont(0, 14));
        
        p.add(lbl, GBC.eol().insets(5,0,5,0));
        JEditorPane txt = new JEditorPane();
        txt.setContentType("text/html");
        txt.setText(log);
        txt.setEditable(false);
        txt.setOpaque(false);
        
        JScrollPane sp = new JScrollPane(txt);
        sp.setOpaque(false);
        sp.setPreferredSize(new Dimension(600,300));
        
        
        p.add(sp, GBC.eop().insets(5,15,0,0).fill(GBC.HORIZONTAL));

        return p;
    }
}
 

