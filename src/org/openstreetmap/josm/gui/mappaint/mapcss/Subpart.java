// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;

/**
 * A subpart identifies different rendering layers (<code>::subpart</code> syntax).
 */
public interface Subpart {
    String getId(Environment env);

    Subpart DEFAULT_SUBPART = new StringSubpart("default");

    /**
     * Simple static subpart identifier.
     *
     * E.g. ::layer_1
     */
    class StringSubpart implements Subpart {
        private final String id;

        public StringSubpart(String id) {
            this.id = id;
        }

        @Override
        public String getId(Environment env) {
            return id;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    /**
     * Subpart identifier given by an expression.
     *
     * E.g. ::(concat("layer_", prop("i", "default")))
     */
    class ExpressionSubpart implements Subpart {
        private final Expression id;

        public ExpressionSubpart(Expression id) {
            this.id = id;
        }

        @Override
        public String getId(Environment env) {
            return Cascade.convertTo(id.evaluate(env), String.class);
        }

        @Override
        public String toString() {
            return String.valueOf(id);
        }
    }
}
