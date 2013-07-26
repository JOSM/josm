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

import org.w3c.dom.Element;

import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.GpxImporter;
import org.openstreetmap.josm.io.IllegalDataException;

public class GpxTracksSessionImporter implements SessionLayerImporter {

    @Override
    public Layer load(Element elem, SessionReader.ImportSupport support, ProgressMonitor progressMonitor) throws IOException, IllegalDataException {
        String version = elem.getAttribute("version");
        if (!"0.1".equals(version)) {
            throw new IllegalDataException(tr("Version ''{0}'' of meta data for gpx track layer is not supported. Expected: 0.1", version));
        }
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();
            XPathExpression fileExp = xpath.compile("file/text()");
            String fileStr = (String) fileExp.evaluate(elem, XPathConstants.STRING);
            if (fileStr == null || fileStr.isEmpty()) {
                throw new IllegalDataException(tr("File name expected for layer no. {0}", support.getLayerIndex()));
            }

            InputStream in = support.getInputStream(fileStr);
            GpxImporter.GpxImporterData importData = GpxImporter.loadLayers(in, support.getFile(fileStr), support.getLayerName(), null, progressMonitor);

            support.addPostLayersTask(importData.getPostLayerTask());
            return importData.getGpxLayer();

        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

}
