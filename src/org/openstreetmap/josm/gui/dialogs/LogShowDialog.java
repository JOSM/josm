// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.widgets.JosmEditorPane;
import org.openstreetmap.josm.tools.GBC;

/**
 * Generic dialog with message and scrolling area
 * @author Alexei
 * @since 5114
 */
public class LogShowDialog extends ExtendedDialog {

    /**
     * Constructs a new {@code LogShowDialog}.
     * @param title The text that will be shown in the window titlebar
     * @param msg Single-line Label
     * @param log Multi-line log
     */
    public LogShowDialog(String title, String msg, String log) {
        super(MainApplication.getMainFrame(), title, tr("OK"));
        setButtonIcons("ok");
        setContent(build(msg, log));
    }

    protected final JPanel build(String msg, String log) {
        JPanel p = new JPanel(new GridBagLayout());
        JLabel lbl = new JLabel(msg);

        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 14));

        p.add(lbl, GBC.eol().insets(5, 0, 5, 0));
        JosmEditorPane txt = new JosmEditorPane();
        txt.setContentType("text/html");
        txt.setText(log);
        txt.setEditable(false);
        txt.setOpaque(false);

        lbl.setLabelFor(txt);

        JScrollPane sp = new JScrollPane(txt);
        sp.setOpaque(false);
        sp.setPreferredSize(new Dimension(600, 300));

        p.add(sp, GBC.eop().insets(5, 15, 0, 0).fill(GBC.HORIZONTAL));

        return p;
    }
}
