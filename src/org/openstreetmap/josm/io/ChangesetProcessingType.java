// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

public enum ChangesetProcessingType {
    USE_NEW_AND_CLOSE(true, true),
    USE_NEW_AND_LEAVE_OPEN (true, false),
    USE_EXISTING_AND_CLOSE (false, true),
    USE_EXISTING_AND_LEAVE_OPEN (false, false);

    private boolean useNew;
    private boolean closeAfterUpload;

    private ChangesetProcessingType(boolean useNew, boolean closeAfterUpload) {
        this.useNew = useNew;
        this.closeAfterUpload = closeAfterUpload;
    }

    public boolean isUseNew() {
        return useNew;
    }

    public boolean isCloseAfterUpload() {
        return closeAfterUpload;
    }
}
