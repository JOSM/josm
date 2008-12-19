// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.io.GpxReader;
import org.openstreetmap.josm.io.NmeaReader;
import org.openstreetmap.josm.io.OsmReader;
import org.xml.sax.SAXException;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Open a file chooser dialog and select an file to import. Then call the gpx-import
 * driver. Finally open an internal frame into the main window with the gpx data shown.
 *
 * @author imi
 */
public class OpenFileAction extends DiskAccessAction {

	/**
	 * Create an open action. The name is "Open a file".
	 */
	public OpenFileAction() {
		super(tr("Open ..."), "open", tr("Open a file."),
		Shortcut.registerShortcut("system:open", tr("File: {0}", tr("Open ...")), KeyEvent.VK_O, Shortcut.GROUP_MENU));
	}

	public void actionPerformed(ActionEvent e) {
		JFileChooser fc = createAndOpenFileChooser(true, true, null);
		if (fc == null)
			return;
		File[] files = fc.getSelectedFiles();
		for (int i = files.length; i > 0; --i)
			openFile(files[i-1]);
	}

	/**
	 * Open the given file.
	 */
	public void openFile(File file) {
		try {
			if (asGpxData(file.getName()))
				openFileAsGpx(file);
			else if (asNmeaData(file.getName()))
				openFileAsNmea(file);
			else
				openAsData(file);
		} catch (SAXException x) {
			x.printStackTrace();
			JOptionPane.showMessageDialog(Main.parent, tr("Error while parsing {0}",file.getName())+": "+x.getMessage());
		} catch (IOException x) {
			x.printStackTrace();
			JOptionPane.showMessageDialog(Main.parent, tr("Could not read \"{0}\"",file.getName())+"\n"+x.getMessage());
		}
	}

	private void openAsData(File file) throws SAXException, IOException, FileNotFoundException {
		String fn = file.getName();
		if (ExtensionFileFilter.filters[ExtensionFileFilter.OSM].acceptName(fn)) {
			DataSet dataSet = OsmReader.parseDataSet(new FileInputStream(file), null, Main.pleaseWaitDlg);
			OsmDataLayer layer = new OsmDataLayer(dataSet, file.getName(), file);
			Main.main.addLayer(layer);
			layer.fireDataChange();
		}
		else
			JOptionPane.showMessageDialog(Main.parent, fn+": "+tr("Unknown file extension: {0}", fn.substring(file.getName().lastIndexOf('.')+1)));
	}

	private void openFileAsGpx(File file) throws SAXException, IOException, FileNotFoundException {
		String fn = file.getName();
		if (ExtensionFileFilter.filters[ExtensionFileFilter.GPX].acceptName(fn)) {
			GpxReader r = null;
			InputStream is;
			if (file.getName().endsWith(".gpx.gz")) {
				is = new GZIPInputStream(new FileInputStream(file));
			} else {
				is = new FileInputStream(file);
			}
			// Workaround for SAX BOM bug
			// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6206835
			if(!((is.read()==0xef)&&(is.read()==0xbb)&&(is.read()==0xbf))) {
				is.close();
				if (file.getName().endsWith(".gpx.gz")) {
					is = new GZIPInputStream(new FileInputStream(file));
				} else {
					is = new FileInputStream(file);
				}
			}
			r = new GpxReader(is,file.getAbsoluteFile().getParentFile());
			r.data.storageFile = file;
			GpxLayer gpxLayer = new GpxLayer(r.data, fn);
			Main.main.addLayer(gpxLayer);
			if (Main.pref.getBoolean("marker.makeautomarkers", true)) {
				MarkerLayer ml = new MarkerLayer(r.data, tr("Markers from {0}", fn), file, gpxLayer);
				if (ml.data.size() > 0) {
					Main.main.addLayer(ml);
				}
			}

		} else {
			throw new IllegalStateException();
		}
    }

	private void openFileAsNmea(File file) throws IOException, FileNotFoundException {
		String fn = file.getName();
		if (ExtensionFileFilter.filters[ExtensionFileFilter.NMEA].acceptName(fn)) {
			NmeaReader r = new NmeaReader(new FileInputStream(file), file.getAbsoluteFile().getParentFile());
			r.data.storageFile = file;
			GpxLayer gpxLayer = new GpxLayer(r.data, fn);
			Main.main.addLayer(gpxLayer);
			if (Main.pref.getBoolean("marker.makeautomarkers", true)) {
				MarkerLayer ml = new MarkerLayer(r.data, tr("Markers from {0}", fn), file, gpxLayer);
				if (ml.data.size() > 0) {
					Main.main.addLayer(ml);
				}
			}

		} else {
			throw new IllegalStateException();
		}
    }

	private boolean asGpxData(String fn) {
		return ExtensionFileFilter.filters[ExtensionFileFilter.GPX].acceptName(fn);
	}

	private boolean asNmeaData(String fn) {
		return ExtensionFileFilter.filters[ExtensionFileFilter.NMEA].acceptName(fn);
	}


}
