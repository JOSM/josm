// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import org.openstreetmap.josm.tools.ImageProvider;

import javax.swing.Icon;
import java.util.List;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * A layer showing geocoded images from <a href="https://commons.wikimedia.org/">Wikimedia Commons</a>
 */
class WikimediaCommonsLayer extends GeoImageLayer {
    WikimediaCommonsLayer(List<ImageEntry> imageEntries) {
        super(imageEntries, null);
        setName(tr("Wikimedia Commons"));
    }

    @Override
    public Icon getIcon() {
        return ImageProvider.get("wikimedia_commons", ImageProvider.ImageSizes.LAYER);
    }
}
