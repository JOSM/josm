// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import java.util.ArrayList;
import java.util.List;

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

    public List<Object> getErrorObjects() {
        return errorMessages;
    }
}
