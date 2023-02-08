// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.preferences.BooleanProperty;

/**
 * Test class for {@link AuthorizationHandler}
 */
class AuthorizationHandlerTest {
    private static class TestAuthorizationConsumer implements AuthorizationHandler.AuthorizationConsumer {
        boolean validated;
        boolean handled;
        @Override
        public void validateRequest(String sender, String request, Map<String, String> args)
                throws RequestHandler.RequestHandlerBadRequestException {
            this.validated = true;
        }

        @Override
        public AuthorizationHandler.ResponseRecord handleRequest(String sender, String request, Map<String, String> args)
                throws RequestHandler.RequestHandlerErrorException, RequestHandler.RequestHandlerBadRequestException {
            this.handled = true;
            return null;
        }
    }

    @Test
    void testValidateAndHandleRequest() {
        final AuthorizationHandler handler = new AuthorizationHandler();
        TestAuthorizationConsumer consumer = new TestAuthorizationConsumer();
        AuthorizationHandler.addAuthorizationConsumer("test_state", consumer);
        assertDoesNotThrow(() -> handler.setUrl("http://localhost:8111/oauth_authorization?code=code&state=test_state"));
        assertAll(() -> assertDoesNotThrow(handler::validateRequest),
                () -> assertDoesNotThrow(handler::handleRequest),
                () -> assertTrue(consumer.validated),
                () -> assertTrue(consumer.handled));
        // The consumer should only ever be called once
        consumer.validated = false;
        consumer.handled = false;
        assertAll(() -> assertThrows(RequestHandler.RequestHandlerBadRequestException.class, handler::validateRequest),
                () -> assertThrows(NullPointerException.class, handler::handleRequest),
                () -> assertFalse(consumer.validated),
                () -> assertFalse(consumer.handled));
        // Check to make certain that a bad state doesn't work
        AuthorizationHandler.addAuthorizationConsumer("testState", consumer);
        AuthorizationHandler.addAuthorizationConsumer("test_state", consumer);
        assertThrows(IllegalArgumentException.class, () -> AuthorizationHandler.addAuthorizationConsumer("test_state", consumer));
        assertDoesNotThrow(() -> handler.setUrl("http://localhost:8111/oauth_authorization?code=code&testState=test_state"));
        assertAll(() -> assertThrows(RequestHandler.RequestHandlerBadRequestException.class, handler::validateRequest),
                () -> assertThrows(NullPointerException.class, handler::handleRequest),
                () -> assertFalse(consumer.validated),
                () -> assertFalse(consumer.handled));
        assertDoesNotThrow(() -> handler.setUrl("http://localhost:8111/oauth_authorization?code=code&state=no_state_handler"));
        assertAll(() -> assertThrows(RequestHandler.RequestHandlerBadRequestException.class, handler::validateRequest),
                () -> assertThrows(NullPointerException.class, handler::handleRequest),
                () -> assertFalse(consumer.validated),
                () -> assertFalse(consumer.handled));
    }

    @Test
    void testGetPermissionMessage() {
        assertEquals("Allow OAuth remote control to set credentials", new AuthorizationHandler().getPermissionMessage());
    }

    @Test
    void testGetPermissionPref() {
        assertNull(new AuthorizationHandler().getPermissionPref());
    }

    @Test
    void testGetPermissionPreference() {
        final BooleanProperty property = new AuthorizationHandler().getPermissionPreference();
        assertEquals("remotecontrol.permission.authorization", property.getKey());
        assertFalse(property.getDefaultValue());
    }

    @Test
    void testGetMandatoryParams() {
        assertArrayEquals(new String[] {"code", "state"}, new AuthorizationHandler().getMandatoryParams());
    }
}
