// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.bbox;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Button that allows to choose the imagery source used for slippy map background.
 * @since 1390
 */
public class SourceButton extends JComponent {

    private static final int LAYER_HEIGHT = 20;
    private static final int LEFT_PADDING = 5;
    private static final int TOP_PADDING = 5;
    private static final int BOTTOM_PADDING = 5;

    private transient TileSource[] sources;

    private final ImageIcon enlargeImage;
    private final ImageIcon shrinkImage;
    private final Dimension hiddenDimension;

    // Calculated after component is added to container
    private int barWidth;
    private Dimension shownDimension;
    private Font font;

    private boolean isEnlarged;

    private int currentMap;
    private final SlippyMapBBoxChooser slippyMapBBoxChooser;

    /**
     * Constructs a new {@code SourceButton}.
     * @param slippyMapBBoxChooser parent slippy map
     * @param sources list of imagery sources to display
     */
    public SourceButton(SlippyMapBBoxChooser slippyMapBBoxChooser, Collection<TileSource> sources) {
        this.slippyMapBBoxChooser = slippyMapBBoxChooser;
        setSources(sources);
        enlargeImage = ImageProvider.get("layer-switcher-maximize");
        shrinkImage = ImageProvider.get("layer-switcher-minimize");

        hiddenDimension = new Dimension(enlargeImage.getIconWidth(), enlargeImage.getIconHeight());
        setPreferredSize(hiddenDimension);

        addMouseListener(mouseListener);
    }

    private final transient MouseListener mouseListener = new MouseAdapter() {
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                Point point = e.getPoint();
                if (isEnlarged) {
                    if (barWidth < point.x && point.y < shrinkImage.getIconHeight()) {
                        toggle();
                    } else {
                        int result = (point.y - 5) / LAYER_HEIGHT;
                        if (result >= 0 && result < SourceButton.this.sources.length) {
                            SourceButton.this.slippyMapBBoxChooser.toggleMapSource(SourceButton.this.sources[result]);
                            currentMap = result;
                            toggle();
                        }
                    }
                } else {
                    toggle();
                }
            }
        }
    };

    /**
     * Set the tile sources.
     * @param sources The tile sources to display
     * @since 6364
     */
    public final void setSources(Collection<TileSource> sources) {
        CheckParameterUtil.ensureParameterNotNull(sources, "sources");
        this.sources = sources.toArray(new TileSource[sources.size()]);
        shownDimension = null;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            calculateShownDimension();
            g.setFont(font);
            if (isEnlarged) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int radioButtonSize = 10;

                g.setColor(new Color(0, 0, 139, 179));
                g.fillRoundRect(0, 0, barWidth + shrinkImage.getIconWidth(),
                        sources.length * LAYER_HEIGHT + TOP_PADDING + BOTTOM_PADDING, 10, 10);
                for (int i = 0; i < sources.length; i++) {
                    g.setColor(Color.WHITE);
                    g.fillOval(LEFT_PADDING, TOP_PADDING + i * LAYER_HEIGHT + 6, radioButtonSize, radioButtonSize);
                    g.drawString(sources[i].getName(), LEFT_PADDING + radioButtonSize + LEFT_PADDING,
                            TOP_PADDING + i * LAYER_HEIGHT + g.getFontMetrics().getHeight());
                    if (currentMap == i) {
                        g.setColor(Color.BLACK);
                        g.fillOval(LEFT_PADDING + 1, TOP_PADDING + 7 + i * LAYER_HEIGHT, radioButtonSize - 2, radioButtonSize - 2);
                    }
                }

                g.drawImage(shrinkImage.getImage(), barWidth, 0, null);
            } else {
                g.drawImage(enlargeImage.getImage(), 0, 0, null);
            }
        } finally {
            g.dispose();
        }
    }

    /**
     * Toggle the visibility of imagery source list.
     */
    public void toggle() {
        this.isEnlarged = !this.isEnlarged;
        calculateShownDimension();
        setPreferredSize(isEnlarged ? shownDimension : hiddenDimension);
        revalidate();
    }

    /**
     * Changes the current imagery source used for slippy map background.
     * @param tileSource the new imagery source to use
     */
    public void setCurrentMap(TileSource tileSource) {
        for (int i = 0; i < sources.length; i++) {
            if (sources[i].equals(tileSource)) {
                currentMap = i;
                return;
            }
        }
        currentMap = 0;
    }

    private void calculateShownDimension() {
        if (shownDimension == null) {
            font = getFont().deriveFont(Font.BOLD).deriveFont(15.0f);
            int textWidth = 0;
            FontMetrics fm = getFontMetrics(font);
            for (TileSource source: sources) {
                int width = fm.stringWidth(source.getName());
                if (width > textWidth) {
                    textWidth = width;
                }
            }
            barWidth = textWidth + 50;
            shownDimension = new Dimension(barWidth + shrinkImage.getIconWidth(), sources.length * LAYER_HEIGHT + TOP_PADDING + BOTTOM_PADDING);
        }
    }
}
