// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.openstreetmap.josm.gui.io.importexport.OsmImporter;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.session.SessionReader.ImportSupport;
import org.w3c.dom.Element;

/**
 * Session importer for {@link OsmDataLayer}.
 * @since 4685
 */
public class OsmDataSessionImporter implements SessionLayerImporter {

    @Override
    public Layer load(Element elem, ImportSupport support, ProgressMonitor progressMonitor) throws IOException, IllegalDataException {
        checkMetaVersion(elem);
        String fileStr = extractFileName(elem, support);
        return importData(new OsmImporter(), support, fileStr, progressMonitor);
    }

    /**
     * Checks that element defines the expected version number.
     * @param elem element to check
     * @throws IllegalDataException if version is not the expected one
     * @since 15377
     */
    public static void checkMetaVersion(Element elem) throws IllegalDataException {
        String version = elem.getAttribute("version");
        if (!"0.1".equals(version)) {
            throw new IllegalDataException(tr("Version ''{0}'' of meta data for osm data layer is not supported. Expected: 0.1", version));
        }
    }

    /**
     * Extract file name from element.
     * @param elem element to parse
     * @param support import/export support
     * @return file name, if present
     * @throws IllegalDataException if file name missing or empty
     * @since 15377
     */
    public static String extractFileName(Element elem, ImportSupport support) throws IllegalDataException {
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();
            XPathExpression fileExp = xpath.compile("file/text()");
            String fileStr = (String) fileExp.evaluate(elem, XPathConstants.STRING);
            if (fileStr == null || fileStr.isEmpty()) {
                throw new IllegalDataException(tr("File name expected for layer no. {0}", support.getLayerIndex()));
            }
            return fileStr;
        } catch (XPathExpressionException e) {
            throw new IllegalDataException(e);
        }
    }

    /**
     * Import data as a new layer.
     * @param osmImporter OSM importer
     * @param support import/export support
     * @param fileStr file name to import
     * @param progressMonitor progress monitor
     * @return new layer
     * @throws IOException in case of I/O error
     * @throws IllegalDataException in case of illegal data
     * @since 15377
     */
    public static Layer importData(OsmImporter osmImporter, ImportSupport support, String fileStr, ProgressMonitor progressMonitor)
            throws IOException, IllegalDataException {
        try (InputStream in = support.getInputStream(fileStr)) {
            OsmImporter.OsmImporterData importData = osmImporter.loadLayer(
                    in, support.getFile(fileStr), support.getLayerName(), progressMonitor);

            support.addPostLayersTask(importData.getPostLayerTask());
            return importData.getLayer();
        }
    }
}
