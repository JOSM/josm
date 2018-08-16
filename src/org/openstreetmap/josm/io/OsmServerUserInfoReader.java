// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.time.DateTimeException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.UserInfo;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.UncheckedParseException;
import org.openstreetmap.josm.tools.XmlParsingException;
import org.openstreetmap.josm.tools.date.DateUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Download and parse info of the logged in user (OSM API v0.6 "/user/details").
 * @see <a href="https://wiki.openstreetmap.org/wiki/API_v0.6#Details_of_the_logged-in_user">/user/details</a>
 */
public class OsmServerUserInfoReader extends OsmServerReader {

    /**
     * Parses the given XML data and returns the associated user info.
     * @param document The XML contents
     * @return The user info
     * @throws XmlParsingException if parsing goes wrong
     */
    public static UserInfo buildFromXML(Document document) throws XmlParsingException {
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            UserInfo userInfo = new UserInfo();
            Node xmlNode = (Node) xpath.compile("/osm/user[1]").evaluate(document, XPathConstants.NODE);
            if (xmlNode == null)
                throw new XmlParsingException(tr("XML tag <user> is missing."));

            // -- id
            String v = getAttribute(xmlNode, "id");
            if (v == null)
                throw new XmlParsingException(tr("Missing attribute ''{0}'' on XML tag ''{1}''.", "id", "user"));
            try {
                userInfo.setId(Integer.parseInt(v));
            } catch (NumberFormatException e) {
                throw new XmlParsingException(tr("Illegal value for attribute ''{0}'' on XML tag ''{1}''. Got {2}.", "id", "user", v), e);
            }
            // -- display name
            v = getAttribute(xmlNode, "display_name");
            userInfo.setDisplayName(v);
            // -- account_created
            v = getAttribute(xmlNode, "account_created");
            if (v != null) {
                userInfo.setAccountCreated(DateUtils.fromString(v));
            }
            // -- description
            xmlNode = (Node) xpath.compile("/osm/user[1]/description[1]/text()").evaluate(document, XPathConstants.NODE);
            if (xmlNode != null) {
                userInfo.setDescription(xmlNode.getNodeValue());
            }
            // -- home
            xmlNode = (Node) xpath.compile("/osm/user[1]/home").evaluate(document, XPathConstants.NODE);
            if (xmlNode != null) {
                v = getAttribute(xmlNode, "lat");
                if (v == null)
                    throw new XmlParsingException(tr("Missing attribute ''{0}'' on XML tag ''{1}''.", "lat", "home"));
                double lat;
                try {
                    lat = Double.parseDouble(v);
                } catch (NumberFormatException e) {
                    throw new XmlParsingException(tr("Illegal value for attribute ''{0}'' on XML tag ''{1}''. Got {2}.",
                            "lat", "home", v), e);
                }

                v = getAttribute(xmlNode, "lon");
                if (v == null)
                    throw new XmlParsingException(tr("Missing attribute ''{0}'' on XML tag ''{1}''.", "lon", "home"));
                double lon;
                try {
                    lon = Double.parseDouble(v);
                } catch (NumberFormatException e) {
                    throw new XmlParsingException(tr("Illegal value for attribute ''{0}'' on XML tag ''{1}''. Got {2}.",
                            "lon", "home", v), e);
                }

                v = getAttribute(xmlNode, "zoom");
                if (v == null)
                    throw new XmlParsingException(tr("Missing attribute ''{0}'' on XML tag ''{1}''.", "zoom", "home"));
                int zoom;
                try {
                    zoom = Integer.parseInt(v);
                } catch (NumberFormatException e) {
                    throw new XmlParsingException(tr("Illegal value for attribute ''{0}'' on XML tag ''{1}''. Got {2}.",
                            "zoom", "home", v), e);
                }
                userInfo.setHome(new LatLon(lat, lon));
                userInfo.setHomeZoom(zoom);
            }

            // -- language list
            NodeList xmlNodeList = (NodeList) xpath.compile("/osm/user[1]/languages[1]/lang/text()").evaluate(document, XPathConstants.NODESET);
            if (xmlNodeList != null) {
                List<String> languages = new LinkedList<>();
                for (int i = 0; i < xmlNodeList.getLength(); i++) {
                    languages.add(xmlNodeList.item(i).getNodeValue());
                }
                userInfo.setLanguages(languages);
            }

            // -- messages
            xmlNode = (Node) xpath.compile("/osm/user[1]/messages/received").evaluate(document, XPathConstants.NODE);
            if (xmlNode != null) {
                v = getAttribute(xmlNode, "unread");
                if (v == null)
                    throw new XmlParsingException(tr("Missing attribute ''{0}'' on XML tag ''{1}''.", "unread", "received"));
                try {
                    userInfo.setUnreadMessages(Integer.parseInt(v));
                } catch (NumberFormatException e) {
                    throw new XmlParsingException(
                            tr("Illegal value for attribute ''{0}'' on XML tag ''{1}''. Got {2}.", "unread", "received", v), e);
                }
            }

            return userInfo;
        } catch (XPathException | UncheckedParseException | DateTimeException e) {
            throw new XmlParsingException(e);
        }
    }

    /**
     * Constructs a new {@code OsmServerUserInfoReader}.
     */
    public OsmServerUserInfoReader() {
        setDoAuthenticate(true);
    }

    @Override
    public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {
        // not implemented
        return null;
    }

    /**
     * Fetches user info, without explicit reason.
     * @param monitor The progress monitor
     * @return The user info
     * @throws OsmTransferException if something goes wrong
     */
    public UserInfo fetchUserInfo(ProgressMonitor monitor) throws OsmTransferException {
        return fetchUserInfo(monitor, null);
    }

    /**
     * Fetches user info, with an explicit reason.
     * @param monitor The progress monitor
     * @param reason The reason to show on console. Can be {@code null} if no reason is given
     * @return The user info
     * @throws OsmTransferException if something goes wrong
     * @since 6695
     */
    public UserInfo fetchUserInfo(ProgressMonitor monitor, String reason) throws OsmTransferException {
        return fetchData("user/details", tr("Reading user info ..."),
                OsmServerUserInfoReader::buildFromXML, monitor, reason);
    }
}
