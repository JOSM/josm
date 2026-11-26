// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

/**
 * Exception thrown when a valid OAuth access token was expected, but not found.
 */
public class MissingOAuthAccessTokenException extends OsmTransferException {

    /** the API URL of the server JOSM has no OAuth client id for, so that it could not obtain an access token */
    private final String clientIdMissingFor;

    /**
     * Constructs a new {@code MissingOAuthAccessTokenException}.
     */
    public MissingOAuthAccessTokenException() {
        super();
        this.clientIdMissingFor = null;
    }

    /**
     * Constructs a new {@code MissingOAuthAccessTokenException} with the specified detail message and cause.
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage} method)
     * @param cause   The cause (which is saved for later retrieval by the {@link #getCause} method).
     *                A null value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public MissingOAuthAccessTokenException(String message, Throwable cause) {
        super(message, cause);
        this.clientIdMissingFor = null;
    }

    /**
     * Constructs a new {@code MissingOAuthAccessTokenException} with the specified detail message.
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage} method)
     */
    public MissingOAuthAccessTokenException(String message) {
        super(message);
        this.clientIdMissingFor = null;
    }

    private MissingOAuthAccessTokenException(String message, String clientIdMissingFor) {
        super(message);
        this.clientIdMissingFor = clientIdMissingFor;
    }

    /**
     * Constructs a new {@code MissingOAuthAccessTokenException} with the specified cause.
     * @param cause   The cause (which is saved for later retrieval by the {@link #getCause} method).
     *                A null value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public MissingOAuthAccessTokenException(Throwable cause) {
        super(cause);
        this.clientIdMissingFor = null;
    }

    /**
     * Creates the exception for a server JOSM has no OAuth client id for, so that it cannot obtain an access token:
     * the user has to enter the client id of an OAuth application registered on the server, or an access token.
     * @param apiUrl the API URL of the server
     * @return the exception
     * @since 19707
     */
    public static MissingOAuthAccessTokenException clientIdMissing(String apiUrl) {
        return new MissingOAuthAccessTokenException("No OAuth client id for " + apiUrl + ", cannot request a token", apiUrl);
    }

    /**
     * Determines whether JOSM has no OAuth client id for the server, which is why it has no access token.
     * @return {@code true} if the client id is missing
     * @since 19707
     */
    public boolean isClientIdMissing() {
        return clientIdMissingFor != null;
    }

    /**
     * Get the API URL of the server JOSM has no OAuth client id for.
     * @return the API URL, or {@code null} unless {@link #isClientIdMissing()}
     * @since 19707
     */
    public String getClientIdMissingFor() {
        return clientIdMissingFor;
    }
}
