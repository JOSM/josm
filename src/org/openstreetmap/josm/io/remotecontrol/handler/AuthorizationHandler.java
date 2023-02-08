// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;

/**
 * Handle authorization requests (mostly OAuth)
 * @since 18650
 */
public class AuthorizationHandler extends RequestHandler {

    /**
     * The actual authorization consumer/handler
     */
    public interface AuthorizationConsumer {
        /**
         * Validate the request
         * @param args The GET request arguments
         * @param request The request URL without "GET".
         * @param sender who sent the request? the host from referer header or IP of request sender
         * @throws RequestHandlerBadRequestException if the request is invalid
         * @see RequestHandler#validateRequest()
         */
        void validateRequest(String sender, String request, Map<String, String> args)
                throws RequestHandlerBadRequestException;

        /**
         * Handle the request. Any time-consuming operation must be performed asynchronously to avoid delaying the HTTP response.
         * @param args The GET request arguments
         * @param request The request URL without "GET".
         * @param sender who sent the request? the host from referer header or IP of request sender
         * @return The response to show the user. May be {@code null}.
         * @throws RequestHandlerErrorException if an error occurs while processing the request
         * @throws RequestHandlerBadRequestException if the request is invalid
         * @see RequestHandler#handleRequest()
         */
        ResponseRecord handleRequest(String sender, String request, Map<String, String> args)
                throws RequestHandlerErrorException, RequestHandlerBadRequestException;
    }

    /**
     * A basic record for changing responses
     */
    public static final class ResponseRecord {
        private final String content;
        private final String type;

        /**
         * Create a new record
         * @param content The content to show the user
         * @param type The content mime type
         */
        public ResponseRecord(String content, String type) {
            this.content = content;
            this.type = type;
        }

        /**
         * Get the content for the response
         * @return The content as a string
         */
        public String content() {
            return this.content;
        }

        /**
         * Get the type for the response
         * @return The response mime type
         */
        public String type() {
            return this.type;
        }
    }

    /**
     * The remote control command
     */
    public static final String command = "oauth_authorization";

    private static final BooleanProperty PROPERTY = new BooleanProperty("remotecontrol.permission.authorization", false);
    private static final Map<String, AuthorizationConsumer> AUTHORIZATION_CONSUMERS = new HashMap<>();

    private AuthorizationConsumer consumer;
    /**
     * Add an authorization consumer.
     * @param state The unique state for each request (for OAuth, this would be the {@code state} parameter)
     * @param consumer The consumer of the response
     */
    public static synchronized void addAuthorizationConsumer(String state, AuthorizationConsumer consumer) {
        if (AUTHORIZATION_CONSUMERS.containsKey(state)) {
            throw new IllegalArgumentException("Cannot add multiple consumers for one authorization state");
        }
        AUTHORIZATION_CONSUMERS.put(state, consumer);
    }

    @Override
    protected void validateRequest() throws RequestHandlerBadRequestException {
        boolean clearAll = false;
        for (Map.Entry<String, AuthorizationConsumer> entry : AUTHORIZATION_CONSUMERS.entrySet()) {
            if (Objects.equals(this.args.get("state"), entry.getKey())) {
                if (this.consumer == null) {
                    this.consumer = entry.getValue();
                } else {
                    // Remove all authorization consumers. Someone might be playing games.
                    clearAll = true;
                }
            }
        }
        if (clearAll) {
            AUTHORIZATION_CONSUMERS.clear();
            this.consumer = null;
            throw new RequestHandlerBadRequestException("Multiple states for authorization");
        }

        if (this.consumer == null) {
            throw new RequestHandlerBadRequestException("Unknown state for authorization");
        }
        this.consumer.validateRequest(this.sender, this.request, this.args);
    }

    @Override
    protected void handleRequest() throws RequestHandlerErrorException, RequestHandlerBadRequestException {
        ResponseRecord response = this.consumer.handleRequest(this.sender, this.request, this.args);
        if (response != null) {
            this.content = Optional.ofNullable(response.content()).orElse(this.content);
            this.contentType = Optional.ofNullable(response.type()).orElse(this.contentType);
        }
        // Only ever allow a consumer to be used once
        AUTHORIZATION_CONSUMERS.entrySet().stream().filter(entry -> Objects.equals(this.consumer, entry.getValue()))
                .map(Map.Entry::getKey).collect(Collectors.toList()).forEach(AUTHORIZATION_CONSUMERS::remove);
        this.consumer = null;
    }

    @Override
    public String getPermissionMessage() {
        return "Allow OAuth remote control to set credentials";
    }

    @Override
    public PermissionPrefWithDefault getPermissionPref() {
        return null;
    }

    public BooleanProperty getPermissionPreference() {
        return PROPERTY;
    }

    @Override
    public String[] getMandatoryParams() {
        return new String[] {"code", "state"};
    }
}
