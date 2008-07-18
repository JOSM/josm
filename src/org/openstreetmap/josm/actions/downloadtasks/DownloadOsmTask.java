// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.downloadtasks;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;

import javax.swing.JCheckBox;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.DownloadAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.download.DownloadDialog.DownloadTask;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.BoundingBoxDownloader;
import org.xml.sax.SAXException;

/**
 * Open the download dialog and download the data.
 * Run in the worker thread.
 */
public class DownloadOsmTask implements DownloadTask {

	private static class Task extends PleaseWaitRunnable {
		private BoundingBoxDownloader reader;
		private DataSet dataSet;
		private boolean newLayer;

		public Task(boolean newLayer, BoundingBoxDownloader reader) {
			super(tr("Downloading data"));
			this.reader = reader;
			this.newLayer = newLayer;
		}

		@Override public void realRun() throws IOException, SAXException {
			dataSet = reader.parseOsm();
		}

		@Override protected void finish() {
			if (dataSet == null)
				return; // user cancelled download or error occoured
			if (dataSet.allPrimitives().isEmpty())
				errorMessage = tr("No data imported.");
			OsmDataLayer layer = new OsmDataLayer(dataSet, tr("Data Layer"), null);
			if (newLayer)
				Main.main.addLayer(layer);
			else
				Main.main.editLayer().mergeFrom(layer);
		}

		@Override protected void cancel() {
			if (reader != null)
				reader.cancel();
		}
	}
	private JCheckBox checkBox = new JCheckBox(tr("OpenStreetMap data"), true);

	public void download(DownloadAction action, double minlat, double minlon, double maxlat, double maxlon) {
    // Swap min and max if user has specified them the wrong way round
    // (easy to do if you are crossing 0, for example)
    if (minlat > maxlat) {
      double t = minlat; minlat = maxlat; maxlat = t;
    }
    if (minlon > maxlon) {
      double t = minlon; minlon = maxlon; maxlon = t;
    }
    
		Task task = new Task(action.dialog == null || action.dialog.newLayer.isSelected(), new BoundingBoxDownloader(minlat, minlon, maxlat, maxlon));
		Main.worker.execute(task);
    }

	public JCheckBox getCheckBox() {
	    return checkBox;
    }

	public String getPreferencesSuffix() {
	    return "osm";
    }
}
