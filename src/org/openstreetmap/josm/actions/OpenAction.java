// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.zip.GZIPInputStream;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.RawGpsLayer;
import org.openstreetmap.josm.gui.layer.RawGpsLayer.GpsPoint;
import org.openstreetmap.josm.gui.layer.markerlayer.Marker;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.io.RawCsvReader;
import org.openstreetmap.josm.io.RawGpsReader;
import org.xml.sax.SAXException;

/**
 * Open a file chooser dialog and select an file to import. Than call the gpx-import
 * driver. Finally open an internal frame into the main window with the gpx data shown.
 * 
 * @author imi
 */
public class OpenAction extends DiskAccessAction {
	
	/**
	 * Create an open action. The name is "Open a file".
	 */
	public OpenAction() {
		super(tr("Open"), "open", tr("Open a file."), KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK);
	}

	public void actionPerformed(ActionEvent e) {
		JFileChooser fc = createAndOpenFileChooser(true, true);
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
			if (asRawData(file.getName()))
				openFileAsRawGps(file);
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
	    } else if (ExtensionFileFilter.filters[ExtensionFileFilter.CSV].acceptName(fn))
	    	JOptionPane.showMessageDialog(Main.parent, fn+": "+tr("CSV Data import for non-GPS data is not implemented yet."));
	    else
	    	JOptionPane.showMessageDialog(Main.parent, fn+": "+tr("Unknown file extension: {0}", fn.substring(file.getName().lastIndexOf('.')+1)));
    }

	private void openFileAsRawGps(File file) throws SAXException, IOException, FileNotFoundException {
	    String fn = file.getName();
	    Collection<Collection<GpsPoint>> gpsData = null;
	    Collection<Marker> markerData = null;
	    if (ExtensionFileFilter.filters[ExtensionFileFilter.GPX].acceptName(fn)) {
	    	RawGpsReader r = null;
	    	if (file.getName().endsWith(".gpx.gz"))
	    		r = new RawGpsReader(new GZIPInputStream(new FileInputStream(file)), file.getAbsoluteFile().getParentFile());
	    	else
	    		r = new RawGpsReader(new FileInputStream(file), file.getAbsoluteFile().getParentFile());
	    	gpsData = r.trackData;
	    	markerData = r.markerData;
	    } else if (ExtensionFileFilter.filters[ExtensionFileFilter.CSV].acceptName(fn)) {
	    	gpsData = new LinkedList<Collection<GpsPoint>>();
	    	gpsData.add(new RawCsvReader(new FileReader(file)).parse());
	    } else
	    	throw new IllegalStateException();
	    if (gpsData != null && !gpsData.isEmpty())
	    	Main.main.addLayer(new RawGpsLayer(false, gpsData, tr("Tracks from {0}", file.getName()), file));
	    if (markerData != null && !markerData.isEmpty())
	    	Main.main.addLayer(new MarkerLayer(markerData, tr ("Markers from {0}", file.getName()), file));
    }

	/**
	 * @return Return whether the file should be opened as raw gps data. May ask the
	 * user, if unsure.
	 */
	private boolean asRawData(String fn) {
		if (ExtensionFileFilter.filters[ExtensionFileFilter.CSV].acceptName(fn))
			return true;
		if (!ExtensionFileFilter.filters[ExtensionFileFilter.GPX].acceptName(fn))
			return false;
		return true;
	}
}
