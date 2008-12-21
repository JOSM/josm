// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.io.GpxReader;
import org.openstreetmap.josm.io.NmeaReader;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.io.OsmServerLocationReader;
import org.xml.sax.SAXException;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Open an URL input dialog and load data from the given URL.
 *
 * @author imi
 */
public class OpenLocationAction extends JosmAction {

	/**
	 * Create an open action. The name is "Open a file".
	 */
	public OpenLocationAction() {
		super(tr("Open Location..."), "openlocation", tr("Open a URL."),
		Shortcut.registerShortcut("system:open_location", tr("File: {0}", tr("Open Location...")), KeyEvent.VK_L, Shortcut.GROUP_MENU), true);
	}

	public void actionPerformed(ActionEvent e) {

	    JCheckBox layer = new JCheckBox(tr("Separate Layer"));
	    layer.setSelected(Main.pref.getBoolean("download.newlayer"));
	    JPanel all = new JPanel(new GridBagLayout());
	    all.add(new JLabel("Enter URL to download:"), GBC.eol());
	    JTextField urltext = new JTextField(40);
	    all.add(urltext, GBC.eol());
	    all.add(layer, GBC.eol());
	    int answer = JOptionPane.showConfirmDialog(Main.parent, all, tr("Download Location"), JOptionPane.OK_CANCEL_OPTION);
	    if (answer != JOptionPane.OK_OPTION)
	        return;
	    openUrl(layer.isSelected(), urltext.getText());
	}

	/**
	 * Open the given file.
	 */
	public void openUrl(boolean new_layer, String url) {
	    new DownloadOsmTask().loadUrl(new_layer, url);
	}

}
