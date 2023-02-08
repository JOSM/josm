// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

import javax.json.JsonObject;

/**
 * A generic OAuth 2.0 exception
 * @since 18650
 */
public final class OAuth20Exception extends OAuthException {
    private static final long serialVersionUID = -203910656089454886L;

    /**
     * Invalid request types
     */
    public enum Type {
        invalid_request,
        invalid_client,
        invalid_grant,
        unauthorized_client,
        unsupported_grant_type,
        invalid_scope,
        unknown
    }

    private final Type type;

    /**
     * Create a new exception with a specified cause
     * @param cause The cause leading to this exception
     */
    public OAuth20Exception(Exception cause) {
        super(cause);
        this.type = Type.unknown;
    }

    /**
     * Create a new exception with a given message
     * @param message The message to use
     */
    public OAuth20Exception(String message) {
        super(message);
        this.type = Type.unknown;
    }

    /**
     * Create an exception from a server message
     * @param serverMessage The server message. Should conform to
     *                      <a href="https://datatracker.ietf.org/doc/html/rfc6749#section-4.2.2">RFC 6747 Section 4.2.2</a>, but in JSON
     *                      format.
     */
    OAuth20Exception(JsonObject serverMessage) {
        super(serverMessage != null
                ? serverMessage.getString("error_description", serverMessage.getString("error", "Unknown error"))
                : "Unknown error");
        if (serverMessage != null && serverMessage.containsKey("error")) {
            switch(serverMessage.getString("error")) {
                case "invalid_request":
                case "invalid_client":
                case "invalid_grant":
                case "unauthorized_client":
                case "unsupported_grant_type":
                case "invalid_scope":
                    this.type = Type.valueOf(serverMessage.getString("error"));
                    break;
                default:
                    this.type = Type.unknown;
            }
        } else {
            this.type = Type.unknown;
        }
    }

    @Override
    OAuthVersion[] getOAuthVersions() {
        return new OAuthVersion[] {OAuthVersion.OAuth20};
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        if (message == null) {
            return "OAuth error " + this.type;
        }
        return "OAuth error " + this.type + ": " + message;
    }
}
