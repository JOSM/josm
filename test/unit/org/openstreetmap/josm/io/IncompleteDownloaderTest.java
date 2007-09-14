// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.LinkedList;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.PleaseWaitDialog;
import org.openstreetmap.josm.testframework.Bug;
import org.openstreetmap.josm.testframework.MotherObject;

public class IncompleteDownloaderTest extends MotherObject {


	@Bug(174)
	public void testDownloadDoesNotWriteToMainDataDirectly() throws Exception {
		LinkedList<Way> l = new LinkedList<Way>();
		Way w = new Way();
		w.segments.add(new Segment(23)); // incomplete segment
		Main.ds.nodes.add(createNode(1));
		Main.ds.nodes.add(createNode(2));
		l.add(w);
		IncompleteDownloader downloader = new IncompleteDownloader(l) {
			@Override protected InputStream getInputStream(String urlStr, PleaseWaitDialog pleaseWaitDlg) {
				String xml = "<osm version='0.4'><segment id='23' from='1' to='2'/></osm>";
	            return new ByteArrayInputStream(xml.getBytes());
            }
		};

		Main.pleaseWaitDlg = new PleaseWaitDialog(null);

		downloader.parse();

		assertEquals("Does not directly write to main data", 0, Main.ds.segments.size());
	}
}
