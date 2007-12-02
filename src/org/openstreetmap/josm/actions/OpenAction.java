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
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.markerlayer.Marker;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.io.GpxReader;
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
		super(tr("Open ..."), "open", tr("Open a file."), KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK);
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
			if (asGpxData(file.getName()))
				openFileAsGpx(file);
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

	private void openFileAsGpx(File file) throws SAXException, IOException, FileNotFoundException {
		String fn = file.getName();
		if (ExtensionFileFilter.filters[ExtensionFileFilter.GPX].acceptName(fn)) {
			GpxReader r = null;
			if (file.getName().endsWith(".gpx.gz")) {
				r = new GpxReader(new GZIPInputStream(new FileInputStream(file)), file.getAbsoluteFile().getParentFile());
			} else{
				r = new GpxReader(new FileInputStream(file), file.getAbsoluteFile().getParentFile());
			}
			r.data.storageFile = file;
			Main.main.addLayer(new GpxLayer(r.data, fn));
            MarkerLayer ml = new MarkerLayer(r.data, tr("Markers from {0}", fn), file);
            if (ml.data.size() > 0) {
            	Main.main.addLayer(ml);
            }

		} else {
			throw new IllegalStateException();
		}
    }


	private boolean asGpxData(String fn) {
		return ExtensionFileFilter.filters[ExtensionFileFilter.GPX].acceptName(fn);
	}


}
