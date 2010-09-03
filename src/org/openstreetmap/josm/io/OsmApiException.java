// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;
import static org.openstreetmap.josm.tools.I18n.tr;

public class OsmApiException extends OsmTransferException {

    private int responseCode;
    private String errorHeader;
    private String errorBody;
    private String accessedUrl;

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
        String eh = "";
        try
        {
            if(errorHeader != null)
                eh = tr(errorHeader.trim());
            if (!eh.isEmpty()) {
                sb.append(", Error Header=<")
                .append(eh)
                .append(">");
            }
        }
        catch (Exception e) {
        }
        try
        {
            String eb = errorBody != null ? tr(errorBody.trim()) : "";
            if (!eb.isEmpty() && !eb.equals(eh)) {
                sb.append(", Error Body=<")
                .append(eb)
                .append(">");
            }
        }
        catch (Exception e) {
        }
        return sb.toString();
    }

    /**
     * Replies a message suitable to be displayed in a message dialog
     *
     * @return a message which is suitable to be displayed in a message dialog
     */
    public String getDisplayMessage() {
        StringBuilder sb = new StringBuilder();
        if (errorHeader != null) {
            sb.append(tr(errorHeader));
            sb.append(tr("(Code={0})", responseCode));
        } else if (errorBody != null && !errorBody.trim().equals("")) {
            errorBody = errorBody.trim();
            sb.append(tr(errorBody));
            sb.append(tr("(Code={0})", responseCode));
        } else {
            sb.append(tr("The server replied an error with code {0}.", responseCode));
        }
        return sb.toString();
    }

    public void setAccessedUrl(String url) {
        this.accessedUrl = url;
    }

    public String getAccessedUrl() {
        return accessedUrl;
    }
}
