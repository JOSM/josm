// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.StringTokenizer;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.tagging.TaggingPreset;
import org.openstreetmap.josm.tools.GBC;

public class TaggingPresetPreference implements PreferenceSetting {

	public static Collection<TaggingPreset> taggingPresets;
	private JList taggingPresetSources;

	public void addGui(final PreferenceDialog gui) {
		taggingPresetSources = new JList(new DefaultListModel());
		String annos = Main.pref.get("taggingpreset.sources");
		StringTokenizer st = new StringTokenizer(annos, ";");
		while (st.hasMoreTokens())
			((DefaultListModel)taggingPresetSources.getModel()).addElement(st.nextToken());

		JButton addAnno = new JButton(tr("Add"));
		addAnno.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				String source = JOptionPane.showInputDialog(Main.parent, tr("Tagging preset source"));
				if (source == null)
					return;
				((DefaultListModel)taggingPresetSources.getModel()).addElement(source);
				gui.requiresRestart = true;
			}
		});

		JButton editAnno = new JButton(tr("Edit"));
		editAnno.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (taggingPresetSources.getSelectedIndex() == -1)
					JOptionPane.showMessageDialog(Main.parent, tr("Please select the row to edit."));
				else {
					String source = JOptionPane.showInputDialog(Main.parent, tr("Tagging preset source"), taggingPresetSources.getSelectedValue());
					if (source == null)
						return;
					((DefaultListModel)taggingPresetSources.getModel()).setElementAt(source, taggingPresetSources.getSelectedIndex());
					gui.requiresRestart = true;
				}
			}
		});

		JButton deleteAnno = new JButton(tr("Delete"));
		deleteAnno.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (taggingPresetSources.getSelectedIndex() == -1)
					JOptionPane.showMessageDialog(Main.parent, tr("Please select the row to delete."));
				else {
					((DefaultListModel)taggingPresetSources.getModel()).remove(taggingPresetSources.getSelectedIndex());
					gui.requiresRestart = true;
				}
			}
		});
		taggingPresetSources.setVisibleRowCount(3);

		taggingPresetSources.setToolTipText(tr("The sources (url or filename) of tagging preset definition files. See http://josm.openstreetmap.de/wiki/TaggingPresets for help."));
		addAnno.setToolTipText(tr("Add a new tagging preset source to the list."));
		deleteAnno.setToolTipText(tr("Delete the selected source from the list."));

		gui.map.add(new JLabel(tr("Tagging preset sources")), GBC.eol().insets(0,5,0,0));
		gui.map.add(new JScrollPane(taggingPresetSources), GBC.eol().fill(GBC.BOTH));
		JPanel buttonPanel = new JPanel(new GridBagLayout());
		gui.map.add(buttonPanel, GBC.eol().fill(GBC.HORIZONTAL));
		buttonPanel.add(Box.createHorizontalGlue(), GBC.std().fill(GBC.HORIZONTAL));
		buttonPanel.add(addAnno, GBC.std().insets(0,5,0,0));
		buttonPanel.add(editAnno, GBC.std().insets(5,5,5,0));
		buttonPanel.add(deleteAnno, GBC.std().insets(0,5,0,0));
	}

	public void ok() {
		if (taggingPresetSources.getModel().getSize() > 0) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < taggingPresetSources.getModel().getSize(); ++i)
				sb.append(";"+taggingPresetSources.getModel().getElementAt(i));
			Main.pref.put("taggingpreset.sources", sb.toString().substring(1));
		} else
			Main.pref.put("taggingpreset.sources", null);
	}

	/** 
	 * Initialize the tagging presets (load and may display error)
	 */
	public static void initialize() {
		taggingPresets = TaggingPreset.readFromPreferences();
	}
}
