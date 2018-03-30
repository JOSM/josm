// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

/**
 * Exception thrown when a communication error with the OSM server occurs.
 */
public class OsmTransferException extends Exception {

    private String url = OsmApi.getOsmApi().getBaseUrl();

    /**
     * Constructs an {@code OsmTransferException} with {@code null} as its error detail message.
     * The cause is not initialized, and may subsequently be initialized by a call to {@link #initCause}.
     */
    public OsmTransferException() {
    }

    /**
     * Constructs an {@code OsmTransferException} with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a call to {@link #initCause}.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage} method)
     */
    public OsmTransferException(String message) {
        super(message);
    }

    /**
     * Constructs an {@code OsmTransferException} with the specified cause and a detail message of
     * <code>(cause==null ? null : cause.toString())</code>
     * (which typically contains the class and detail message of <code>cause</code>).
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause} method).
     *              A <code>null</code> value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public OsmTransferException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs an {@code OsmTransferException} with the specified detail message and cause.
     *
     * <p> Note that the detail message associated with {@code cause} is <i>not</i> automatically incorporated
     * into this exception's detail message.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage} method)
     * @param cause   The cause (which is saved for later retrieval by the {@link #getCause} method).
     *                A null value is permitted, and indicates that the cause is nonexistent or unknown.
     *
     */
    public OsmTransferException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Sets the URL related to this error.
     * @param url the URL related to this error (which is saved for later retrieval by the {@link #getUrl} method).
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Gets the URL related to this error.
     * @return API base URL or URL set using the {@link #setUrl} method
     */
    public String getUrl() {
        return url;
    }
}
