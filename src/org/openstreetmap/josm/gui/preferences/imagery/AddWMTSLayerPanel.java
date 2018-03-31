// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;

import javax.swing.JLabel;

import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.data.imagery.WMTSTileSource;
import org.openstreetmap.josm.tools.GBC;

/**
 * Panel for adding WMTS imagery sources
 * @author Wiktor Niesiobędzki
 *
 */
public class AddWMTSLayerPanel extends AddImageryPanel {

    /**
     * default constructor
     */
    public AddWMTSLayerPanel() {
        add(new JLabel(tr("{0} Make sure OSM has the permission to use this service", "1.")), GBC.eol());
        add(new JLabel(tr("{0} Enter GetCapabilities URL", "2.")), GBC.eol());
        add(rawUrl, GBC.eop().fill());
        rawUrl.setLineWrap(true);
        rawUrl.setAlignmentY(TOP_ALIGNMENT);
        add(new JLabel(tr("{0} Enter name for this layer", "3.")), GBC.eol());
        add(name, GBC.eol().fill(GBC.HORIZONTAL));
        registerValidableComponent(rawUrl);
    }

    @Override
    protected ImageryInfo getImageryInfo() {
        ImageryInfo ret = new ImageryInfo(getImageryName(), "wmts:" + sanitize(getImageryRawUrl(), ImageryType.WMTS));
        ret.setImageryType(ImageryType.WMTS);
        try {
            new WMTSTileSource(ret); // check if constructor throws an error
        } catch (IOException e) {
            throw new IllegalArgumentException(e); // if so, wrap exception, so proper message will be shown to the user
        }
        return ret;

    }

    @Override
    protected boolean isImageryValid() {
        return !getImageryName().isEmpty() && !getImageryRawUrl().isEmpty();
    }

}
