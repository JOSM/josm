// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JLabel;
import javax.swing.JTextField;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;

public class CsvPreference implements PreferenceSetting {

	/**
	 * Comma seperated import string specifier or <code>null</code> if the first
	 * data line should be interpreted as one.
	 */
	private JTextField csvImportString = new JTextField(20);

	public void addGui(PreferenceDialog gui) {
		csvImportString.setText(Main.pref.get("csv.importstring"));
		csvImportString.setToolTipText(tr("<html>Import string specification. lat/lon and time are imported.<br>" +
				"<b>lat</b>: The latitude coordinate<br>" +
				"<b>lon</b>: The longitude coordinate<br>" +
				"<b>time</b>: The measured time as string<br>" +
				"<b>ignore</b>: Skip this field<br>" +
				"An example: \"ignore ignore lat lon\" will use ' ' as delimiter, skip the first two values and read then lat/lon.<br>" +
		"Other example: \"lat,lon\" will just read lat/lon values comma seperated.</html>"));

		gui.connection.add(new JLabel(tr("CSV import specification (empty: read from first line in data)")), GBC.eol());
		gui.connection.add(csvImportString, GBC.eop().fill(GBC.HORIZONTAL));
    }

	public void ok() {
		Main.pref.put("csv.importstring", csvImportString.getText());
    }

}
