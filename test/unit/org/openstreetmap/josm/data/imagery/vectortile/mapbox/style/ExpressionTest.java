// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery.vectortile.mapbox.style;

import static org.junit.jupiter.api.Assertions.assertEquals;


import javax.json.Json;
import javax.json.JsonValue;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

/**
 * Test class for {@link Expression}
 * @author Taylor Smock
 * @since 17862
 */
class ExpressionTest {
    @Test
    void testInvalidJson() {
        assertEquals(Expression.EMPTY_EXPRESSION, new Expression(JsonValue.NULL));
        assertEquals(Expression.EMPTY_EXPRESSION, new Expression(JsonValue.FALSE));
        assertEquals(Expression.EMPTY_EXPRESSION, new Expression(JsonValue.TRUE));
        assertEquals(Expression.EMPTY_EXPRESSION, new Expression(JsonValue.EMPTY_JSON_OBJECT));
        assertEquals(Expression.EMPTY_EXPRESSION, new Expression(JsonValue.EMPTY_JSON_ARRAY));
        assertEquals(Expression.EMPTY_EXPRESSION, new Expression(Json.createObjectBuilder().add("bad", "value").build()));
        assertEquals(Expression.EMPTY_EXPRESSION, new Expression(Json.createValue(1)));
        assertEquals(Expression.EMPTY_EXPRESSION, new Expression(Json.createValue(1.0)));
        assertEquals(Expression.EMPTY_EXPRESSION, new Expression(Json.createValue("bad string")));
    }

    @Test
    void testBasicExpressions() {
        // "filter": [ "==|>=|<=|<|>", "key", "value" ]
        assertEquals("[key=value]", new Expression(Json.createArrayBuilder().add("==").add("key").add("value").build()).toString());
        assertEquals("[key>=true]", new Expression(Json.createArrayBuilder().add(">=").add("key").add(true).build()).toString());
        assertEquals("[key<=false]", new Expression(Json.createArrayBuilder().add("<=").add("key").add(false).build()).toString());
        assertEquals("[key<1]", new Expression(Json.createArrayBuilder().add("<").add("key").add(1).build()).toString());
        assertEquals("[key>2.5]", new Expression(Json.createArrayBuilder().add(">").add("key").add(2.5).build()).toString());
        // Test bad expression
        assertEquals(Expression.EMPTY_EXPRESSION, new Expression(Json.createArrayBuilder().add(">>").add("key").add("value").build()));

        // Test expressions with a subarray and object. This is expected to fail when properly supported, so it should be fixed.
        assertEquals("[key=[{bad:value}]]", new Expression(Json.createArrayBuilder().add("==").add("key").add(
          Json.createArrayBuilder().add(Json.createObjectBuilder().add("bad", "value"))).build()).toString());
        assertEquals("[key=]", new Expression(Json.createArrayBuilder().add("==").add("key").add(JsonValue.NULL).build()).toString());
    }

    @Test
    void testEquals() {
        EqualsVerifier.forClass(Expression.class).verify();
    }
}
