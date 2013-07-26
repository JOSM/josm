// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import java.util.ArrayList;
import java.util.List;
import org.openstreetmap.josm.io.XmlWriter;

/**
 * Common abstract implementation of other download tasks
 * @since 2322
 */
public abstract class AbstractDownloadTask implements DownloadTask {
    private List<Object> errorMessages;
    private boolean canceled = false;
    private boolean failed = false;

    public AbstractDownloadTask() {
        errorMessages = new ArrayList<Object>();
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public boolean isFailed() {
        return failed;
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    protected void rememberErrorMessage(String message) {
        errorMessages.add(message);
    }

    protected void rememberException(Exception exception) {
        errorMessages.add(exception);
    }

    @Override
    public List<Object> getErrorObjects() {
        return errorMessages;
    }

    @Override
    public String acceptsDocumentationSummary() {
        StringBuilder buf = new StringBuilder("<tr><td>");
        buf.append(getTitle());
        buf.append(":</td><td>");
        String[] patterns = getPatterns();
        if (patterns.length>0) {
            buf.append("<ul>");
            for (String pattern: patterns) {
                buf.append("<li>");
                buf.append(XmlWriter.encode(pattern));
                buf.append("</li>");
            }
            buf.append("</ul>");
        }
        buf.append("</td></tr>");
        return buf.toString();
    }

    // Can be overridden for more complex checking logic
    @Override
    public boolean acceptsUrl(String url) {
        if (url==null) return false;
        for (String p: getPatterns()) {
            if (url.matches(p)) {
                return true;
            }
        }
        return false;
    }

    // Default name to keep old plugins compatible
    @Override
    public String getTitle() {
        return getClass().getName();
    }

    // Default pattern to keep old plugins compatible
    @Override
    public String[] getPatterns() {
        return new String[]{};
    }

}
