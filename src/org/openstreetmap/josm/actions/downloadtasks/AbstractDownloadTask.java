// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.io.UrlPattern;
import org.openstreetmap.josm.io.XmlWriter;

/**
 * Common abstract implementation of other download tasks.
 * @param <T> The downloaded data type
 * @since 2322
 */
public abstract class AbstractDownloadTask<T> implements DownloadTask {
    private final List<Object> errorMessages;
    private boolean canceled;
    private boolean failed;
    protected T downloadedData;
    protected boolean zoomAfterDownload = true;

    /**
     * Constructs a new {@code AbstractDownloadTask}.
     */
    protected AbstractDownloadTask() {
        errorMessages = new ArrayList<>();
    }

    /**
     * Determines if the download task has been canceled.
     * @return {@code true} if the download task has been canceled
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Marks this download task as canceled.
     * @param canceled {@code true} to mark this download task as canceled
     */
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    /**
     * Determines if the download task has failed.
     * @return {@code true} if the download task has failed
     */
    public boolean isFailed() {
        return failed;
    }

    /**
     * Marks this download task as failed.
     * @param failed {@code true} to mark this download task as failed
     */
    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    protected static <T extends Enum<T> & UrlPattern> String[] patterns(Class<T> urlPatternEnum) {
        // Do not use a method reference until we switch to Java 11, as we face JDK-8141508 with Java 8
        return Arrays.stream(urlPatternEnum.getEnumConstants()).map(/* JDK-8141508 */ t -> t.pattern()).toArray(String[]::new);
    }

    protected final void rememberErrorMessage(String message) {
        errorMessages.add(message);
    }

    protected final void rememberException(Exception exception) {
        errorMessages.add(exception);
    }

    protected final void rememberDownloadedData(T data) {
        this.downloadedData = data;
    }

    /**
     * Replies the downloaded data.
     * @return The downloaded data.
     */
    public final T getDownloadedData() {
        return downloadedData;
    }

    @Override
    public final void setZoomAfterDownload(boolean zoomAfterDownload) {
        this.zoomAfterDownload = zoomAfterDownload;
    }

    @Override
    public List<Object> getErrorObjects() {
        return errorMessages;
    }

    @Override
    public String acceptsDocumentationSummary() {
        StringBuilder buff = new StringBuilder(128)
            .append("<tr><td>")
            .append(getTitle())
            .append(":</td><td>");
        String[] patterns = getPatterns();
        if (patterns.length > 0) {
            buff.append("<ul>");
            for (String pattern: patterns) {
                buff.append("<li>")
                    .append(XmlWriter.encode(pattern))
                    .append("</li>");
            }
            buff.append("</ul>");
        }
        buff.append("</td></tr>");
        return buff.toString();
    }

    /**
     * Determines if the given URL is accepted by {@link #getPatterns}.
     * Can be overridden for more complex checking logic.
     * @param url URL to donwload
     * @return {@code true} if this URL is accepted
     */
    public boolean acceptsUrl(String url) {
        return url != null && Arrays.stream(getPatterns()).anyMatch(url::matches);
    }

    /**
     * Check / decide if the task is safe for remotecontrol.
     *
     * Keep in mind that a potential attacker has full control over the content
     * of the file that will be downloaded.
     * If it is possible to run arbitrary code or write to the local file
     * system, then the task is (obviously) not save for remote execution.
     *
     * The default value is false = unsafe. Override in a subclass to
     * allow running the task via remotecontol.
     *
     * @return true if it is safe to download and open any file of the
     * corresponding format, false otherwise
     */
    public boolean isSafeForRemotecontrolRequests() {
        return false;
    }

    @Override
    public boolean acceptsUrl(String url, boolean isRemotecontrol) {
        if (isRemotecontrol && !isSafeForRemotecontrolRequests())
            return false;
        return acceptsUrl(url);
    }

    // Default name to keep old plugins compatible
    @Override
    public String getTitle() {
        return getClass().getName();
    }

    @Override
    public String toString() {
        return this.getTitle();
    }

    // Default pattern to keep old plugins compatible
    @Override
    public String[] getPatterns() {
        return new String[]{};
    }

    /**
     * Returns the projection bounds of downloaded data.
     * @return the projection bounds of downloaded data or {@code null}
     * @since 11774
     */
    public ProjectionBounds getDownloadProjectionBounds() {
        return null;
    }
}
