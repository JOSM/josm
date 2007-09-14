// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.MergeVisitor;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.IncompleteDownloader;
import org.xml.sax.SAXException;

/**
 * Action that opens a connection to the osm server and download map data.
 *
 * An dialog is displayed asking the user to specify a rectangle to grab.
 * The url and account settings from the preferences are used.
 *
 * @author imi
 */
public class DownloadIncompleteAction extends JosmAction {

	/**
	 * Open the download dialog and download the data.
	 * Run in the worker thread.
	 */
	private final class DownloadTask extends PleaseWaitRunnable {
		private IncompleteDownloader reader;

		private DownloadTask(Collection<Way> toDownload) {
			super(trn("Downloading {0} way", "Downloading {0} ways", toDownload.size(), toDownload.size()));
			reader = new IncompleteDownloader(toDownload);
		}

		@Override public void realRun() throws IOException, SAXException {
			reader.parse();
		}

		@Override protected void finish() {
			MergeVisitor merger = new MergeVisitor(Main.ds, reader.data);
			for (OsmPrimitive osm : reader.data.allPrimitives())
				osm.visit(merger);
			Main.parent.repaint();
		}

		@Override protected void cancel() {
			reader.cancel();
		}
	}

	public DownloadIncompleteAction() {
		super(tr("Download incomplete objects"), "downloadincomplete", tr("Download all (selected) incomplete ways from the OSM server."), KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK, true);
	}

	public void actionPerformed(ActionEvent e) {
		Collection<Way> ways = new HashSet<Way>();
		for (Way w : Main.ds.ways)
			if (w.isIncomplete() && w.selected)
				ways.add(w);
		if (ways.isEmpty()) {
			JOptionPane.showMessageDialog(Main.parent, tr("Please select an incomplete way."));
			return;
		}
		if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(Main.parent, tr("Download {0} incomplete ways?", ways.size()), tr("Download?"), JOptionPane.YES_NO_OPTION))
			return;
		PleaseWaitRunnable task = new DownloadTask(ways);
		Main.worker.execute(task);
	}
}
