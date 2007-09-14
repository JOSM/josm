// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.MergeVisitor;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Capable of downloading ways without having to fully parse their segments.
 *
 * @author Imi
 */
public class IncompleteDownloader extends OsmServerReader {

	/**
	 * The new downloaded data will be inserted here.
	 */
	public final DataSet data = new DataSet();

	/**
	 * The list of incomplete Ways to download. The ways will be filled and are complete after download.
	 */
	private final Collection<Way> toDownload;
	private MergeVisitor merger = new MergeVisitor(data, null);

	public IncompleteDownloader(Collection<Way> toDownload) {
		this.toDownload = toDownload;
	}

	public void parse() throws SAXException, IOException {
		Main.pleaseWaitDlg.currentAction.setText(tr("Downloading incomplete ways..."));
		Main.pleaseWaitDlg.progress.setMaximum(toDownload.size());
		Main.pleaseWaitDlg.progress.setValue(0);
		ArrayList<Command> cmds = new ArrayList<Command>();
		int i = 0;
		try {
			for (Way w : toDownload) {
				// if some of the way's segments fail to download and the user
				// decides to delete them, the download method will return an
				// "edit way" command.
				Command cmd = download(w); 
				if (cmd != null)
					cmds.add(cmd);
				Main.pleaseWaitDlg.progress.setValue(++i);
			}
		} catch (IOException e) {
			if (!cancel)
				throw e;
		} catch (SAXException e) {
			throw e;
		} catch (Exception e) {
			if (!cancel)
				throw (e instanceof RuntimeException) ? (RuntimeException)e : new RuntimeException(e);
		}
		if (cmds.size() > 0)
			Main.main.undoRedo.add(new SequenceCommand(tr("Fix data errors"), cmds));
	}

	private static class SegmentParser extends DefaultHandler {
		public long from, to;
		@Override public void startElement(String ns, String lname, String qname, Attributes a) {
			if (qname.equals("segment")) {
				from = Long.parseLong(a.getValue("from"));
				to = Long.parseLong(a.getValue("to"));
			}
		}
	}

	/**
	 * Downloads all missing segments from the given way. If segments fail do download, 
	 * offers the user a chance to delete those segments from the way.
	 * 
	 * @param w way to complete
	 * @return an "edit way" command if the user decided to delete segments
	 * @throws IOException
	 * @throws SAXException
	 */
	private Command download(Way w) throws IOException, SAXException {
		// get all the segments
		Way newway = null;
		for (Segment s : w.segments) {
			if (!s.incomplete)
				continue;
			BufferedReader segReader;
			try {
				segReader = new BufferedReader(new InputStreamReader(getInputStream("segment/"+s.id, null), "UTF-8"));
			} catch (FileNotFoundException e) {
				Object[] options = {"Delete", "Ignore", "Abort"};
				int n = JOptionPane.showOptionDialog(Main.parent,
						tr("Segment {0} is deleted but part of Way {1}",s.id, w.id),
						tr("Data error"),
						JOptionPane.YES_NO_CANCEL_OPTION,
						JOptionPane.ERROR_MESSAGE,
						null, options, options[2]);
				if (n == 0)
				{
					if( newway == null )
						newway = new Way(w);
					newway.segments.remove(s);
				}
				else if (n == 2)
				{
					e.printStackTrace();
					throw new IOException(tr("Data error: Segment {0} is deleted but part of Way {1}", s.id, w.id));
				}
				continue;
			}
			StringBuilder segBuilder = new StringBuilder();
			for (String line = segReader.readLine(); line != null; line = segReader.readLine())
				segBuilder.append(line+"\n");
			SegmentParser segmentParser = new SegmentParser();
			try {
		        SAXParserFactory.newInstance().newSAXParser().parse(new InputSource(new StringReader(segBuilder.toString())), segmentParser);
	        } catch (ParserConfigurationException e1) {
	        	e1.printStackTrace(); // broken SAXException chaining
	        	throw new SAXException(e1);
	        }
			if (segmentParser.from == 0 || segmentParser.to == 0)
				throw new SAXException("Invalid segment response.");
			if (!hasNode(segmentParser.from))
				readNode(segmentParser.from, s.id).visit(merger);
			if (!hasNode(segmentParser.to))
				readNode(segmentParser.to, s.id).visit(merger);
			readSegment(segBuilder.toString()).visit(merger);
		}
		if( newway != null )
			return new ChangeCommand(w, newway);
		return null;
	}

	private boolean hasNode(long id) {
	    for (Node n : Main.ds.nodes)
	    	if (n.id == id)
	    		return true;
	    return false;
    }

	private Segment readSegment(String seg) throws SAXException, IOException {
        return OsmReader.parseDataSet(new ByteArrayInputStream(seg.getBytes("UTF-8")), data, null).segments.iterator().next();
    }

	private Node readNode(long id, long segId) throws SAXException, IOException {
		try {
	        return OsmReader.parseDataSet(getInputStream("node/"+id, null), data, null).nodes.iterator().next();
        } catch (FileNotFoundException e) {
	        e.printStackTrace();
	        throw new IOException(tr("Data error: Node {0} is deleted but part of Segment {1}", id, segId));
        }
    }
}
