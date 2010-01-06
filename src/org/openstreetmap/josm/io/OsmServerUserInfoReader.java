// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.UserInfo;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.DateUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class OsmServerUserInfoReader extends OsmServerReader {

    static protected String getAttribute(Node node, String name) {
        return node.getAttributes().getNamedItem(name).getNodeValue();
    }

    static public  UserInfo buildFromXML(Document document) throws OsmDataParsingException{
        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();
            UserInfo userInfo = new UserInfo();
            Node xmlNode = (Node)xpath.compile("/osm/user[1]").evaluate(document, XPathConstants.NODE);
            if ( xmlNode== null)
                throw new OsmDataParsingException(tr("XML tag <user> is missing."));

            // -- id
            String v = getAttribute(xmlNode, "id");
            if (v == null)
                throw new OsmDataParsingException(tr("Missing attribute ''{0}'' on XML tag ''{1}''.", "id", "user"));
            try {
                userInfo.setId(Integer.parseInt(v));
            } catch(NumberFormatException e) {
                throw new OsmDataParsingException(tr("Illegal value for attribute ''{0}'' on XML tag ''{1}''. Got {2}.", "id", "user", v));
            }
            // -- display name
            v = getAttribute(xmlNode, "display_name");
            userInfo.setDisplayName(v);
            // -- account_created
            v = getAttribute(xmlNode, "account_created");
            if (v!=null) {
                userInfo.setAccountCreated(DateUtils.fromString(v));
            }
            // -- description
            xmlNode = (Node)xpath.compile("/osm/user[1]/description[1]/text()").evaluate(document, XPathConstants.NODE);
            if (xmlNode != null) {
                userInfo.setDescription(xmlNode.getNodeValue());
            }
            // -- home
            xmlNode = (Node)xpath.compile("/osm/user[1]/home").evaluate(document, XPathConstants.NODE);
            if (xmlNode != null) {
                v = getAttribute(xmlNode, "lat");
                if (v == null)
                    throw new OsmDataParsingException(tr("Missing attribute ''{0}'' on XML tag ''{1}''.", "lat", "home"));
                double lat;
                try {
                    lat = Double.parseDouble(v);
                } catch(NumberFormatException e) {
                    throw new OsmDataParsingException(tr("Illegal value for attribute ''{0}'' on XML tag ''{1}''. Got {2}.", "lat", "home", v));
                }

                v = getAttribute(xmlNode, "lon");
                if (v == null)
                    throw new OsmDataParsingException(tr("Missing attribute ''{0}'' on XML tag ''{1}''.", "lon", "home"));
                double lon;
                try {
                    lon = Double.parseDouble(v);
                } catch(NumberFormatException e) {
                    throw new OsmDataParsingException(tr("Illegal value for attribute ''{0}'' on XML tag ''{1}''. Got {2}.", "lon", "home", v));
                }

                v = getAttribute(xmlNode, "zoom");
                if (v == null)
                    throw new OsmDataParsingException(tr("Missing attribute ''{0}'' on XML tag ''{1}''.", "zoom", "home"));
                int zoom;
                try {
                    zoom = Integer.parseInt(v);
                } catch(NumberFormatException e) {
                    throw new OsmDataParsingException(tr("Illegal value for attribute ''{0}'' on XML tag ''{1}''. Got {2}.", "zoom", "home", v));
                }
                userInfo.setHome(new LatLon(lat,lon));
                userInfo.setHomeZoom(zoom);
            }

            // -- language list
            NodeList xmlNodeList = (NodeList)xpath.compile("/osm/user[1]/languages[1]/lang").evaluate(document, XPathConstants.NODESET);
            if (xmlNodeList != null) {
                List<String> languages = new LinkedList<String>();
                for (int i=0; i < xmlNodeList.getLength(); i++) {
                    languages.add(xmlNodeList.item(i).getNodeValue());
                }
                userInfo.setLanguages(languages);
            }
            return userInfo;
        } catch(XPathException e) {
            throw new OsmDataParsingException(e);
        }
    }

    public OsmServerUserInfoReader() {
        setDoAuthenticate(true);
    }

    @Override
    public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {
        // not implemented
        return null;
    }



    public UserInfo fetchUserInfo(ProgressMonitor monitor) throws OsmTransferException {
        try {
            monitor.beginTask(tr("Reading user info ..."));
            InputStream in = getInputStream("user/details", monitor.createSubTaskMonitor(1, true));
            return buildFromXML(
                    DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in)
            );
        } catch(OsmTransferException e) {
            throw e;
        } catch(Exception e) {
            throw new OsmTransferException(e);
        } finally {
            monitor.finishTask();
        }
    }
}
