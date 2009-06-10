// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

public class OsmApiException extends OsmTransferException {

    private int responseCode;
    private String errorHeader;
    private String errorBody;

    public OsmApiException() {
        super();
    }

    public OsmApiException(int responseCode, String errorHeader, String errorBody) {
        this.responseCode = responseCode;
        this.errorHeader = errorHeader;
        this.errorBody = errorBody;
    }

    public OsmApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public OsmApiException(String message) {
        super(message);
    }

    public OsmApiException(Throwable cause) {
        super(cause);
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public String getErrorHeader() {
        return errorHeader;
    }

    public void setErrorHeader(String errorHeader) {
        this.errorHeader = errorHeader;
    }

    public String getErrorBody() {
        return errorBody;
    }

    public void setErrorBody(String errorBody) {
        this.errorBody = errorBody;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("ResponseCode=")
        .append(responseCode);
        if (errorHeader != null) {
            sb.append(", Error Header=<")
            .append(errorHeader)
            .append(">");
        }
        if (errorBody != null) {
            sb.append(",Error Body=<")
            .append(errorBody)
            .append(">");
        }
        return sb.toString();
    }
}
