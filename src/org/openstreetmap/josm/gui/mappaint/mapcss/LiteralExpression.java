// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import java.util.Arrays;

import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Simple literal value, that does not depend on other expressions.
 * @since 5705
 */
public class LiteralExpression implements Expression {
    private final Object literal;

    /**
     * Constructs a new {@code LiteralExpression}.
     * @param literal literal
     */
    public LiteralExpression(Object literal) {
        CheckParameterUtil.ensureParameterNotNull(literal);
        this.literal = literal;
    }

    /**
     * Returns the literal.
     * @return the literal
     * @since 14484
     */
    public final Object getLiteral() {
        return literal;
    }

    @Override
    public Object evaluate(Environment env) {
        return literal;
    }

    @Override
    public String toString() {
        if (literal instanceof float[]) {
            return Arrays.toString((float[]) literal);
        }
        return '<' + literal.toString() + '>';
    }
}
