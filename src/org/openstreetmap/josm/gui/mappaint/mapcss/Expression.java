// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import org.openstreetmap.josm.gui.mappaint.Environment;

/**
 * A MapCSS Expression.
 *
 * Can be evaluated in a certain {@link Environment}. Usually takes
 * parameters, that are also Expressions and have to be evaluated first.
 */
public interface Expression {
    /**
     * Evaluate this expression.
     * @param env The environment
     * @return the result of the evaluation, can be a {@link java.util.List}, String or any
     * primitive type or wrapper classes of a primitive type.
     */
    Object evaluate(Environment env);
}
