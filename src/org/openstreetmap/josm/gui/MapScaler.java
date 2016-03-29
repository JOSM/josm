// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.marktr;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleValue;
import javax.swing.JComponent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.help.Helpful;

/**
 * Map scale bar, displaying the distance in meter that correspond to 100 px on screen.
 * @since 115
 */
public class MapScaler extends JComponent implements Helpful, Accessible {

    private final NavigatableComponent mv;

    private final static int PADDING_LEFT = 5;
    private final static int PADDING_RIGHT = 50;

    /**
     * Constructs a new {@code MapScaler}.
     * @param mv map view
     */
    public MapScaler(NavigatableComponent mv) {
        this.mv = mv;
        setPreferredLineLength(100);
        setOpaque(false);
    }

    /**
     * Sets the preferred length the distance line should have.
     * @param pixel The length.
     */
    public void setPreferredLineLength(int pixel) {
        setPreferredSize(new Dimension(pixel + PADDING_LEFT + PADDING_RIGHT, 30));
    }

    @Override
    public void paint(Graphics g) {
        g.setColor(getColor());

        double dist100Pixel = mv.getDist100Pixel(true);
        TickMarks tickMarks = new TickMarks(dist100Pixel, getWidth() - PADDING_LEFT - PADDING_RIGHT);
        tickMarks.paintTicks(g);
    }

    /**
     * Returns the color of map scaler.
     * @return the color of map scaler
     */
    public static Color getColor() {
        return Main.pref.getColor(marktr("scale"), Color.white);
    }

    @Override
    public String helpTopic() {
        return ht("/MapView/Scaler");
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleMapScaler();
        }
        return accessibleContext;
    }

    class AccessibleMapScaler extends AccessibleJComponent implements AccessibleValue {

        @Override
        public Number getCurrentAccessibleValue() {
            return mv.getDist100Pixel();
        }

        @Override
        public boolean setCurrentAccessibleValue(Number n) {
            return false;
        }

        @Override
        public Number getMinimumAccessibleValue() {
            return null;
        }

        @Override
        public Number getMaximumAccessibleValue() {
            return null;
        }
    }

    /**
     * This class finds the best possible tick mark positions.
     * <p>
     * It will attempt to use steps of 1m, 2.5m, 10m, 25m, ...
     */
    private static final class TickMarks {

        private final double dist100Pixel;
        private final double lineDistance;
        /**
         * Distance in meters between two ticks.
         */
        private final double spacingMeter;
        private final int steps;
        private final int majorStepEvery;

        /**
         * Creates a new tick mark helper.
         * @param dist100Pixel The distance of 100 pixel on the map.
         * @param width The width of the mark.
         */
        public TickMarks(double dist100Pixel, int width) {
            this.dist100Pixel = dist100Pixel;
            lineDistance = dist100Pixel * width / 100;

            double log10 = Math.log(lineDistance) / Math.log(10);
            double spacingLog10 = Math.pow(10, Math.floor(log10));
            if (log10 - Math.floor(log10) < .75) {
                spacingMeter = spacingLog10 / 4;
                majorStepEvery = 4;
            } else {
                spacingMeter = spacingLog10;
                majorStepEvery = 5;
            }
            steps = (int) Math.floor(lineDistance / spacingMeter);
        }

        public void paintTicks(Graphics g) {
            double spacingPixel = spacingMeter / (dist100Pixel / 100);
            double textBlockedUntil = -1;
            for (int step = 0; step <= steps; step++) {
                int x = (int) (PADDING_LEFT + spacingPixel * step);
                boolean isMajor = step % majorStepEvery == 0;
                int paddingY = isMajor ? 0 : 3;
                g.drawLine(x, paddingY, x, 10 - paddingY);

                if (isMajor || (step == steps && textBlockedUntil < 0)) {
                    String text;
                    if (step == 0) {
                        text = "0";
                    } else {
                        text = NavigatableComponent.getDistText(spacingMeter * step);
                    }
                    Rectangle2D bound = g.getFontMetrics().getStringBounds(text, g);
                    int left = (int) (x - bound.getWidth() / 2);
                    if (textBlockedUntil < left) {
                        g.drawString(text, left, 23);
                        textBlockedUntil = left + bound.getWidth() + 2;
                    }
                }
            }
            g.drawLine(PADDING_LEFT + 0, 5, (int) (PADDING_LEFT + spacingPixel * steps), 5);
        }
    }
}
