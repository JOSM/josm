//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.AbstractVisitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 */
public class DiffResultReader extends AbstractVisitor {

    /**
     * mapping from old id to new id/version
     */
    private Map<String, Long[]> versions = new HashMap<String, Long[]>();
    private Collection<OsmPrimitive> processed;
    private Map<OsmPrimitive,Long> newIdMap;

    /**
     * List of protocol versions that will be accepted on reading
     */

    private class Parser extends DefaultHandler {

        @Override public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            try {
                if (qName.equals("osm")) {
                } else if (qName.equals("node") || qName.equals("way") || qName.equals("relation")) {
                    String key = qName + ":" + atts.getValue("old_id");
                    String newid = atts.getValue("new_id");
                    String newver = atts.getValue("new_version");
                    Long[] value = new Long[] { newid == null ? null : new Long(newid), newver == null ? null : new Long(newver) };
                    versions.put(key, value);
                }
            } catch (NumberFormatException x) {
                x.printStackTrace(); // SAXException does not chain correctly
                throw new SAXException(x.getMessage(), x);
            } catch (NullPointerException x) {
                x.printStackTrace(); // SAXException does not chain correctly
                throw new SAXException(tr("NullPointerException, possibly some missing tags."), x);
            }
        }
    }

    /**
     * Parse the given input source and return the dataset.
     */
    public static void parseDiffResult(String source, Collection<OsmPrimitive> osm, Collection<OsmPrimitive> processed, Map<OsmPrimitive,Long> newIdMap, ProgressMonitor progressMonitor)
    throws SAXException, IOException {

        progressMonitor.beginTask(tr("Preparing data..."));
        try {

            DiffResultReader drr = new DiffResultReader();
            drr.processed = processed;
            drr.newIdMap = newIdMap;
            InputSource inputSource = new InputSource(new StringReader(source));
            try {
                SAXParserFactory.newInstance().newSAXParser().parse(inputSource, drr.new Parser());
            } catch (ParserConfigurationException e1) {
                e1.printStackTrace(); // broken SAXException chaining
                throw new SAXException(e1);
            }

            for (OsmPrimitive p : osm) {
                //System.out.println("old: "+ p);
                p.visit(drr);
                //System.out.println("new: "+ p);
                //System.out.println("");
            }
        } finally {
            progressMonitor.finishTask();
        }
    }

    public void visit(Node n) {
        String key = "node:" + (newIdMap.containsKey(n) ? newIdMap.get(n) : n.getId());
        Long[] nv = versions.get(key);
        if (nv != null) {
            processed.add(n);
            if (!n.isDeleted()) {
                n.setOsmId(nv[0], nv[1].intValue());
            }
        }
    }
    public void visit(Way w) {
        String key = "way:" + (newIdMap.containsKey(w) ? newIdMap.get(w) : w.getId());
        Long[] nv = versions.get(key);
        if (nv != null) {
            processed.add(w);
            if (!w.isDeleted()) {
                w.setOsmId(nv[0], nv[1].intValue());
            }
        }
    }
    public void visit(Relation r) {
        String key = "relation:" + (newIdMap.containsKey(r) ? newIdMap.get(r) : r.getId());
        Long[] nv = versions.get(key);
        if (nv != null) {
            processed.add(r);
            if (!r.isDeleted()) {
                r.setOsmId(nv[0], nv[1].intValue());
            }
        }
    }
}
