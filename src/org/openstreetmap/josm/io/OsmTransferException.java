// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

public class OsmTransferException extends Exception {

    private String url = OsmApi.getOsmApi().getBaseUrl();

    public OsmTransferException() {
    }

    public OsmTransferException(String message) {
        super(message);
    }

    public OsmTransferException(Throwable cause) {
        super(cause);
    }

    public OsmTransferException(String message, Throwable cause) {
        super(message, cause);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    /**
     *
     * @return Api base url or url set using setUrl method
     */
    public String getUrl() {
        return url;
    }

}
