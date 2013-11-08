// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * Read from an other reader and increment an progress counter while on the way.
 * @author Imi
 */
public class ProgressInputStream extends InputStream {

    private final InputStream in;
    private int readSoFar = 0;
    private int lastDialogUpdate = 0;
    private boolean sizeKnown;
    private final URLConnection connection;
    private final ProgressMonitor progressMonitor;

    public ProgressInputStream(URLConnection con, ProgressMonitor progressMonitor) throws OsmTransferException {
        this.connection = con;
        if (progressMonitor == null) {
            progressMonitor = NullProgressMonitor.INSTANCE;
        }
        this.progressMonitor = progressMonitor;
        progressMonitor.beginTask(tr("Contacting OSM Server..."), 1);
        progressMonitor.indeterminateSubTask(null);

        try {
            this.in = con.getInputStream();
        } catch (IOException e) {
            progressMonitor.finishTask();
            if (con.getHeaderField("Error") != null)
                throw new OsmTransferException(tr(con.getHeaderField("Error")));
            throw new OsmTransferException(e);
        }

        updateSize();
        if (!sizeKnown) {
            progressMonitor.indeterminateSubTask(tr("Downloading OSM data..."));
        }
    }

    @Override public void close() throws IOException {
        try {
            in.close();
        } finally {
            progressMonitor.finishTask();
        }
    }

    @Override public int read(byte[] b, int off, int len) throws IOException {
        int read = in.read(b, off, len);
        if (read != -1) {
            advanceTicker(read);
        } else {
            progressMonitor.finishTask();
        }
        return read;
    }

    @Override public int read() throws IOException {
        int read = in.read();
        if (read != -1) {
            advanceTicker(1);
        } else {
            progressMonitor.finishTask();
        }
        return read;
    }

    /**
     * Increase ticker (progress counter and displayed text) by the given amount.
     * @param amount
     */
    private void advanceTicker(int amount) {
        readSoFar += amount;
        updateSize();

        if (readSoFar / 1024 != lastDialogUpdate) {
            lastDialogUpdate++;
            if (sizeKnown) {
                progressMonitor.setTicks(readSoFar);
            }
            progressMonitor.setExtraText(readSoFar/1024 + " KB");
        }
    }

    private void updateSize() {
        if (!sizeKnown && connection.getContentLength() > 0) {
            sizeKnown = true;
            progressMonitor.subTask(tr("Downloading OSM data..."));
            progressMonitor.setTicksCount(connection.getContentLength());
        }
    }
}
