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

public class SourceButton extends JComponent {

    private final int layerHeight = 20;
    private final int leftPadding = 5;
    private final int topPadding = 5;
    private final int bottomPadding = 5;


    private TileSource[] sources;

    private final ImageIcon enlargeImage;
    private final ImageIcon shrinkImage;
    private final Dimension hiddenDimension;

    // Calculated after component is added to container
    private int barWidth;
    private Dimension shownDimension;
    private Font font;

    private boolean isEnlarged = false;

    private int currentMap;
    private final SlippyMapBBoxChooser slippyMapBBoxChooser;

    public SourceButton(SlippyMapBBoxChooser slippyMapBBoxChooser, Collection<TileSource> sources) {
        super();
        this.slippyMapBBoxChooser = slippyMapBBoxChooser;
        setSources(sources);
        enlargeImage = ImageProvider.get("layer-switcher-maximize.png");
        shrinkImage = ImageProvider.get("layer-switcher-minimize.png");

        hiddenDimension= new Dimension(enlargeImage.getIconWidth(), enlargeImage.getIconHeight());
        setPreferredSize(hiddenDimension);

        addMouseListener(mouseListener);
    }

    private final MouseListener mouseListener = new MouseAdapter() {
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                Point point = e.getPoint();
                if (isEnlarged) {
                    if (barWidth < point.x && point.y < shrinkImage.getIconHeight()) {
                        toggle();
                    } else {
                        int result = (point.y - 5) / layerHeight;
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
                g.fillRoundRect(0, 0, barWidth + shrinkImage.getIconWidth(), sources.length * layerHeight + topPadding + bottomPadding, 10, 10);
                for (int i=0; i<sources.length; i++) {
                    g.setColor(Color.WHITE);
                    g.fillOval(leftPadding, topPadding + i * layerHeight + 6, radioButtonSize, radioButtonSize);
                    g.drawString(sources[i].getName(), leftPadding + radioButtonSize + leftPadding, topPadding + i * layerHeight + g.getFontMetrics().getHeight());
                    if (currentMap == i) {
                        g.setColor(Color.BLACK);
                        g.fillOval(leftPadding + 1, topPadding + 7 + i * layerHeight, radioButtonSize - 2, radioButtonSize - 2);
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

    public void toggle() {
        this.isEnlarged = !this.isEnlarged;
        calculateShownDimension();
        setPreferredSize(isEnlarged?shownDimension:hiddenDimension);
        revalidate();
    }


    public void setCurrentMap(TileSource tileSource) {
        for (int i=0; i<sources.length; i++) {
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
            shownDimension = new Dimension(barWidth + shrinkImage.getIconWidth(), sources.length * layerHeight + topPadding + bottomPadding);
        }
    }
}
