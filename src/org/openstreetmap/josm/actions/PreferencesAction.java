// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.preferences.PreferenceDialog;
import org.openstreetmap.josm.tools.GBC;

/**
 * Open the Preferences dialog.
 *
 * @author imi
 */
public class PreferencesAction extends JosmAction {

	/**
	 * Create the preference action with "&Preferences" as label.
	 */
	public PreferencesAction() {
		super(tr("Preferences"), "preference", tr("Open a preferences page for global settings."), KeyEvent.VK_F12, 0, true);
	}

	/**
	 * Launch the preferences dialog.
	 */
	public void actionPerformed(ActionEvent e) {
		PreferenceDialog prefDlg = new PreferenceDialog();
		JPanel prefPanel = new JPanel(new GridBagLayout());
		prefPanel.add(prefDlg, GBC.eol().fill(GBC.BOTH));

		JOptionPane pane = new JOptionPane(prefPanel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		JDialog dlg = pane.createDialog(Main.parent, tr("Preferences"));

		if (dlg.getWidth() > 600)
			dlg.setSize(600, dlg.getHeight());
		if (dlg.getHeight() > 600)
			dlg.setSize(dlg.getWidth(),600);

		dlg.setVisible(true);
		if (pane.getValue() instanceof Integer && (Integer)pane.getValue() == JOptionPane.OK_OPTION)
			prefDlg.ok();
	}
}
