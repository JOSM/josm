// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations.fake_imagery;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.testutils.annotations.FakeImagery;
import org.openstreetmap.josm.tools.Logging;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;

/**
 * A plain color tile source
 * @since xxx
 */
public class ColorSource extends ConstSource {
    protected final Color color;
    protected final String label;
    protected final int tileSize;

    /**
     * Create a new ColorSource
     * @param colorTileSource The tile source to use
     */
    public ColorSource(final FakeImagery.ColorTileSource colorTileSource) {
        this(colorTileSource.colors().getColor(), colorTileSource.name(), colorTileSource.size());
    }

    /**
     * @param color Color for these tiles
     * @param label text label/name for this source if displayed in JOSM menus
     * @param tileSize Pixel dimension of tiles (usually 256)
     */
    public ColorSource(Color color, String label, int tileSize) {
        this.color = color;
        this.label = label;
        this.tileSize = tileSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.color, this.label, this.tileSize, this.getClass());
    }

    @Override
    public byte[] generatePayloadBytes() {
        BufferedImage image = new BufferedImage(this.tileSize, this.tileSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setBackground(this.color);
        g.clearRect(0, 0, image.getWidth(), image.getHeight());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", outputStream);
        } catch (IOException e) {
            Logging.trace(e);
        }
        return outputStream.toByteArray();
    }

    @Override
    public MappingBuilder getMappingBuilder() {
        return WireMock.get(WireMock.urlMatching(String.format("/%h/(\\d+)/(\\d+)/(\\d+)\\.png", this.hashCode())));
    }

    @Override
    public ImageryInfo getImageryInfo(int port) {
        return new ImageryInfo(this.label,
                String.format("tms[20]:http://localhost:%d/%h/{z}/{x}/{y}.png", port, this.hashCode()), "tms",
                (String) null, (String) null);
    }

    @Override
    public String getLabel() {
        return this.label;
    }
}
