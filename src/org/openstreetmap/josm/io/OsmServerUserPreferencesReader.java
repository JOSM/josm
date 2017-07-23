// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.XmlParsingException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Download and parse preferences of the logged in user (OSM API v0.6 "/user/preferences").
 * @see <a href="https://wiki.openstreetmap.org/wiki/API_v0.6#Preferences_of_the_logged-in_user">/user/preferences</a>
 * @since 12502
 */
public class OsmServerUserPreferencesReader extends OsmServerReader {

    protected static String getAttribute(Node node, String name) {
        return node.getAttributes().getNamedItem(name).getNodeValue();
    }

    /**
     * Parses the given XML data and returns the associated user preferences.
     * @param document The XML contents
     * @return The user preferences
     * @throws XmlParsingException if parsing goes wrong
     */
    public static Map<String, String> buildFromXML(Document document) throws XmlParsingException {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            Map<String, String> result = new HashMap<>();

            // -- preferences
            NodeList xmlNodeList = (NodeList) xpath.compile("/osm/preferences/preference").evaluate(document, XPathConstants.NODESET);
            if (xmlNodeList != null) {
                for (int i = 0; i < xmlNodeList.getLength(); i++) {
                    Node xmlNode = xmlNodeList.item(i);
                    String k = getAttribute(xmlNode, "k");
                    if (k == null)
                        throw new XmlParsingException(tr("Missing attribute ''{0}'' on XML tag ''{1}''.", "k", "preference"));
                    String v = getAttribute(xmlNode, "v");
                    if (v == null)
                        throw new XmlParsingException(tr("Missing attribute ''{0}'' on XML tag ''{1}''.", "v", "preference"));
                    result.put(k, v);
                }
            }

            return result;
        } catch (XPathException e) {
            throw new XmlParsingException(e);
        }
    }

    /**
     * Constructs a new {@code OsmServerUserInfoReader}.
     */
    public OsmServerUserPreferencesReader() {
        setDoAuthenticate(true);
    }

    @Override
    public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {
        // not implemented
        return null;
    }

    /**
     * Fetches user preferences, without explicit reason.
     * @param monitor The progress monitor
     * @return The user preferences
     * @throws OsmTransferException if something goes wrong
     */
    public Map<String, String> fetchUserPreferences(ProgressMonitor monitor) throws OsmTransferException {
        return fetchUserPreferences(monitor, null);
    }

    /**
     * Fetches user info, with an explicit reason.
     * @param monitor The progress monitor
     * @param reason The reason to show on console. Can be {@code null} if no reason is given
     * @return The user info
     * @throws OsmTransferException if something goes wrong
     */
    public Map<String, String> fetchUserPreferences(ProgressMonitor monitor, String reason) throws OsmTransferException {
        try {
            monitor.beginTask("");
            monitor.indeterminateSubTask(tr("Reading user preferences ..."));
            try (InputStream in = getInputStream("user/preferences", monitor.createSubTaskMonitor(1, true), reason)) {
                return buildFromXML(Utils.parseSafeDOM(in));
            }
        } catch (OsmTransferException e) {
            throw e;
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new OsmTransferException(e);
        } finally {
            monitor.finishTask();
        }
    }
}
