package org.openstreetmap.josm.gui.bbox;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.tools.ImageProvider;

public class SourceButton {

    // Filled in paint, used in hit
    private int barX;
    private int barY;
    private int barWidth;
    private int layerHeight;

    private String[] sources = new String[] {"Mapnik", "Osmarender", "Cyclemap"};

    private ImageIcon enlargeImage;
    private ImageIcon shrinkImage;

    private boolean isEnlarged = false;

    private int currentMap = MAPNIK;

    public static final int HIDE_OR_SHOW = 1;
    public static final int MAPNIK = 2;
    public static final int OSMARENDER = 3;
    public static final int CYCLEMAP = 4;

    public SourceButton() {
        enlargeImage = ImageProvider.get("layer-switcher-maximize.png");
        shrinkImage = ImageProvider.get("layer-switcher-minimize.png");
    }

    public void paint(Graphics2D g) {
        if (isEnlarged) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int leftPadding = 5;
            int radioButtonSize = 10;
            int topPadding = 5;
            int bottomPadding = 5;

            int textWidth = 0;

            g.setFont(g.getFont().deriveFont(Font.BOLD).deriveFont(15.0f));
            FontMetrics fm = g.getFontMetrics();
            for (String source: sources) {
                int width = fm.stringWidth(source);
                if (width > textWidth) {
                    textWidth = width;
                }
            }

            barWidth = textWidth + 50;
            barX = g.getClipBounds().width  - barWidth - shrinkImage.getIconWidth();
            barY = 30;
            layerHeight = 20;

            g.setColor(new Color(0, 0, 139, 179));
            g.fillRoundRect(barX, barY, barWidth + shrinkImage.getIconWidth(), sources.length * layerHeight + topPadding + bottomPadding, 10, 10);
            for (int i=0; i<sources.length; i++) {
                g.setColor(Color.WHITE);
                g.fillOval(barX + leftPadding, barY + topPadding + i * layerHeight + 6, radioButtonSize, radioButtonSize);
                g.drawString(sources[i], barX + leftPadding + radioButtonSize + leftPadding, barY + topPadding + i * layerHeight + g.getFontMetrics().getHeight());
                if (currentMap == i + 2) {
                    g.setColor(Color.BLACK);
                    g.fillOval(barX + leftPadding + 1, barY + topPadding + 7 + i * layerHeight, radioButtonSize - 2, radioButtonSize - 2);
                }
            }

            g.drawImage(shrinkImage.getImage(), barX + barWidth, barY, null);
        } else {
            barWidth = 0;
            barX = g.getClipBounds().width  - shrinkImage.getIconWidth();
            barY = 30;
            g.drawImage(enlargeImage.getImage(), barX + barWidth, barY, null);
        }
    }

    public void toggle() {
        this.isEnlarged = !this.isEnlarged;

    }

    public int hit(Point point) {
        if (isEnlarged) {
            if (barX + barWidth < point.x) {
                if (barY < point.y && point.y < barY + shrinkImage.getIconHeight())
                    return HIDE_OR_SHOW;
            } else if (barX < point.x && point.x < barX + barWidth) {
                int result = (point.y - barY - 5) / layerHeight;
                if (result >= 0 && result < sources.length) {
                    currentMap = result + 2;
                    return currentMap;
                }
            }
        } else {
            if (barX + barWidth < point.x) {
                if (barY < point.y && point.y < barY + shrinkImage.getIconHeight())
                    return HIDE_OR_SHOW;
            }
        }

        return 0;
    }

    /**
     * One of the constants OSMARENDER,MAPNIK or CYCLEMAP
     */
    public void setMapStyle (int style) {
        currentMap = (style < 2 || style > 4) ? MAPNIK : style;
    }
}
