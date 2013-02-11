// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import java.util.Arrays;

import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Simple literal value, that does not depend on other expressions.
 */
public class LiteralExpression implements Expression {
    Object literal;

    public LiteralExpression(Object literal) {
        CheckParameterUtil.ensureParameterNotNull(literal);
        this.literal = literal;
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
        return "<" + literal.toString() + ">";
    }

}
