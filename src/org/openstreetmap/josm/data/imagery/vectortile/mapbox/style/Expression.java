// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.vectortile.mapbox.style;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

/**
 * A Mapbox vector style expression (immutable)
 * @author Taylor Smock
 * @see <a href="https://docs.mapbox.com/mapbox-gl-js/style-spec/expressions/">https://docs.mapbox.com/mapbox-gl-js/style-spec/expressions/</a>
 * @since xxx
 */
public final class Expression {
    /** An empty expression to use */
    public static final Expression EMPTY_EXPRESSION = new Expression(JsonValue.NULL);
    private static final String EMPTY_STRING = "";

    private final String mapcssFilterExpression;

    /**
     * Create a new filter expression. <i>Please note that this currently only supports basic comparators!</i>
     * @param value The value to parse
     */
    public Expression(JsonValue value) {
        if (value.getValueType() == JsonValue.ValueType.ARRAY) {
            final JsonArray array = value.asJsonArray();
            if (!array.isEmpty() && array.get(0).getValueType() == JsonValue.ValueType.STRING) {
                if ("==".equals(array.getString(0))) {
                    // The mapcss equivalent of == is = (for the most part)
                    this.mapcssFilterExpression = convertToString(array.get(1)) + "=" + convertToString(array.get(2));
                } else if (Arrays.asList("<=", ">=", ">", "<", "!=").contains(array.getString(0))) {
                    this.mapcssFilterExpression = convertToString(array.get(1)) + array.getString(0) + convertToString(array.get(2));
                } else {
                    this.mapcssFilterExpression = EMPTY_STRING;
                }
            } else {
                this.mapcssFilterExpression = EMPTY_STRING;
            }
        } else {
            this.mapcssFilterExpression = EMPTY_STRING;
        }
    }

    /**
     * Convert a value to a string
     * @param value The value to convert
     * @return A string
     */
    private static String convertToString(JsonValue value) {
        switch (value.getValueType()) {
        case STRING:
            return ((JsonString) value).getString();
        case FALSE:
            return Boolean.FALSE.toString();
        case TRUE:
            return Boolean.TRUE.toString();
        case NUMBER:
            return value.toString();
        case ARRAY:
            return '['
              + ((JsonArray) value).stream().map(Expression::convertToString).collect(Collectors.joining(","))
              + ']';
        case OBJECT:
            return '{'
              + ((JsonObject) value).entrySet().stream()
              .map(entry -> entry.getKey() + ":" + convertToString(entry.getValue())).collect(
                Collectors.joining(","))
              + '}';
        case NULL:
        default:
            return EMPTY_STRING;
        }
    }

    @Override
    public String toString() {
        return !EMPTY_STRING.equals(this.mapcssFilterExpression) ? '[' + this.mapcssFilterExpression + ']' : EMPTY_STRING;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Expression) {
            Expression o = (Expression) other;
            return Objects.equals(this.mapcssFilterExpression, o.mapcssFilterExpression);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.mapcssFilterExpression);
    }
}
