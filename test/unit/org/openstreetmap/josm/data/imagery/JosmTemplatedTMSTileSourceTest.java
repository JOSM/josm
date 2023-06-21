// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import org.openstreetmap.gui.jmapviewer.interfaces.TemplatedTileSource;

/**
 * Test class for {@link JosmTemplatedTMSTileSource}
 */
class JosmTemplatedTMSTileSourceTest implements TileSourceTest {
    @Override
    public ImageryInfo getInfo() {
        final ImageryInfo info = new ImageryInfo();
        info.setImageryType(ImageryInfo.ImageryType.TMS);
        // This is a temporary extended URL. Set it to the wiremock server if one is added and used.
        info.setExtendedUrl("https://localhost:8111/example/{x}/{y}/{z}.png");
        return info;
    }

    @Override
    public TemplatedTileSource getTileSource(ImageryInfo info) {
        return new JosmTemplatedTMSTileSource(info);
    }
}
