// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JLabel;

import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
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
        add(new JLabel(tr("1. Enter getCapabilities URL")), GBC.eol());
        add(rawUrl, GBC.eop().fill());
        rawUrl.setLineWrap(true);
        rawUrl.setAlignmentY(TOP_ALIGNMENT);
        add(new JLabel(tr("2. Enter name for this layer")), GBC.eol());
        add(name, GBC.eol().fill(GBC.HORIZONTAL));
        registerValidableComponent(rawUrl);
    }

    @Override
    protected ImageryInfo getImageryInfo() {
        return new ImageryInfo(getImageryName(), "wmts:" + sanitize(getImageryRawUrl(), ImageryType.WMTS));
    }

    @Override
    protected boolean isImageryValid() {
        return !getImageryName().isEmpty() && !getImageryRawUrl().isEmpty();
    }

}
