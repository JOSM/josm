// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import java.awt.BasicStroke;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.openstreetmap.josm.tools.Logging;

/**
 * A property that stores a {@link BasicStroke}.
 * @author Michael Zangl
 * @since 10874
 */
public class StrokeProperty extends AbstractToStringProperty<BasicStroke> {

    /**
     * Create a new stroke property from a string.
     * @param key The key to use
     * @param defaultValue The default stroke as string
     */
    public StrokeProperty(String key, String defaultValue) {
        super(key, getFromString(defaultValue));
    }

    /**
     * Create a new stroke property from a stroke object.
     * @param key The key
     * @param defaultStroke The default stroke.
     */
    public StrokeProperty(String key, BasicStroke defaultStroke) {
        super(key, defaultStroke);
    }

    @Override
    protected BasicStroke fromString(String string) {
        return getFromString(string);
    }

    @Override
    protected String toString(BasicStroke t) {
        StringBuilder string = new StringBuilder();
        string.append(t.getLineWidth());

        float[] dashes = t.getDashArray();
        if (dashes != null) {
            for (float d : dashes) {
                string.append(' ').append(d);
            }
        }

        return string.toString();
    }

    /**
     * Return s new BasicStroke object with given thickness and style
     * @param code = 3.5 -&gt; thickness=3.5px; 3.5 10 5 -&gt; thickness=3.5px, dashed: 10px filled + 5px empty
     * @return stroke for drawing
     */
    public static BasicStroke getFromString(String code) {
        Pattern floatPattern = Pattern.compile("(\\.\\d+|\\d+(\\.\\d+)?)");

        List<Double> captures = Pattern.compile("[^\\d.]+").splitAsStream(code)
                .filter(s -> floatPattern.matcher(s).matches())
                .map(Double::valueOf).collect(Collectors.toList());

        double w = 1;
        List<Double> dashes = Collections.emptyList();
        if (!captures.isEmpty()) {
            w = captures.get(0);
            dashes = captures.subList(1, captures.size());
        }

        if (!dashes.isEmpty()) {
            double sumAbs = dashes.stream().mapToDouble(Math::abs).sum();

            if (sumAbs < 1e-1) {
                Logging.error("Error in stroke dash format (all zeros): " + code);
                dashes = Collections.emptyList();
            }
        }

        int cap;
        int join;
        if (w > 1) {
            // thick stroke
            cap = BasicStroke.CAP_ROUND;
            join = BasicStroke.JOIN_ROUND;
        } else {
            // thin stroke
            cap = BasicStroke.CAP_BUTT;
            join = BasicStroke.JOIN_MITER;
        }

        return new BasicStroke((float) w, cap, join, 10.0f, toDashArray(dashes), 0.0f);
    }

    private static float[] toDashArray(List<Double> dashes) {
        if (dashes.isEmpty()) {
            return null;
        } else {
            float[] array = new float[dashes.size()];
            for (int i = 0; i < array.length; i++) {
                array[i] = (float) (double) dashes.get(i);
            }
            return array;
        }
    }
}
