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

import org.openstreetmap.josm.data.preferences.NamedColorProperty;
import org.openstreetmap.josm.gui.help.Helpful;

/**
 * Map scale bar, displaying the distance in meter that correspond to 100 px on screen.
 * @since 115
 */
public class MapScaler extends JComponent implements Helpful, Accessible {

    private final NavigatableComponent mv;

    private static final int PADDING_LEFT = 5;
    private static final int PADDING_RIGHT = 50;

    private static final NamedColorProperty SCALER_COLOR = new NamedColorProperty(marktr("scale"), Color.WHITE);

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
        return SCALER_COLOR.get();
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
        /**
         * Distance in meters between two ticks.
         */
        private final double spacingMeter;
        private final int steps;
        private final int minorStepsPerMajor;

        /**
         * Creates a new tick mark helper.
         * @param dist100Pixel The distance of 100 pixel on the map.
         * @param width The width of the mark.
         */
        TickMarks(double dist100Pixel, int width) {
            this.dist100Pixel = dist100Pixel;
            double lineDistance = dist100Pixel * width / 100;

            double log10 = Math.log(lineDistance) / Math.log(10);
            double spacingLog10 = Math.pow(10, Math.floor(log10));
            int minorStepsPerMajor;
            double distanceBetweenMinor;
            if (log10 - Math.floor(log10) < .75) {
                // Add 2 ticks for every full unit
                distanceBetweenMinor = spacingLog10 / 2;
                minorStepsPerMajor = 2;
            } else {
                // Add 10 ticks for every full unit
                distanceBetweenMinor = spacingLog10;
                minorStepsPerMajor = 5;
            }
            // round down to the last major step.
            int majorSteps = (int) Math.floor(lineDistance / distanceBetweenMinor / minorStepsPerMajor);
            if (majorSteps >= 4) {
                // we have many major steps, do not paint the minor now.
                this.spacingMeter = distanceBetweenMinor * minorStepsPerMajor;
                this.minorStepsPerMajor = 1;
            } else {
                this.minorStepsPerMajor = minorStepsPerMajor;
                this.spacingMeter = distanceBetweenMinor;
            }
            steps = majorSteps * this.minorStepsPerMajor;
        }

        /**
         * Paint the ticks to the graphics.
         * @param g The graphics to paint on.
         */
        public void paintTicks(Graphics g) {
            double spacingPixel = spacingMeter / (dist100Pixel / 100);
            double textBlockedUntil = -1;
            for (int step = 0; step <= steps; step++) {
                int x = (int) (PADDING_LEFT + spacingPixel * step);
                boolean isMajor = step % minorStepsPerMajor == 0;
                int paddingY = isMajor ? 0 : 3;
                g.drawLine(x, paddingY, x, 10 - paddingY);

                if (step == 0 || step == steps) {
                    String text;
                    if (step == 0) {
                        text = "0";
                    } else {
                        text = NavigatableComponent.getDistText(spacingMeter * step);
                    }
                    Rectangle2D bound = g.getFontMetrics().getStringBounds(text, g);
                    int left = (int) (x - bound.getWidth() / 2);
                    if (textBlockedUntil > left) {
                        left = (int) (textBlockedUntil + 5);
                    }
                    g.drawString(text, left, 23);
                    textBlockedUntil = left + bound.getWidth() + 2;
                }
            }
            g.drawLine(PADDING_LEFT + 0, 5, (int) (PADDING_LEFT + spacingPixel * steps), 5);
        }
    }
}
